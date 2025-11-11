package com.minestom.mechanics.systems.compatibility.animation;

import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

// TODO: Make sure this isn't causing issues and is done smoothly and efficiently
//  ALSO could probably be renamed. This is only really necessary when working with older versions anyways,
//  so if your server only supports like 1.21 or something, you're probably fine.
//  Also make sure it's being used properly, and potentially move this to a different package?
//  It IS a utility package though, so it might be fine here

// TODO: Also move this to player package

/**
 * Version-based animation handler.
 *
 * Core principle:
 * - Modern clients (1.9+): Handle animations via game state → NEVER send metadata packets
 * - Legacy clients (1.8.9): Need metadata packets → send every tick
 *
 * This prevents:
 * - Stuttering on modern clients (no conflicting packets)
 * - Animation breaking on legacy clients (constant updates)
 */
public class ViewerBasedAnimationHandler {

    private static ViewerBasedAnimationHandler instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("ViewerAnimation");

    private final Map<UUID, AnimationState> activeAnimations = new ConcurrentHashMap<>();

    private ViewerBasedAnimationHandler() {}

    public static ViewerBasedAnimationHandler getInstance() {
        if (instance == null) {
            instance = new ViewerBasedAnimationHandler();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        var handler = MinecraftServer.getGlobalEventHandler();

        handler.addListener(PlayerTickEvent.class, event -> {
            updateAnimations();
        });

        handler.addListener(PlayerDisconnectEvent.class, event -> {
            activeAnimations.remove(event.getPlayer().getUuid());
        });

        log.debug("Version-based animation system initialized");
    }

    // ===========================
    // PUBLIC API
    // ===========================

    public void registerAnimation(Player player, String animationId,
                                  Predicate<Player> isActiveCheck) {
        UUID uuid = player.getUuid();

        AnimationState state = activeAnimations.computeIfAbsent(uuid, k -> new AnimationState());
        state.player = player;
        state.animationId = animationId;
        state.isActiveCheck = isActiveCheck;
        state.lastActive = isActiveCheck.test(player);

        if (state.lastActive) {
            // Initial state: send to legacy clients only
            // Modern clients will handle via game mechanics
            ClientVersionDetector detector = ClientVersionDetector.getInstance();
            sendToLegacyViewersAndSelf(player, detector);

            log.debug("{} started animation: {}", player.getUsername(), animationId);
        }
    }

    public void unregisterAnimation(Player player) {
        UUID uuid = player.getUuid();
        AnimationState state = activeAnimations.remove(uuid);

        if (state != null) {
            // Final state: send to legacy clients only
            // Modern clients will handle via game mechanics
            ClientVersionDetector detector = ClientVersionDetector.getInstance();
            sendToLegacyViewersAndSelf(player, detector);

            log.debug("{} stopped animation: {}", player.getUsername(), state.animationId);
        }
    }

    // ===========================
    // UPDATE LOOP
    // ===========================

    private void updateAnimations() {
        ClientVersionDetector detector = ClientVersionDetector.getInstance();

        for (AnimationState state : activeAnimations.values()) {
            Player player = state.player;
            if (player == null || !player.isOnline()) continue;

            boolean currentlyActive = state.isActiveCheck.test(player);
            boolean stateChanged = currentlyActive != state.lastActive;

            if (stateChanged) {
                // STATE CHANGE: Only send to legacy viewers
                // Modern clients handle state changes automatically via game mechanics
                sendToLegacyViewersAndSelf(player, detector);
                state.lastActive = currentlyActive;

            } else if (currentlyActive) {
                // ANIMATION ACTIVE: Update self + legacy viewers only
                sendToLegacyViewersAndSelf(player, detector);
            }
        }
    }

    // ===========================
    // PACKET SENDING
    // ===========================

    /**
     * Send metadata to legacy clients only (both self and viewers).
     * Modern clients handle animations via game state, not metadata packets.
     */
    private void sendToLegacyViewersAndSelf(Player player, ClientVersionDetector detector) {
        var packet = player.getMetadataPacket();

        // Send to self if legacy
        if (detector.getClientVersion(player) == ClientVersionDetector.ClientVersion.LEGACY) {
            player.sendPacket(packet);
        }

        // Send to legacy viewers
        for (Player viewer : player.getViewers()) {
            if (detector.getClientVersion(viewer) == ClientVersionDetector.ClientVersion.LEGACY) {
                viewer.sendPacket(packet);
            }
        }
    }

    // ===========================
    // STATISTICS
    // ===========================

    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Active Animations: ").append(activeAnimations.size()).append("\n");

        ClientVersionDetector detector = ClientVersionDetector.getInstance();
        int totalLegacy = 0;
        int totalModern = 0;

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (detector.getClientVersion(player) == ClientVersionDetector.ClientVersion.LEGACY) {
                totalLegacy++;
            } else {
                totalModern++;
            }
        }

        stats.append("Legacy Clients: ").append(totalLegacy).append(" (receive packets)\n");
        stats.append("Modern Clients: ").append(totalModern).append(" (no packets - handle via game state)\n");

        return stats.toString();
    }

    // ===========================
    // INNER CLASSES
    // ===========================

    private static class AnimationState {
        Player player;
        String animationId;
        Predicate<Player> isActiveCheck;
        boolean lastActive = false;
    }
}

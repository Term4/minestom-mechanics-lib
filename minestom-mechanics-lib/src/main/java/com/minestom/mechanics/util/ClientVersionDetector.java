package com.minestom.mechanics.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: This doesn't actually detect their PROTOCOL version, just their client
//  Not a bad thing to have, but in the future I want to have a velocity level plugin
//  that can detect the actual protocol version, and use a plugin messager channel
//  to send that here. Should be an optional feature though, not a requirement.

/**
 * Detects client version through observable behavior patterns.
 *
 * Detection Methods:
 * 1. Brand string (minecraft:brand channel)
 * 2. Settings packet analysis
 * 3. Behavioral heuristics (animation persistence)
 *
 * NO Velocity plugin messaging required!
 */
public class ClientVersionDetector {

    private static ClientVersionDetector instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("ClientVersionDetector");

    // Client version tracking
    private final Map<UUID, ClientVersion> detectedVersions = new ConcurrentHashMap<>();
    private final Map<UUID, String> clientBrands = new ConcurrentHashMap<>();

    // Animation persistence tracking for heuristic detection
    private final Map<UUID, AnimationTracker> animationTrackers = new ConcurrentHashMap<>();

    private ClientVersionDetector() {}

    public static ClientVersionDetector getInstance() {
        if (instance == null) {
            instance = new ClientVersionDetector();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Listen for brand messages
        handler.addListener(PlayerPluginMessageEvent.class, event -> {
            String identifier = event.getIdentifier();

            // Handle both legacy and modern brand channels
            if (identifier.equals("minecraft:brand") || identifier.equals("MC|Brand")) {
                handleBrandMessage(event.getPlayer(), event.getMessage());
            }
        });

        // Initialize tracking on spawn
        handler.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                UUID uuid = event.getPlayer().getUuid();
                animationTrackers.put(uuid, new AnimationTracker());
            }
        });

        // Cleanup on disconnect
        handler.addListener(PlayerDisconnectEvent.class, event -> {
            UUID uuid = event.getPlayer().getUuid();
            detectedVersions.remove(uuid);
            clientBrands.remove(uuid);
            animationTrackers.remove(uuid);
        });

        log.debug("Client version detector initialized");
    }

    /**
     * Parse brand string to detect version
     */
    private void handleBrandMessage(Player player, byte[] data) {
        try {
            // Read VarInt length (skip it)
            int index = 0;
            int length = 0;
            int shift = 0;
            byte b;
            do {
                b = data[index++];
                length |= (b & 0x7F) << shift;
                shift += 7;
            } while ((b & 0x80) != 0);

            // Read brand string
            String brand = new String(data, index, Math.min(length, data.length - index),
                    StandardCharsets.UTF_8);

            clientBrands.put(player.getUuid(), brand);

            // Attempt detection from brand
            ClientVersion detected = detectFromBrand(brand);
            if (detected != ClientVersion.UNKNOWN) {
                detectedVersions.put(player.getUuid(), detected);
                log.info("{} detected as {} (brand: '{}')",
                        player.getUsername(), detected, brand);
            } else {
                log.debug("{} has brand '{}' - will use heuristic detection",
                        player.getUsername(), brand);
            }

        } catch (Exception e) {
            log.warn("Failed to parse brand message from {}: {}",
                    player.getUsername(), e.getMessage());
        }
    }

    /**
     * Detect version from brand string
     */
    private ClientVersion detectFromBrand(String brand) {
        if (brand == null) return ClientVersion.UNKNOWN;

        String lower = brand.toLowerCase();

        // Known legacy clients
        if (lower.contains("labymod 3") || lower.contains("labymod3")) {
            return ClientVersion.LEGACY; // LabyMod 3 is 1.8.9
        }
        if (lower.contains("lunarclient")) {
            if (lower.contains("1.8") || lower.contains(":v1.") || lower.contains(":v2.")) {
                return ClientVersion.LEGACY;  // This will catch Term4!
            }
            return ClientVersion.LEGACY;  // Default for Lunar
        }
        if (lower.contains("pvplounge") || lower.contains("kohi")) {
            return ClientVersion.LEGACY;
        }

        // Known modern clients
        if (lower.contains("fabric") || lower.contains("forge") || lower.contains("quilt")) {
            // Modded clients - usually modern
            return ClientVersion.MODERN;
        }
        if (lower.contains("labymod 4") || lower.contains("labymod4")) {
            return ClientVersion.MODERN;
        }

        // Generic "vanilla" doesn't tell us much
        return ClientVersion.UNKNOWN;
    }

    /**
     * Get client version for a player
     */
    public ClientVersion getClientVersion(Player player) {
        ClientVersion detected = detectedVersions.get(player.getUuid());

        if (detected != null && detected != ClientVersion.UNKNOWN) {
            return detected;
        }

        // Fall back to heuristic if not detected from brand
        return getHeuristicVersion(player);
    }

    /**
     * Heuristic detection based on behavior
     */
    private ClientVersion getHeuristicVersion(Player player) {
        AnimationTracker tracker = animationTrackers.get(player.getUuid());
        if (tracker == null) return ClientVersion.UNKNOWN;

        // If we have enough data, make a decision
        if (tracker.samples >= 10) {
            // If animation breaks frequently during movement → legacy client
            double breakRate = (double) tracker.breaksWhileMoving / tracker.samples;

            if (breakRate > 0.3) {
                log.info("{} detected as LEGACY via heuristic (break rate: {:.1f}%)",
                        player.getUsername(), breakRate * 100);
                return ClientVersion.LEGACY;
            } else if (breakRate < 0.1) {
                log.info("{} detected as MODERN via heuristic (break rate: {:.1f}%)",
                        player.getUsername(), breakRate * 100);
                return ClientVersion.MODERN;
            }
        }

        // Default to modern to avoid packet spam for unknown clients
        return ClientVersion.MODERN;
    }

    /**
     * Report animation state for heuristic learning
     */
    public void reportAnimationState(Player player, boolean animating, boolean moving) {
        AnimationTracker tracker = animationTrackers.get(player.getUuid());
        if (tracker == null) return;

        // Track if animation broke while moving (indicates legacy client)
        if (!animating && moving && tracker.wasAnimating) {
            tracker.breaksWhileMoving++;
        }

        tracker.wasAnimating = animating;
        tracker.samples++;
    }

    /**
     * Check if player needs frequent animation updates
     */
    public boolean needsFrequentUpdates(Player player) {
        return getClientVersion(player) == ClientVersion.LEGACY;
    }

    /**
     * Get client brand string
     */
    public String getClientBrand(Player player) {
        return clientBrands.get(player.getUuid());
    }

    // ===========================
    // INNER CLASSES
    // ===========================

    public enum ClientVersion {
        LEGACY,   // 1.8.9 and similar - needs constant updates
        MODERN,   // 1.9+ - maintains animations automatically
        UNKNOWN   // Not yet detected
    }

    private static class AnimationTracker {
        int samples = 0;
        int breaksWhileMoving = 0;
        boolean wasAnimating = false;
    }

    /**
     * Get detection statistics
     */
    public String getStatistics() {
        int total = detectedVersions.size();
        long legacy = detectedVersions.values().stream()
                .filter(v -> v == ClientVersion.LEGACY).count();
        long modern = detectedVersions.values().stream()
                .filter(v -> v == ClientVersion.MODERN).count();

        return String.format("Clients: %d total (%d legacy, %d modern)",
                total, legacy, modern);
    }
}

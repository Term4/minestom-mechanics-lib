package com.minestom.mechanics.systems.compatibility;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reduces stutter for modern clients by filtering self-metadata.
 *
 * Minestom syncs the player entity to all viewers (including the player), so the server
 * echoes pose/hand state back to the client. That echo conflicts with client-side state
 * and causes stutter (blocking, sneaking). We can't change who receives sync; we filter
 * outgoing packets instead.
 *
 * <p>We block most self-metadata and allow only what's needed for elytra: STANDING and
 * 0:Byte=0 when on ground (landing), and 0:Byte=-128 (fall flying). No SNEAKING or other
 * pose/hand sync, so the client drives those animations without server echo.
 */
public class ModernStutterFix {

    private static ModernStutterFix instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("ModernStutterFix");

    /** Elytra bit in the entity flags byte (index 0). Bit 7 = 0x80. */
    private static final byte ELYTRA_FLAG_BIT = (byte) 0x80;

    /** Grace period (ms) after last known FALL_FLYING pose to still allow landing metadata. */
    private static final long ELYTRA_GRACE_MS = 1000;

    /**
     * Tracks the last time each player was known to be in FALL_FLYING pose.
     * This solves the race condition where Minestom updates player.getPose() to STANDING
     * before our packet filter runs, causing us to miss the FALL_FLYING → STANDING transition.
     */
    private final Map<UUID, Long> recentlyFlying = new ConcurrentHashMap<>();

    private ModernStutterFix() {}

    public static ModernStutterFix getInstance() {
        if (instance == null) {
            instance = new ModernStutterFix();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Track when players are in FALL_FLYING so we can detect landing even after
        // Minestom updates the pose before our packet filter runs.
        handler.addListener(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();
            if (player.getPose() == EntityPose.FALL_FLYING) {
                recentlyFlying.put(player.getUuid(), System.currentTimeMillis());
            }
        });

        // Clean up tracking on disconnect
        handler.addListener(PlayerDisconnectEvent.class, event -> {
            recentlyFlying.remove(event.getPlayer().getUuid());
        });

        // Intercept outgoing packets
        handler.addListener(PlayerPacketOutEvent.class, event -> {
            if (!(event.getPacket() instanceof EntityMetaDataPacket metadataPacket)) {
                return;
            }

            Player player = event.getPlayer();

            // Only filter self-metadata
            if (metadataPacket.entityId() != player.getEntityId()) {
                return;
            }

            // Only filter modern clients
            ClientVersionDetector detector = ClientVersionDetector.getInstance();
            if (detector.getClientVersion(player) != ClientVersionDetector.ClientVersion.MODERN) {
                return;
            }

            boolean onGround = player.isOnGround();
            boolean wasRecentlyFlying = wasRecentlyFlying(player.getUuid());

            // Check each entry — allow the packet if ANY entry is elytra-related
            boolean allow = false;
            var entries = metadataPacket.entries();

            for (var e : entries.entrySet()) {
                int index = e.getKey();
                Object val = e.getValue().value();

                // Index 6: Pose
                if (val instanceof EntityPose p) {
                    if (p == EntityPose.FALL_FLYING) {
                        // Starting elytra flight
                        log.debug("[metadata] {} allowing self packet (pose=FALL_FLYING, entering flight)",
                                player.getUsername());
                        allow = true;
                        continue;
                    }
                    if (p == EntityPose.STANDING && onGround && wasRecentlyFlying) {
                        // Elytra landing — client needs this to exit FALL_FLYING
                        log.debug("[metadata] {} allowing self packet (pose=STANDING, elytra landing)",
                                player.getUsername());
                        recentlyFlying.remove(player.getUuid());
                        allow = true;
                        continue;
                    }
                }

                // Index 0: Entity flags byte (bitmask check, not exact comparison)
                if (index == 0 && val instanceof Byte b) {
                    boolean elytraBitSet = (b & ELYTRA_FLAG_BIT) != 0;

                    if (elytraBitSet) {
                        // Elytra activation (bit 7 set)
                        log.debug("[metadata] {} allowing self packet (flags=0x{}, elytra bit set)",
                                player.getUsername(), String.format("%02X", b));
                        allow = true;
                        continue;
                    }
                    if (!elytraBitSet && onGround && wasRecentlyFlying) {
                        // Elytra deactivation on landing (bit 7 cleared)
                        log.debug("[metadata] {} allowing self packet (flags=0x{}, elytra bit cleared, landing)",
                                player.getUsername(), String.format("%02X", b));
                        allow = true;
                        continue;
                    }
                }
            }

            if (allow) {
                return; // Don't cancel — let the packet through
            }

            // Block everything else
            logMetadataPacket(player, metadataPacket);
            event.setCancelled(true);
        });

        // Reduce sync frequency for modern clients
        handler.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                Player player = event.getPlayer();
                ClientVersionDetector detector = ClientVersionDetector.getInstance();

                if (detector.getClientVersion(player) == ClientVersionDetector.ClientVersion.MODERN) {
                    log.debug("Disabled metadata sync for modern client: {}",
                            player.getUsername());
                }
            }
        });
    }

    /**
     * Returns true if the player was in FALL_FLYING within the grace period.
     */
    private boolean wasRecentlyFlying(UUID uuid) {
        Long lastFlying = recentlyFlying.get(uuid);
        if (lastFlying == null) return false;
        return System.currentTimeMillis() - lastFlying < ELYTRA_GRACE_MS;
    }

    public String getStatistics() {
        ClientVersionDetector detector = ClientVersionDetector.getInstance();
        int modernClients = 0;
        int legacyClients = 0;

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (detector.getClientVersion(player) == ClientVersionDetector.ClientVersion.MODERN) {
                modernClients++;
            } else {
                legacyClients++;
            }
        }

        return String.format(
                "Modern Clients: %d (selective block)\n" +
                        "Legacy Clients: %d (metadata allowed)\n" +
                        "Currently Tracked Flying: %d\n",
                modernClients, legacyClients, recentlyFlying.size()
        );
    }

    /**
     * Logs each metadata entry (index + value) for a blocked self-metadata packet.
     */
    private void logMetadataPacket(Player player, EntityMetaDataPacket packet) {
        var entries = packet.entries();
        if (entries.isEmpty()) {
            log.debug("[metadata] {} BLOCKED self packet entityId={} entries=(none)",
                    player.getUsername(), packet.entityId());
            return;
        }
        var parts = new ArrayList<String>();
        for (var e : entries.entrySet()) {
            int index = e.getKey();
            var entry = e.getValue();
            Object val = entry.value();
            String valueDesc = val == null ? "null" : val.getClass().getSimpleName() + "=" + val;
            parts.add(index + ":" + valueDesc);
        }
        log.debug("[metadata] {} BLOCKED self packet entityId={} entries=[{}]",
                player.getUsername(), packet.entityId(), String.join(", ", parts));
    }
}
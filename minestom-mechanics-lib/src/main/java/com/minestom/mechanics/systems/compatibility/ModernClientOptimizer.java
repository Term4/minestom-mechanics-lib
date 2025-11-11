package com.minestom.mechanics.systems.compatibility;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;

// TODO: This could be noisy in production. Make sure A. this is actually necessary,
//  and B. make sure it only blocks sending metadata packets to itself.
//  Potentially could remove entirely, as the issues seen before (in blocking feature and bows)
//  COULD have been an issue with the ViewerBasedAnimationHandler, or something else in the features themselves
//  rather than actually with the server sending self metadata packets.

// TODO: Also move this to player package

/**
 * Prevents modern clients from receiving metadata packets about themselves.
 *
 * Problem:
 * - Modern clients handle all animations/poses client-side
 * - Server sending metadata packets causes conflicts
 * - Rapid state changes + lag = packet backlog = STUTTER
 *
 * Solution:
 * - Intercept EntityMetaDataPacket
 * - Drop if it's about the player themselves AND player is modern
 * - Result: Zero server interference = smooth experience
 */
public class ModernClientOptimizer {

    private static ModernClientOptimizer instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("ModernClientOptimizer");

    private ModernClientOptimizer() {}

    public static ModernClientOptimizer getInstance() {
        if (instance == null) {
            instance = new ModernClientOptimizer();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Intercept outgoing packets
        handler.addListener(PlayerPacketOutEvent.class, event -> {
            // Only check metadata packets
            if (!(event.getPacket() instanceof EntityMetaDataPacket metadataPacket)) {
                return;
            }

            Player player = event.getPlayer();

            // Check if packet is about the player themselves
            if (metadataPacket.entityId() != player.getEntityId()) {
                return; // Packet is about another entity, allow it
            }

            // Check if player is modern
            ClientVersionDetector detector = ClientVersionDetector.getInstance();
            if (detector.getClientVersion(player) != ClientVersionDetector.ClientVersion.MODERN) {
                return; // Legacy player, needs metadata packets
            }

            // BLOCK THE PACKET: Modern player receiving metadata about themselves
            event.setCancelled(true);

            /*log.debug("Blocked self-metadata packet for modern client: {}",
                    player.getUsername());

             */
        });

        // Reduce sync frequency for modern clients
        handler.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                Player player = event.getPlayer();
                ClientVersionDetector detector = ClientVersionDetector.getInstance();

                if (detector.getClientVersion(player) == ClientVersionDetector.ClientVersion.MODERN) {
                    // Modern clients don't need frequent metadata sync
                    // Set to very high value (effectively disable)
                    player.setSynchronizationTicks(Integer.MAX_VALUE);

                    log.debug("Disabled metadata sync for modern client: {}",
                            player.getUsername());
                }
            }
        });

        log.debug("Modern client optimizer initialized");
        log.debug("Self-metadata packets will be blocked for modern clients");
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
                "Modern Clients: %d (metadata blocked)\n" +
                        "Legacy Clients: %d (metadata allowed)\n",
                modernClients, legacyClients
        );
    }
}

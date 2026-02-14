package com.minestom.mechanics.systems.compatibility.legacy_1_8.fix;

import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.network.packet.server.play.EntityAttributesPacket;
import net.minestom.server.network.packet.server.play.UpdateHealthPacket;
import net.minestom.server.tag.Tag;

/**
 * For 1.8 clients: suppresses Update Health and EntityAttributes packets when applying
 * silent damage ({@code hurtEffect=false}). Health is sent via entity metadata (index 6)
 * instead, which updates the health bar without triggering screen tilt.
 * <p>
 * Only active during {@link com.minestom.mechanics.systems.health.HealthSystem#setHealthWithoutHurtEffect}.
 * The flag stays set for the whole setHealth+sendMetadata block so multiple or deferred
 * packets are all suppressed.
 */
public class LegacyHurtSuppression {

    private static LegacyHurtSuppression instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("LegacyHurtSuppression");

    /** When true, suppress ALL UpdateHealth/EntityAttributes packets for this player (1.8 only) until cleared. */
    public static final Tag<Boolean> SUPPRESS_HEALTH_PACKETS = Tag.Transient("legacy_suppress_health_packet");

    private LegacyHurtSuppression() {}

    public static LegacyHurtSuppression getInstance() {
        if (instance == null) {
            instance = new LegacyHurtSuppression();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        var handler = MinecraftServer.getGlobalEventHandler();
        handler.addListener(PlayerPacketOutEvent.class, this::onPacketOut);
        handler.addListener(PlayerDisconnectEvent.class, e -> e.getPlayer().removeTag(SUPPRESS_HEALTH_PACKETS));
        log.debug("Legacy hurt suppression initialized (metadata-only health for 1.8)");
    }

    private void onPacketOut(PlayerPacketOutEvent event) {
        Player player = event.getPlayer();
        if (!Boolean.TRUE.equals(player.getTag(SUPPRESS_HEALTH_PACKETS))) return;
        ClientVersionDetector detector = ClientVersionDetector.getInstance();
        if (detector.getClientVersion(player) != ClientVersionDetector.ClientVersion.LEGACY) return;

        var packet = event.getPacket();
        boolean suppress = false;
        if (packet instanceof UpdateHealthPacket) {
            suppress = true;
        } else if (packet instanceof EntityAttributesPacket attr && attr.entityId() == player.getEntityId()) {
            suppress = true;
        }

        if (suppress) {
            event.setCancelled(true);
        }
    }

    /**
     * When true, suppress all health-related packets (UpdateHealth, EntityAttributes for self)
     * for this player until cleared. Call before setHealth when using metadata path.
     */
    public static void setSuppressNextHealthPacket(Player player, boolean suppress) {
        if (suppress) {
            player.setTag(SUPPRESS_HEALTH_PACKETS, true);
        } else {
            player.removeTag(SUPPRESS_HEALTH_PACKETS);
        }
    }
}

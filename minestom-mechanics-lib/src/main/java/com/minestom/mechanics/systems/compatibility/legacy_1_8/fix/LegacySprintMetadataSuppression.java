package com.minestom.mechanics.systems.compatibility.legacy_1_8.fix;

import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.manager.CombatManager;
import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.network.packet.server.play.EntityAttributesPacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.kyori.adventure.key.Key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For 1.8 clients: suppresses sprint-related metadata and attribute packets that cause
 * latency-dependent hit slowdown. See <a href="https://github.com/Beaness/BetterSlowdown">BetterSlowdown</a>.
 * <p>
 * When the server calls {@code setSprinting(false)} on hit (or echoes sprint state back),
 * it sends metadata/attribute packets. Due to network latency, these arrive at arbitrary
 * client ticks, making hit slowdown (40% motion reduction) inconsistent. This fix strips
 * sprint from those packets so the client determines slowdown from its own input state.
 */
public class LegacySprintMetadataSuppression {

    private static LegacySprintMetadataSuppression instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("LegacySprintMetadataSuppression");

    /** Entity flags byte index 0: sprint bit (0x08). */
    private static final byte FLAG_SPRINTING = 0x08;

    /** Key for vanilla sprint modifier - minecraft:sprinting. */
    private static final Key SPRINT_MODIFIER_KEY = Key.key("minecraft", "sprinting");

    private LegacySprintMetadataSuppression() {}

    public static LegacySprintMetadataSuppression getInstance() {
        if (instance == null) {
            instance = new LegacySprintMetadataSuppression();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        var handler = MinecraftServer.getGlobalEventHandler();
        handler.addListener(PlayerPacketOutEvent.class, this::onPacketOut);
        log.debug("Legacy sprint metadata suppression initialized (1.8 hit slowdown fix)");
    }

    private void onPacketOut(PlayerPacketOutEvent event) {
        Player player = event.getPlayer();

        CombatConfig combatConfig = CombatManager.getInstance().getCurrentConfig();
        if (combatConfig != null && !combatConfig.fix18HitSlowdown()) {
            return;
        }

        ClientVersionDetector detector = ClientVersionDetector.getInstance();
        if (detector.getClientVersion(player) != ClientVersionDetector.ClientVersion.LEGACY) {
            return;
        }

        var packet = event.getPacket();

        if (packet instanceof EntityMetaDataPacket metadataPacket) {
            handleMetadataPacket(event, player, metadataPacket);
        } else if (packet instanceof EntityAttributesPacket attributesPacket) {
            handleAttributesPacket(event, player, attributesPacket);
        }
    }

    /**
     * Strip sprint from self-metadata (index 0 bitmask). Cancel entirely if index 0 is the
     * only entry; otherwise send modified packet without index 0.
     */
    private void handleMetadataPacket(PlayerPacketOutEvent event, Player player, EntityMetaDataPacket packet) {
        if (packet.entityId() != player.getEntityId()) {
            return;
        }

        var entries = packet.entries();
        if (!entries.containsKey(0)) {
            return;
        }

        Object val = entries.get(0).value();
        if (!(val instanceof Byte b)) {
            return;
        }

        if ((b & FLAG_SPRINTING) == 0) {
            return;
        }

        log.debug("[LegacySprintMetadataSuppression] {} stripping sprint from self-metadata (bitmask=0x{})",
                player.getUsername(), String.format("%02X", b));

        event.setCancelled(true);

        if (entries.size() == 1) {
            return;
        }

        Map<Integer, net.minestom.server.entity.Metadata.Entry<?>> newEntries = new HashMap<>(entries);
        newEntries.remove(0);
        player.sendPacket(new EntityMetaDataPacket(packet.entityId(), Map.copyOf(newEntries)));
    }

    /**
     * Remove sprint modifier from MOVEMENT_SPEED attribute packet. Cancel if the packet
     * becomes redundant (only sprint modifier change).
     */
    private void handleAttributesPacket(PlayerPacketOutEvent event, Player player, EntityAttributesPacket packet) {
        if (packet.entityId() != player.getEntityId()) {
            return;
        }

        if (Boolean.TRUE.equals(player.getTag(LegacyHurtSuppression.SUPPRESS_HEALTH_PACKETS))) {
            return;
        }

        List<EntityAttributesPacket.Property> newProperties = new ArrayList<>();
        boolean hasMovementSpeedChange = false;

        for (var prop : packet.properties()) {
            if (prop.attribute() != Attribute.MOVEMENT_SPEED) {
                newProperties.add(prop);
                continue;
            }

            List<AttributeModifier> filtered = prop.modifiers().stream()
                    .filter(mod -> !isSprintModifier(mod))
                    .toList();

            if (filtered.size() == prop.modifiers().size()) {
                newProperties.add(prop);
                continue;
            }

            hasMovementSpeedChange = true;
            newProperties.add(new EntityAttributesPacket.Property(prop.attribute(), prop.value(), filtered));
        }

        if (!hasMovementSpeedChange) {
            return;
        }

        log.debug("[LegacySprintMetadataSuppression] {} stripping sprint modifier from attribute packet",
                player.getUsername());

        event.setCancelled(true);
        player.sendPacket(new EntityAttributesPacket(packet.entityId(), newProperties));
    }

    private boolean isSprintModifier(AttributeModifier mod) {
        Key id = mod.id();
        if (id == null) return false;
        if (SPRINT_MODIFIER_KEY.equals(id)) return true;
        String keyStr = id.asString();
        return keyStr != null && keyStr.contains("sprint");
    }
}

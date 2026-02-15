package com.minestom.mechanics.systems.blocking;

import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.systems.blocking.tags.BlockableTagValue;
import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import com.minestom.mechanics.systems.compatibility.legacy_1_8.fix.LegacyAnimationFix;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.event.inventory.InventoryItemChangeEvent;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientPlayerActionPacket;
import net.minestom.server.network.packet.client.play.ClientUseItemPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.minestom.mechanics.config.constants.CombatConstants.LEGACY_BLOCKING_SPEED_MULTIPLIER;

/**
 * Blocking system - state tracking only. Gives blocking tag to players currently blocking,
 * removes it when they stop. Damage/knockback reduction is applied by damage and knockback systems.
 */
public class BlockingSystem extends InitializableSystem {
    private static BlockingSystem instance;

    private static final LogUtil.SystemLogger log = LogUtil.system("BlockingSystem");

    /** Player tag: true when blocking. Source of truth for blocking state. */
    public static final Tag<Boolean> BLOCKING = Tag.Boolean("blocking").defaultValue(false);

    /** Serialized tag for items — presence marks item as blockable. */
    public static final Tag<BlockableTagValue> BLOCKABLE =
            Tag.Structure("blockable", new com.minestom.mechanics.systems.blocking.tags.BlockableTagSerializer());

    /** Applied legacy slowdown modifiers so we can remove by modifier reference. */
    private final Map<UUID, AttributeModifier> legacySlowdownModifiers = new ConcurrentHashMap<>();

    private final CombatConfig config;
    private boolean runtimeEnabled = true;

    private BlockingSystem(CombatConfig config) {
        this.config = config;
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static BlockingSystem initialize(CombatConfig config) {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("BlockingSystem");
            return instance;
        }

        instance = new BlockingSystem(config);
        instance.registerListeners();
        instance.markInitialized();

        LogUtil.logInit("BlockingSystem");
        return instance;
    }

    public static BlockingSystem getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BlockingSystem not initialized! Call initialize() first.");
        }
        return instance;
    }

    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Input: start blocking on use item, stop on release
        handler.addListener(PlayerPacketEvent.class, this::handlePlayerPacket);
        handler.addListener(PlayerChangeHeldSlotEvent.class, event -> {
            if (isBlocking(event.getPlayer())) stopBlocking(event.getPlayer());
        });
        handler.addListener(PlayerDeathEvent.class, event -> {
            if (isBlocking(event.getPlayer())) stopBlocking(event.getPlayer());
        });
        // Cleanup on disconnect is handled by CombatManager.cleanupPlayer via system registry

        // Auto-apply blockable preset to sword/etc when placed in inventory
        handler.addListener(InventoryItemChangeEvent.class, this::onInventoryItemChange);
    }

    private void handlePlayerPacket(PlayerPacketEvent event) {
        if (!config.blockingEnabled()) return;

        Player player = event.getPlayer();

        if (event.getPacket() instanceof ClientUseItemPacket) {
            ItemStack mainHand = player.getItemInMainHand();
            if (mainHand != null && !mainHand.isAir() && mainHand.getTag(BLOCKABLE) != null
                    && !isBlocking(player)) {
                startBlocking(player);
            }
        } else if (event.getPacket() instanceof ClientPlayerActionPacket digging) {
            if (digging.status() == ClientPlayerActionPacket.Status.UPDATE_ITEM_STATE && isBlocking(player)) {
                stopBlocking(player);
            }
        }
    }

    private void onInventoryItemChange(InventoryItemChangeEvent event) {
        if (!config.blockingEnabled() || !runtimeEnabled) return;
        var inv = event.getInventory();
        boolean isPlayerInventory = false;
        for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (p.getInventory() == inv) {
                isPlayerInventory = true;
                break;
            }
        }
        if (!isPlayerInventory) return;
        ItemStack item = event.getInventory().getItemStack(event.getSlot());
        ItemStack replacement = BlockableItem.applyPresetIfNeeded(item);
        if (replacement != item) {
            inv.setItemStack(event.getSlot(), replacement);
        }
    }

    // ===========================
    // RUNTIME
    // ===========================

    public void setRuntimeEnabled(boolean enabled) {
        this.runtimeEnabled = enabled;
        if (!enabled) {
            for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                if (isBlocking(player)) stopBlocking(player);
            }
        }
        log.debug("Runtime blocking toggle: {}", enabled);
    }

    // ===========================
    // BLOCKING MECHANICS (state tracking)
    // ===========================

    public void startBlocking(Player player) {
        if (!config.blockingEnabled()) return;
        if (isBlocking(player)) return;

        player.setTag(BLOCKING, true);
        LegacyAnimationFix.getInstance().registerAnimation(player, "blocking", this::isBlocking);
        applyLegacyBlockingSlowdownIfNeeded(player);

        log.debug("{} started blocking", player.getUsername());
    }

    public void stopBlocking(Player player) {
        if (!isBlocking(player)) return;

        removeLegacyBlockingSlowdown(player);
        LegacyAnimationFix.getInstance().unregisterAnimation(player);
        player.setTag(BLOCKING, false);

        log.debug("{} stopped blocking", player.getUsername());
    }

    public boolean isBlocking(Player player) {
        return Boolean.TRUE.equals(player.getTag(BLOCKING));
    }

    private void applyLegacyBlockingSlowdownIfNeeded(Player player) {
        if (ClientVersionDetector.getInstance().getClientVersion(player) != ClientVersionDetector.ClientVersion.LEGACY) return;
        ItemStack mainHand = player.getItemInMainHand();
        if (mainHand == null || mainHand.isAir()) return;
        BlockableTagValue tag = mainHand.getTag(BLOCKABLE);
        if (tag == null || tag.applyLegacySlowdown() != Boolean.TRUE) return;

        double amount = LEGACY_BLOCKING_SPEED_MULTIPLIER - 1.0;
        AttributeModifier modifier = new AttributeModifier("blocking_slowdown", amount, AttributeOperation.ADD_MULTIPLIED_TOTAL);
        player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(modifier);
        legacySlowdownModifiers.put(player.getUuid(), modifier);
    }

    private void removeLegacyBlockingSlowdown(Player player) {
        AttributeModifier modifier = legacySlowdownModifiers.remove(player.getUuid());
        if (modifier != null) {
            player.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(modifier);
        }
    }

    // ===========================
    // CONFIGURATION ACCESS
    // ===========================

    private Double damageReductionOverride = null;
    private Double knockbackHMultiplierOverride = null;
    private Double knockbackVMultiplierOverride = null;

    public CombatConfig getConfig() {
        return config;
    }

    public boolean isEnabled() {
        return config.blockingEnabled() && runtimeEnabled;
    }

    public double getDamageReduction(@Nullable Player player) {
        double configReduction = damageReductionOverride != null ? damageReductionOverride : config.blockDamageReduction();
        if (player == null || !isBlocking(player)) return configReduction;
        ItemStack mainHand = player.getItemInMainHand();
        if (mainHand == null || mainHand.isAir()) return configReduction;
        BlockableTagValue tag = mainHand.getTag(BLOCKABLE);
        if (tag == null) return configReduction;
        return tag.damageReduction() != null ? tag.damageReduction() : configReduction;
    }

    public double getKnockbackHorizontalReduction(@Nullable Player player) {
        double configReduction = knockbackHMultiplierOverride != null ? 1.0 - knockbackHMultiplierOverride : config.blockKnockbackHReduction();
        if (player == null || !isBlocking(player)) return configReduction;
        ItemStack mainHand = player.getItemInMainHand();
        if (mainHand == null || mainHand.isAir()) return configReduction;
        BlockableTagValue tag = mainHand.getTag(BLOCKABLE);
        if (tag == null) return configReduction;
        if (tag.knockbackHMultiplier() != null) return 1.0 - tag.knockbackHMultiplier();
        return configReduction;
    }

    public double getKnockbackVerticalReduction(@Nullable Player player) {
        double configReduction = knockbackVMultiplierOverride != null ? 1.0 - knockbackVMultiplierOverride : config.blockKnockbackVReduction();
        if (player == null || !isBlocking(player)) return configReduction;
        ItemStack mainHand = player.getItemInMainHand();
        if (mainHand == null || mainHand.isAir()) return configReduction;
        BlockableTagValue tag = mainHand.getTag(BLOCKABLE);
        if (tag == null) return configReduction;
        if (tag.knockbackVMultiplier() != null) return 1.0 - tag.knockbackVMultiplier();
        return configReduction;
    }

    public void setDamageReduction(double reduction) {
        this.damageReductionOverride = reduction;
    }

    public void setKnockbackHorizontalReduction(double reduction) {
        this.knockbackHMultiplierOverride = 1.0 - reduction;
    }

    public void setKnockbackVerticalReduction(double reduction) {
        this.knockbackVMultiplierOverride = 1.0 - reduction;
    }

    // ===========================
    // CLEANUP
    // ===========================

    public void cleanup(Player player) {
        removeLegacyBlockingSlowdown(player);
        if (isBlocking(player)) stopBlocking(player);
        player.removeTag(BLOCKING);
        log.debug("Cleaned up blocking state for: {}", player.getUsername());
    }

    @Override
    public void cleanupPlayer(Player player) {
        cleanup(player);
    }

    public int getActiveCount() {
        int count = 0;
        for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (isBlocking(p)) count++;
        }
        return count;
    }

    public void shutdown() {
        log.info("Shutting down BlockingSystem");
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (isBlocking(player)) stopBlocking(player);
            cleanup(player);
        }
        log.info("BlockingSystem shutdown complete");
    }
}

package com.minestom.mechanics.systems.blocking;

import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.config.blocking.BlockingPreferences;

import com.minestom.mechanics.systems.blocking.tags.BlockableTagValue;
import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.event.inventory.InventoryItemChangeEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.minestom.mechanics.config.constants.CombatConstants.LEGACY_BLOCKING_SPEED_MULTIPLIER;

// TODO: Update to use TAG based tracking instead of a hashmap

/**
 * Main blocking system orchestrator - coordinates all blocking components.
 * Replaces the monolithic BlockingFeature with focused component architecture.
 */
public class BlockingSystem extends InitializableSystem {
    private static BlockingSystem instance;

    private static final LogUtil.SystemLogger log = LogUtil.system("BlockingSystem");

    /** Serialized tag for items — presence marks item as blockable. */
    public static final Tag<BlockableTagValue> BLOCKABLE =
            Tag.Structure("blockable", new com.minestom.mechanics.systems.blocking.tags.BlockableTagSerializer());

    /** Applied legacy slowdown modifiers so we can remove by modifier reference. */
    private final Map<UUID, AttributeModifier> legacySlowdownModifiers = new ConcurrentHashMap<>();

    // Component references
    private final BlockingState stateManager;
    private final BlockingInputHandler inputHandler;
    private final BlockingModifiers modifiers;
    private final BlockingVisualEffects visualEffects;

    // Configuration
    private final CombatConfig config;

    // Runtime state
    private boolean runtimeEnabled = true;

    private BlockingSystem(CombatConfig config) {
        this.config = config;

        // Initialize components
        this.stateManager = new BlockingState(config);
        this.inputHandler = new BlockingInputHandler(config, this);
        this.modifiers = new BlockingModifiers(this, stateManager);
        this.visualEffects = new BlockingVisualEffects(config, stateManager);
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

        // Register input handler
        inputHandler.registerListeners();

        // Blocking damage reduction is applied inside HealthSystem's damage pipeline (DamageType.processDamage)
        // so it runs after weapon damage is calculated. Do NOT add EntityDamageEvent here - that runs before
        // the health pipeline and would see amount=0 for melee attacks.

        // When a player inventory slot is set with a plain sword (or other auto-preset material), apply blockable preset
        handler.addListener(InventoryItemChangeEvent.class, this::onInventoryItemChange);
    }

    private void onInventoryItemChange(InventoryItemChangeEvent event) {
        if (!config.blockingEnabled() || !runtimeEnabled) return;
        // Only apply auto-preset when the inventory is a player's own inventory
        var inv = event.getInventory();
        boolean isPlayerInventory = false;
        for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (p.getInventory() == inv) {
                isPlayerInventory = true;
                break;
            }
        }
        if (!isPlayerInventory) return;
        // Item in slot after the change (event fires when setItemStack is invoked)
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

        // If disabling, stop all active blocks
        if (!enabled) {
            for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                if (isBlocking(player)) {
                    stopBlocking(player);
                }
            }
        }
        log.debug("Runtime blocking toggle: {}", enabled);
    }

    // ===========================
    // BLOCKING MECHANICS
    // ===========================

    /**
     * Start blocking for a player
     */
    public void startBlocking(Player player) {
        if (!config.blockingEnabled()) return;
        if (stateManager.isBlocking(player)) return;

        stateManager.startBlocking(player);
        visualEffects.showBlockingMessage(player, true);
        visualEffects.sendBlockingEffects(player);
        applyLegacyBlockingSlowdownIfNeeded(player);

        log.debug("{} started blocking", player.getUsername());
    }

    /**
     * Stop blocking for a player
     */
    public void stopBlocking(Player player) {
        if (!stateManager.isBlocking(player)) return;

        removeLegacyBlockingSlowdown(player);
        stateManager.stopBlocking(player);
        visualEffects.showBlockingMessage(player, false);
        visualEffects.stopBlockingEffects(player);

        log.debug("{} stopped blocking", player.getUsername());
    }

    /**
     * Check if a player is blocking
     */
    public boolean isBlocking(Player player) {
        return stateManager.isBlocking(player);
    }

    /**
     * For legacy clients blocking with an item that has {@link BlockableTagValue#applyLegacySlowdown()} == true, apply movement slowdown.
     * Sword preset uses false (no slowdown); items like stick can set true in the tag.
     */
    private void applyLegacyBlockingSlowdownIfNeeded(Player player) {
        if (ClientVersionDetector.getInstance().getClientVersion(player) != ClientVersionDetector.ClientVersion.LEGACY) return;
        ItemStack mainHand = player.getItemInMainHand();
        if (mainHand == null || mainHand.isAir()) return;
        BlockableTagValue tag = mainHand.getTag(BLOCKABLE);
        if (tag == null || tag.applyLegacySlowdown() != Boolean.TRUE) return;

        double multiplier = LEGACY_BLOCKING_SPEED_MULTIPLIER;
        // ADD_MULTIPLIED_TOTAL: final = total * (1 + amount), so amount = multiplier - 1 to get total * multiplier
        double amount = multiplier - 1.0;
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
    // PREFERENCES MANAGEMENT
    // ===========================

    public void setPlayerPreferences(Player player, BlockingPreferences preferences) {
        player.setTag(BlockingState.PREFERENCES, preferences);
    }

    public BlockingPreferences getPlayerPreferences(Player player) {
        return player.getTag(BlockingState.PREFERENCES);
    }

    // ===========================
    // CONFIGURATION ACCESS
    // ===========================

    private Double damageReductionOverride = null;
    private Double knockbackHMultiplierOverride = null;
    private Double knockbackVMultiplierOverride = null;
    private Boolean showDamageMessagesOverride = null;
    private Boolean showBlockEffectsOverride = null;

    public CombatConfig getConfig() {
        return config;
    }

    public boolean isEnabled() {
        return config.blockingEnabled() && runtimeEnabled;
    }

    // TODO: Make reductions appear consistent. Seeinng 1 - reduction can be confusing.
    //  Just have 0.95 reduce by 95% (0.95), and whenever we present the user with the option to
    //  configure this, we present it like that. Now, we can use it HERE however we want to, but
    //  it's confusing to present the user with two different ways of presenting the same information.

    /**
     * Resolve damage reduction for a blocking player. When player is null or not blocking, returns config default.
     * When blocking, uses the main-hand item's BLOCKABLE tag if present, else config.
     */
    public double getDamageReduction(@Nullable Player player) {
        double configReduction = damageReductionOverride != null ? damageReductionOverride : config.blockDamageReduction();
        if (player == null || !stateManager.isBlocking(player)) return configReduction;
        ItemStack mainHand = player.getItemInMainHand();
        if (mainHand == null || mainHand.isAir()) return configReduction;
        BlockableTagValue tag = mainHand.getTag(BLOCKABLE);
        if (tag == null) return configReduction;
        return tag.damageReduction() != null ? tag.damageReduction() : configReduction;
    }

    /**
     * Resolve horizontal knockback reduction for a blocking player. When player is null or not blocking, returns config default.
     * Return value is "reduction" (0 = no reduction, 0.6 = 60% reduction).
     */
    public double getKnockbackHorizontalReduction(@Nullable Player player) {
        double configMult = knockbackHMultiplierOverride != null ? knockbackHMultiplierOverride : config.blockKnockbackHReduction();
        if (player == null || !stateManager.isBlocking(player)) return 1.0 - configMult;
        ItemStack mainHand = player.getItemInMainHand();
        if (mainHand == null || mainHand.isAir()) return 1.0 - configMult;
        BlockableTagValue tag = mainHand.getTag(BLOCKABLE);
        if (tag == null) return 1.0 - configMult;
        double mult = tag.knockbackHMultiplier() != null ? tag.knockbackHMultiplier() : configMult;
        return 1.0 - mult;
    }

    /**
     * Resolve vertical knockback reduction for a blocking player. When player is null or not blocking, returns config default.
     */
    public double getKnockbackVerticalReduction(@Nullable Player player) {
        double configMult = knockbackVMultiplierOverride != null ? knockbackVMultiplierOverride : config.blockKnockbackVReduction();
        if (player == null || !stateManager.isBlocking(player)) return 1.0 - configMult;
        ItemStack mainHand = player.getItemInMainHand();
        if (mainHand == null || mainHand.isAir()) return 1.0 - configMult;
        BlockableTagValue tag = mainHand.getTag(BLOCKABLE);
        if (tag == null) return 1.0 - configMult;
        double mult = tag.knockbackVMultiplier() != null ? tag.knockbackVMultiplier() : configMult;
        return 1.0 - mult;
    }

    public boolean shouldShowDamageMessages() {
        return showDamageMessagesOverride != null ? showDamageMessagesOverride : config.showBlockDamageMessages();
    }

    public boolean shouldShowBlockEffects() {
        return showBlockEffectsOverride != null ? showBlockEffectsOverride : config.showBlockEffects();
    }

    /**
     * Apply blocking damage reduction and feedback. Called from the damage pipeline (DamageType)
     * after weapon damage is calculated, so blocking sees the correct amount.
     *
     * @return the reduced damage amount, or the original if not blocking / not applicable
     */
    public float applyBlockingDamageReduction(Player victim, float currentAmount,
                                              net.minestom.server.registry.RegistryKey<?> damageType) {
        return modifiers.applyBlockingReduction(victim, currentAmount, damageType);
    }

    // Setters for runtime configuration
    public void setDamageReduction(double reduction) {
        this.damageReductionOverride = reduction;
    }

    public void setKnockbackHorizontalReduction(double reduction) {
        this.knockbackHMultiplierOverride = 1.0 - reduction;
    }

    public void setKnockbackVerticalReduction(double reduction) {
        this.knockbackVMultiplierOverride = 1.0 - reduction;
    }

    public void setShowDamageMessages(boolean show) {
        this.showDamageMessagesOverride = show;
    }

    public void setShowBlockEffects(boolean show) {
        this.showBlockEffectsOverride = show;
    }

    // ===========================
    // CLEANUP
    // ===========================

    /**
     * Clean up player data when they disconnect
     */
    public void cleanup(Player player) {
        removeLegacyBlockingSlowdown(player);
        stateManager.cleanup(player);
        visualEffects.cleanup(player);
        log.debug("Cleaned up blocking data for: {}", player.getUsername());
    }

    /**
     * Get active player count (for memory leak checking)
     */
    public int getActiveCount() {
        return stateManager.getActiveCount();
    }

    /**
     * Shutdown the blocking system
     */
    public void shutdown() {
        log.info("Shutting down BlockingSystem");

        // Stop all active blocking
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (isBlocking(player)) {
                stopBlocking(player);
            }
            cleanup(player);
        }

        // Clean up visual effects
        visualEffects.shutdown();

        log.info("BlockingSystem shutdown complete");
    }
}
package com.minestom.mechanics.systems.blocking;

import com.minestom.mechanics.systems.util.InitializableSystem;
import com.minestom.mechanics.systems.util.LogUtil;
import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.config.blocking.BlockingPreferences;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityDamageEvent;

// TODO: Maybe modify how we currently maintain blocking state visual sync across versions?
//  Instead of just listening to tick events (very intensive), we could listen to move events?
//  If not, we could also run a plugin proxy side for it, but it might be slightly more complicated
//  and less user friendly

/**
 * Main blocking system orchestrator - coordinates all blocking components.
 * Replaces the monolithic BlockingFeature with focused component architecture.
 */
public class BlockingSystem extends InitializableSystem {
    private static BlockingSystem instance;

    private static final LogUtil.SystemLogger log = LogUtil.system("BlockingSystem");

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

        // Register damage and velocity handlers
        handler.addListener(EntityDamageEvent.class, modifiers::handleDamage);
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
        visualEffects.updateBlockingVisuals(player, true);
        visualEffects.showBlockingMessage(player, true);
        visualEffects.sendBlockingEffects(player);  // ← Changed

        log.debug("{} started blocking", player.getUsername());
    }

    /**
     * Stop blocking for a player
     */
    public void stopBlocking(Player player) {
        if (!stateManager.isBlocking(player)) return;

        stateManager.stopBlocking(player);
        visualEffects.updateBlockingVisuals(player, false);
        visualEffects.showBlockingMessage(player, false);
        visualEffects.stopBlockingEffects(player);  // ← Changed

        log.debug("{} stopped blocking", player.getUsername());
    }

    /**
     * Check if a player is blocking
     */
    public boolean isBlocking(Player player) {
        return stateManager.isBlocking(player);
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

    // Convenience getters for blocking-specific values
    public double getDamageReduction() {
        return damageReductionOverride != null ? damageReductionOverride : config.blockDamageReduction();
    }

    public double getKnockbackHorizontalReduction() {
        double multiplier = knockbackHMultiplierOverride != null ? knockbackHMultiplierOverride : config.blockKnockbackHReduction();
        return 1.0 - multiplier;
    }

    public double getKnockbackVerticalReduction() {
        double multiplier = knockbackVMultiplierOverride != null ? knockbackVMultiplierOverride : config.blockKnockbackVReduction();
        return 1.0 - multiplier;
    }

    public boolean shouldShowDamageMessages() {
        return showDamageMessagesOverride != null ? showDamageMessagesOverride : config.showBlockDamageMessages();
    }

    public boolean shouldShowBlockEffects() {
        return showBlockEffectsOverride != null ? showBlockEffectsOverride : config.showBlockEffects();
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
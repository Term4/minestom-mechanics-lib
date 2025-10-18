package com.minestom.mechanics.features.blocking;

import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.config.blocking.BlockingConfig;
import com.minestom.mechanics.config.blocking.BlockingPreferences;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.entity.EntityVelocityEvent;

/**
 * Main blocking system orchestrator - coordinates all blocking components.
 * Replaces the monolithic BlockingFeature with focused component architecture.
 */
public class BlockingSystem extends InitializableSystem {
    private static BlockingSystem instance;

    private static final LogUtil.SystemLogger log = LogUtil.system("BlockingSystem");

    // Component references
    private final BlockingStateManager stateManager;
    private final BlockingInputHandler inputHandler;
    private final BlockingModifiers modifiers;
    private final BlockingVisualEffects visualEffects;

    // Configuration
    private final BlockingConfig config;

    private BlockingSystem(BlockingConfig config) {
        this.config = config;
        
        // Initialize components
        this.stateManager = new BlockingStateManager(config);
        this.inputHandler = new BlockingInputHandler(config, this);
        this.modifiers = new BlockingModifiers(config, stateManager);
        this.visualEffects = new BlockingVisualEffects(config, stateManager);
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static BlockingSystem initialize(BlockingConfig config) {
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
        handler.addListener(EntityVelocityEvent.class, modifiers::handleVelocity);
    }

    // ===========================
    // BLOCKING MECHANICS
    // ===========================

    /**
     * Start blocking for a player
     */
    public void startBlocking(Player player) {
        if (!config.isEnabled()) return;
        if (stateManager.isBlocking(player)) return;

        stateManager.startBlocking(player);
        visualEffects.updateBlockingVisuals(player, true);
        visualEffects.showBlockingMessage(player, true);
        visualEffects.startParticleTask(player);

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
        visualEffects.stopParticleTask(player);

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
        player.setTag(BlockingStateManager.PREFERENCES, preferences);
    }

    public BlockingPreferences getPlayerPreferences(Player player) {
        return player.getTag(BlockingStateManager.PREFERENCES);
    }

    // ===========================
    // CONFIGURATION
    // ===========================

    public BlockingConfig getConfig() {
        return config;
    }

    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);

        // If disabling, stop all active blocks
        if (!enabled) {
            for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                if (isBlocking(player)) {
                    stopBlocking(player);
                }
            }
        }
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public void setDamageReduction(double reduction) {
        config.setDamageReduction(reduction);
    }

    public void setKnockbackHorizontalReduction(double reduction) {
        config.setKnockbackHorizontalMultiplier(1.0 - reduction);
    }

    public void setKnockbackVerticalReduction(double reduction) {
        config.setKnockbackVerticalMultiplier(1.0 - reduction);
    }

    public double getDamageReduction() {
        return config.getDamageReduction();
    }

    public double getKnockbackHorizontalReduction() {
        return 1.0 - config.getKnockbackHorizontalMultiplier();
    }

    public double getKnockbackVerticalReduction() {
        return 1.0 - config.getKnockbackVerticalMultiplier();
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

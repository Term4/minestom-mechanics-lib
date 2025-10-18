package com.minestom.mechanics.manager;

import com.minestom.mechanics.attack.AttackFeature;
import com.minestom.mechanics.features.blocking.BlockingSystem;
import com.minestom.mechanics.config.combat.CombatModeBundle;
import com.minestom.mechanics.config.combat.CombatRulesConfig;
import com.minestom.mechanics.config.combat.CombatPresets;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

// TODO: This should simplify greatly by am update to our builder system.
//  As of right now, it's a bit of a mess, and very long.

/**
 * Combat Manager - Manages player vs player combat mechanics
 * 
 * REFACTORED: Now focused only on combat-specific systems
 * 
 * Manages:
 * - CombatSystem (player attacks and damage calculations)
 * - BlockingSystem (shield blocking mechanics)
 * 
 * Other systems have been extracted to specialized managers:
 * - ArmorManager: Armor damage reduction
 * - KnockbackManager: Knockback mechanics
 * - HitboxManager: Hit detection and validation
 * - GameplayManager: Player collisions and environmental damage
 */
public class CombatManager implements ManagerLifecycle {
    private static CombatManager instance;

    // Current configuration
    private CombatModeBundle currentBundle;

    // System references
    private AttackFeature attackFeature;
    private BlockingSystem blockingSystem;
    
    // State tracking
    private boolean initialized = false;
    private final LogUtil.SystemLogger log = LogUtil.system("CombatManager");

    private CombatManager() {
        // Private constructor for singleton
    }

    public static CombatManager getInstance() {
        if (instance == null) {
            instance = new CombatManager();
        }
        return instance;
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    /**
     * Initialize with a CombatModeBundle.
     * 
     * @param bundle The combat mode bundle to use
     * @return this manager for chaining
     */
    public CombatManager initialize(CombatModeBundle bundle) {
        this.currentBundle = bundle;
        
        try {
            log.info("Mode: {}", bundle.name());
            
            // Step 1: BlockingSystem (modifies damage when blocking)
            log.debug("Initializing BlockingSystem...");
            blockingSystem = BlockingSystem.initialize(bundle.blocking());
            
            // Step 2: AttackFeature (player attacks - main combat logic)
            log.debug("Initializing AttackFeature...");
            attackFeature = AttackFeature.initialize(bundle.combatRules());
            
            initialized = true;
            log.info("CombatManager initialized successfully");
            return this;
        } catch (Exception e) {
            log.error("Failed to initialize CombatManager: " + e.getMessage(), e);
            initialized = false;
            return this;
        }
    }
    
    @Override
    public boolean initialize() {
        return initialize(CombatPresets.Modes.MINEMEN).isInitialized();
    }
    
    /**
     * Get a configuration builder for custom combat setup.
     * 
     * @return a new ConfigurationBuilder instance
     */
    public ConfigurationBuilder configure() {
        if (initialized) {
            throw new IllegalStateException("Cannot configure after initialization! Call shutdown() first.");
        }
        return new ConfigurationBuilder();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String getStatus() {
        if (!initialized) {
            return ManagerUtils.buildStatus("CombatManager", false, "Not initialized");
        }

        StringBuilder status = new StringBuilder();
        status.append("Combat Systems Status:\n");
        status.append("  AttackFeature: ").append(attackFeature != null ? "✓" : "✗").append("\n");
        status.append("  BlockingSystem: ").append(blockingSystem != null ? "✓" : "✗").append("\n");

        return status.toString();
    }

    // ===========================
    // RUNTIME CONFIGURATION
    // ===========================

    /**
     * Enable or disable blocking at runtime
     */
    public void setBlockingEnabled(boolean enabled) {
        if (!initialized) {
            throw new IllegalStateException("CombatManager not initialized");
        }
        blockingSystem.setEnabled(enabled);
        log.info("Blocking {}", enabled ? "enabled" : "disabled");
    }

    // ===========================
    // CLEANUP
    // ===========================

    @Override
    public void cleanupPlayer(Player player) {
        if (!initialized) return;

        ManagerUtils.safeCleanup(() -> {
            if (blockingSystem != null) {
                blockingSystem.cleanup(player);
            }
            if (attackFeature != null) {
                attackFeature.cleanup(player);
            }
        }, "player cleanup", log);
    }

    @Override
    public void shutdown() {
        ManagerUtils.safeShutdown(() -> {
            // Cleanup all players first
            MinecraftServer.getConnectionManager().getOnlinePlayers()
                    .forEach(this::cleanupPlayer);

            // Shutdown each system
            if (blockingSystem != null) {
                blockingSystem.shutdown();
            }

            if (attackFeature != null) {
                attackFeature.shutdown();
            }

            // Reset references
            attackFeature = null;
            blockingSystem = null;
            initialized = false;
        }, "CombatManager", log);
    }

    // ===========================
    // SYSTEM ACCESS (for advanced use)
    // ===========================

    public AttackFeature getAttackFeature() {
        if (!initialized) {
            throw new IllegalStateException("CombatManager not initialized");
        }
        return attackFeature;
    }

    public BlockingSystem getBlockingSystem() {
        if (!initialized) {
            throw new IllegalStateException("CombatManager not initialized");
        }
        return blockingSystem;
    }

    /**
     * Get the current combat mode bundle
     */
    public CombatModeBundle getCurrentBundle() {
        if (!initialized) {
            throw new IllegalStateException("CombatManager not initialized");
        }
        return currentBundle;
    }
    
    // ===========================
    // CONFIGURATION BUILDER
    // ===========================
    
    /**
     * Fluent API for configuring combat systems.
     * Allows users to:
     * - Start from a preset and override specific parts
     * - Mix and match different presets
     * - Build everything from scratch
     */
    public class ConfigurationBuilder {
        private String name;
        private String description;
        private CombatRulesConfig combatRules;
        private com.minestom.mechanics.config.blocking.BlockingConfig blocking;
        private com.minestom.mechanics.config.combat.ProjectileConfig projectiles;
        
        private ConfigurationBuilder() {
            // Start with default MINEMEN preset
            CombatModeBundle defaultBundle = CombatPresets.Modes.MINEMEN;
            this.name = defaultBundle.name();
            this.description = defaultBundle.description();
            this.combatRules = defaultBundle.combatRules();
            this.blocking = defaultBundle.blocking();
            this.projectiles = defaultBundle.projectiles();
        }
        
        /**
         * Start from a full preset and optionally override parts.
         * 
         * @param preset The preset to start from
         * @return this builder for chaining
         */
        public ConfigurationBuilder fromPreset(CombatModeBundle preset) {
            this.name = preset.name();
            this.description = preset.description();
            this.combatRules = preset.combatRules();
            this.blocking = preset.blocking();
            this.projectiles = preset.projectiles();
            return this;
        }
        
        /**
         * Override combat rules configuration.
         * 
         * @param combatRules The combat rules to use
         * @return this builder for chaining
         */
        public ConfigurationBuilder withCombatRules(CombatRulesConfig combatRules) {
            this.combatRules = combatRules;
            return this;
        }
        
        /**
         * Override blocking configuration.
         * 
         * @param blocking The blocking config to use
         * @return this builder for chaining
         */
         public ConfigurationBuilder withBlocking(com.minestom.mechanics.config.blocking.BlockingConfig blocking) {
            this.blocking = blocking;
            return this;
        }
        
        /**
         * Set a custom name and description for this configuration.
         * 
         * @param name The name for this configuration
         * @param description The description for this configuration
         * @return this builder for chaining
         */
        public ConfigurationBuilder withName(String name, String description) {
            this.name = name;
            this.description = description;
            return this;
        }
        
        /**
         * Apply the configuration and initialize the combat systems.
         * 
         * @return the CombatManager instance
         */
        public CombatManager apply() {
            CombatModeBundle bundle = new CombatModeBundle(
                name, description, combatRules, blocking, projectiles
            );
            return CombatManager.this.initialize(bundle);
        }
    }
}

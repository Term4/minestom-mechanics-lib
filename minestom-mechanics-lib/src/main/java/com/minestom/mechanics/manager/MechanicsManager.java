package com.minestom.mechanics.manager;

import com.minestom.mechanics.config.MechanicsPresets;
import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.config.gameplay.DamageConfig;
import com.minestom.mechanics.config.timing.TickScalingConfig;
import com.minestom.mechanics.config.timing.TickScalingMode;
import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.config.gameplay.GameplayConfig;
import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.systems.knockback.KnockbackSystem;
import com.minestom.mechanics.systems.player.PlayerDeathHandler;
import com.minestom.mechanics.systems.misc.VelocityEstimator;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

// TODO: Could change a lot with proposed builder update.
//  ALSO a lot of getters / logging logic could probably be generalized for
//  simplification, and for shortening code. This class has a ton of lines,
//  and a lot of it is nearly identical code, just for different systems.

/**
 * MechanicsManager - Master orchestrator for all game mechanics
 * 
 * Provides a unified interface to initialize and manage all game systems:
 * - Combat mechanics (CombatManager)
 * - Gameplay mechanics (GameplayManager)
 * - Hitbox detection (HitboxManager)
 * - Armor calculations (ArmorManager)
 * - Knockback mechanics (KnockbackManager)
 * 
 * Supports:
 * - Preset configurations (VANILLA, MINEMEN, etc.)
 * - Selective initialization (use only what you need)
 * - Runtime configuration changes
 * - Coordinated cleanup and shutdown
 */
public class MechanicsManager {
    private static MechanicsManager instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("MechanicsManager");
    
    private boolean initialized = false;
    
    // Manager references
    private CombatManager combatManager;
    private GameplayManager gameplayManager;
    private HealthSystem healthSystem;
    private HitboxManager hitboxManager;
    private ArmorManager armorManager;
    private KnockbackSystem knockbackSystem;

    // Track which systems are enabled
    private boolean combatEnabled = false;
    private boolean gameplayEnabled = false;
    private boolean damageEnabled = false;
    private boolean hitboxEnabled = false;
    private boolean armorEnabled = false;
    private boolean knockbackEnabled = false;
    
    private MechanicsManager() {}
    
    public static MechanicsManager getInstance() {
        if (instance == null) {
            instance = new MechanicsManager();
        }
        return instance;
    }
    
    // ===========================
    // PRESET INITIALIZATION
    // ===========================
    
    /**
     * Initialize with a preset configuration.
     * This initializes all systems with matching configurations.
     * 
     * @param preset The preset to use
     * @return this manager for chaining
     */
    public MechanicsManager withPreset(MechanicsPresets preset) {
        if (initialized) {
            throw new IllegalStateException("Already initialized! Call shutdown() first.");
        }
        
        log.info("Initializing with preset: {}", preset.getName());
        
        // Initialize all systems with preset configurations
        ConfigurationBuilder builder = configure()
                .withCombat(preset.getCombatConfig())
                .withGameplay(preset.getGameplayConfig())
                .withDamage(preset.getDamageConfig())
                .withHitbox(preset.getHitDetectionConfig())
                .withArmor(preset.isArmorEnabled())
                .withKnockback(preset.getKnockbackConfig());
        
        return builder.initialize();
    }
    
    /**
     * Get a configuration builder for custom setup.
     * 
     * @return a new ConfigurationBuilder instance
     */
    public ConfigurationBuilder configure() {
        if (initialized) {
            throw new IllegalStateException("Cannot configure after initialization! Call shutdown() first.");
        }
        return new ConfigurationBuilder();
    }
    
    // ===========================
    // RUNTIME OPERATIONS
    // ===========================
    
    /**
     * Check if the manager is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Clean up player data across all systems
     */
    public void cleanupPlayer(Player player) {
        if (!initialized) return;
        
        if (combatEnabled && combatManager != null) {
            combatManager.cleanupPlayer(player);
        }
        if (gameplayEnabled && gameplayManager != null) {
            gameplayManager.cleanupPlayer(player);
        }
        if (damageEnabled && healthSystem != null) {
            healthSystem.cleanup(player);
        }
        if (hitboxEnabled && hitboxManager != null) {
            hitboxManager.cleanupPlayer(player);
        }
        if (armorEnabled && armorManager != null) {
            armorManager.cleanupPlayer(player);
        }
        // KnockbackSystem doesn't track player data (for now) so doesn't need cleanup
        //  if (knockbackEnabled && knockbackSystem != null) {
        //    knockbackSystem.cleanup(player);
        //  }
    }
    
    /**
     * Shutdown all systems
     */
    public void shutdown() {
        if (!initialized) {
            log.warn("Not initialized, nothing to shutdown");
            return;
        }
        
        log.info("Shutting down...");
        
        // Cleanup all players first
        MinecraftServer.getConnectionManager().getOnlinePlayers()
                .forEach(this::cleanupPlayer);
        
        // Shutdown each enabled system
        if (combatEnabled && combatManager != null) {
            try {
                combatManager.shutdown();
                log.info("Combat systems shut down");
            } catch (Exception e) {
                log.error("Combat shutdown failed", e);
            }
        }

        if (gameplayEnabled && gameplayManager != null) {
            try {
                gameplayManager.shutdown();
                log.info("Gameplay systems shut down");
            } catch (Exception e) {
                log.error("Gameplay shutdown failed", e);
            }
        }

        if (damageEnabled && healthSystem != null) {
            try {
                healthSystem.shutdown();
                log.info("Health system shut down");
            } catch (Exception e) {
                log.error("Health system shutdown failed", e);
            }
        }
        
        if (hitboxEnabled && hitboxManager != null) {
            try {
                hitboxManager.shutdown();
                log.info("Hitbox systems shut down");
            } catch (Exception e) {
                log.error("Hitbox shutdown failed", e);
            }
        }
        
        if (armorEnabled && armorManager != null) {
            try {
                armorManager.shutdown();
                log.info("Armor system shut down");
            } catch (Exception e) {
                log.error("Armor shutdown failed", e);
            }
        }

        if (knockbackEnabled && knockbackSystem != null) {
            try {
                knockbackSystem.shutdown();
                log.info("Knockback system shut down");
            } catch (Exception e) {
                log.error("Knockback shutdown failed", e);
            }
        }

        // Reset references
        combatManager = null;
        gameplayManager = null;
        healthSystem = null;
        hitboxManager = null;
        armorManager = null;
        knockbackSystem = null;

        combatEnabled = false;
        gameplayEnabled = false;
        damageEnabled = false;
        hitboxEnabled = false;
        armorEnabled = false;
        knockbackEnabled = false;
        
        initialized = false;
        
        log.info("All systems shut down successfully");
    }
    
    // ===========================
    // SYSTEM ACCESS
    // ===========================
    
    public CombatManager getCombatManager() {
        if (!combatEnabled) {
            throw new IllegalStateException("Combat system not enabled!");
        }
        return combatManager;
    }
    
    public GameplayManager getGameplayManager() {
        if (!gameplayEnabled) {
            throw new IllegalStateException("Gameplay system not enabled!");
        }
        return gameplayManager;
    }

    /**
     * Get the health system instance
     */
    public HealthSystem getHealthSystem() {
        if (!damageEnabled) {
            throw new IllegalStateException("Health system not enabled!");
        }
        return healthSystem;
    }
    
    public HitboxManager getHitboxManager() {
        if (!hitboxEnabled) {
            throw new IllegalStateException("Hitbox system not enabled!");
        }
        return hitboxManager;
    }
    
    public ArmorManager getArmorManager() {
        if (!armorEnabled) {
            throw new IllegalStateException("Armor system not enabled!");
        }
        return armorManager;
    }

    public KnockbackSystem getKnockbackHandler() {
        if (!knockbackEnabled) {
            throw new IllegalStateException("Knockback system not enabled!");
        }
        return knockbackSystem;
    }

    /**
     * @deprecated Use {@link #getKnockbackHandler()} instead. KnockbackManager is deprecated.
     */
    @Deprecated
    public KnockbackSystem getKnockbackManager() {
        return getKnockbackHandler();
    }
    
    /**
     * Get a status report of all systems
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Mechanics Manager Status:\n");
        status.append("  Initialized: ").append(initialized ? "✓" : "✗").append("\n");

        if (initialized) {
            status.append("\nEnabled Systems:\n");
            status.append("  Combat: ").append(combatEnabled ? "✓" : "✗").append("\n");
            status.append("  Gameplay: ").append(gameplayEnabled ? "✓" : "✗").append("\n");
            status.append("  Damage: ").append(damageEnabled ? "✓" : "✗").append("\n");
            status.append("  Hitbox: ").append(hitboxEnabled ? "✓" : "✗").append("\n");
            status.append("  Armor: ").append(armorEnabled ? "✓" : "✗").append("\n");
            status.append("  Knockback: ").append(knockbackEnabled ? "✓" : "✗").append("\n");
            
            if (combatEnabled) {
                status.append("\n").append(combatManager.getStatus());
            }
            if (gameplayEnabled) {
                status.append("\n").append(gameplayManager.getStatus());
            }
            if (hitboxEnabled) {
                status.append("\n").append(hitboxManager.getStatus());
            }
            if (armorEnabled) {
                status.append("\n").append(armorManager.getStatus());
            }
            if (knockbackEnabled) {
                status.append("\n  KnockbackHandler: ✓");
            }
        }
        
        return status.toString();
    }
    
    // ===========================
    // CONFIGURATION BUILDER
    // ===========================
    
    /**
     * Fluent API for configuring which systems to initialize
     */
    public class ConfigurationBuilder {
        // Combat configuration
        private CombatConfig combatConfig = null;

        // Gameplay configuration
        private GameplayConfig gameplayConfig = null;

        // Damage configuration
        private DamageConfig damageConfig = null;
        
        // Hitbox configuration
        private HitDetectionConfig hitDetectionConfig = null;
        
        // Armor configuration
        private boolean armorEnabled = true;
        
        // Knockback configuration
        private KnockbackConfig knockbackConfig = null;
        private boolean knockbackSyncEnabled = false;

        // Tick scaling (SCALED = real-time durations; UNSCALED = literal tick counts)
        private TickScalingMode tickScalingMode = TickScalingMode.SCALED;

        private ConfigurationBuilder() {}

        /**
         * Enable gameplay systems with the specified config
         */
        public ConfigurationBuilder withGameplay(GameplayConfig config) {
            this.gameplayConfig = config;
            return this;
        }

        /**
         * Enable combat systems with the specified config
         */
        public ConfigurationBuilder withCombat(CombatConfig config) {
            this.combatConfig = config;
            return this;
        }

        /**
         * Enable damage system with the specified config
         */
        public ConfigurationBuilder withDamage(DamageConfig config) {
            this.damageConfig = config;
            return this;
        }
        
        /**
         * Enable hitbox detection with the specified config
         */
        public ConfigurationBuilder withHitbox(HitDetectionConfig config) {
            this.hitDetectionConfig = config;
            return this;
        }
        
        /**
         * Enable armor system
         */
        public ConfigurationBuilder withArmor() {
            this.armorEnabled = true;
            return this;
        }
        
        /**
         * Set armor system enabled/disabled
         */
        public ConfigurationBuilder withArmor(boolean enabled) {
            this.armorEnabled = enabled;
            return this;
        }
        
        /**
         * Disable armor system
         */
        public ConfigurationBuilder withoutArmor() {
            this.armorEnabled = false;
            return this;
        }
        
        /**
         * Enable knockback with the specified profile
         */
        public ConfigurationBuilder withKnockback(KnockbackConfig config) {
            this.knockbackConfig = config;
            return this;
        }
        
        /**
         * Enable knockback with profile and sync settings
         */
        public ConfigurationBuilder withKnockback(KnockbackConfig config, boolean syncEnabled) {
            this.knockbackConfig = config;
            this.knockbackSyncEnabled = syncEnabled;
            return this;
        }

        /**
         * Set tick-scaling mode. SCALED (default) keeps real-time durations consistent across TPS;
         * UNSCALED uses literal tick counts. Call before {@link #initialize()}.
         */
        public ConfigurationBuilder withTickScaling(TickScalingMode mode) {
            this.tickScalingMode = mode;
            return this;
        }

        /**
         * Initialize the configured systems
         */
        public MechanicsManager initialize() {
            MechanicsManager manager = MechanicsManager.this;

            TickScalingConfig.initialize(tickScalingMode);

            log.info("Initializing...");
            
            int systemCount = 0;

            // Initialize gameplay if configured
            if (gameplayConfig != null) {
                log.info("Initializing Gameplay System...");
                manager.gameplayManager = GameplayManager.getInstance().initialize(gameplayConfig);
                manager.gameplayEnabled = true;
                systemCount++;
            }

            // Initialize health system if configured
            if (damageConfig != null) {
                log.info("Initializing Health System...");
                // Convert DamageConfig to HealthConfig
                HealthConfig healthConfig = new HealthConfig(
                        damageConfig.getInvulnerabilityTicks(),
                        false,  // logDamage: no built-in replacement logging; use HealthSystem.wasLastDamageReplacement() to detect
                        false, 0.0f, 0  // Regeneration (disabled)
                );
                manager.healthSystem = HealthSystem.initialize(healthConfig);
                manager.damageEnabled = true;
                systemCount++;
                // PlayerDeathHandler: visibility on death, IS_DEAD tag, and disables death messages
                // (incomplete messages like "X was slain by" crash 1.7 clients)
                PlayerDeathHandler.initialize();
            }

            // Initialize hitbox if configured
            if (hitDetectionConfig != null) {
                log.info("Initializing Hitbox System...");
                manager.hitboxManager = HitboxManager.getInstance().initialize(hitDetectionConfig);
                manager.hitboxEnabled = true;
                systemCount++;
            }
            
            // Initialize armor if enabled
            if (armorEnabled) {
                log.info("Initializing Armor System...");
                manager.armorManager = ArmorManager.getInstance().initialize(true);
                manager.armorEnabled = true;
                systemCount++;
            }

            // Initialize knockback if configured
            KnockbackConfig effectiveKnockback = knockbackConfig;
            if (effectiveKnockback == null && combatConfig != null) {
                effectiveKnockback = combatConfig.knockbackConfig();
            }
            if (effectiveKnockback != null && combatConfig != null) {
                effectiveKnockback = effectiveKnockback.withSprintBufferTicks(combatConfig.sprintWindowTicks());
            }
            if (effectiveKnockback != null) {
                log.info("Initializing Knockback System...");
                VelocityEstimator.initialize();
                manager.knockbackSystem = KnockbackSystem.initialize(effectiveKnockback);
                // Removed: setKnockbackSyncEnabled() - sync is disabled
                manager.knockbackEnabled = true;
                systemCount++;
            }

            // Initialize combat if configured
            if (combatConfig != null) {
                log.info("Initializing Combat System...");
                manager.combatManager = CombatManager.getInstance().initialize(combatConfig);
                manager.combatEnabled = true;
                systemCount++;
            }
            
            manager.initialized = true;
            
            log.info("All systems initialized ({} systems ready)", systemCount);
            
            return manager;
        }
    }
}

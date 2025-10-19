package com.minestom.mechanics;

import com.minestom.mechanics.config.MechanicsPresets;
import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.config.gameplay.DamageConfig;
import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.config.gameplay.GameplayConfig;
import com.minestom.mechanics.config.gameplay.GameplayPresets;
import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.damage.DamageFeature;
import com.minestom.mechanics.features.gameplay.EyeHeightSystem;
import com.minestom.mechanics.features.gameplay.HitboxSystem;
import com.minestom.mechanics.features.gameplay.MovementRestrictionSystem;
import com.minestom.mechanics.features.gameplay.PlayerCollisionSystem;
import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.manager.CombatManager;
import com.minestom.mechanics.manager.GameplayManager;
import com.minestom.mechanics.manager.HitboxManager;
import com.minestom.mechanics.manager.ArmorManager;
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
    private DamageFeature damageFeature;
    private HitboxManager hitboxManager;
    private ArmorManager armorManager;
    private KnockbackHandler knockbackHandler;

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
        if (damageEnabled && damageFeature != null) {
            // DamageFeature cleanup if needed
        }
        if (hitboxEnabled && hitboxManager != null) {
            hitboxManager.cleanupPlayer(player);
        }
        if (armorEnabled && armorManager != null) {
            armorManager.cleanupPlayer(player);
        }
        if (knockbackEnabled && knockbackHandler != null) {
            knockbackHandler.cleanup(player);
        }
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

        if (damageEnabled && damageFeature != null) {
            try {
                damageFeature.shutdown();
                log.info("Damage system shut down");
            } catch (Exception e) {
                log.error("Damage shutdown failed", e);
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

        if (knockbackEnabled && knockbackHandler != null) {
            try {
                knockbackHandler.shutdown();
                log.info("Knockback system shut down");
            } catch (Exception e) {
                log.error("Knockback shutdown failed", e);
            }
        }

        // Reset references
        combatManager = null;
        gameplayManager = null;
        damageFeature = null;
        hitboxManager = null;
        armorManager = null;
        knockbackHandler = null;

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

    public DamageFeature getDamageFeature() {
        if (!damageEnabled) {
            throw new IllegalStateException("Damage system not enabled!");
        }
        return damageFeature;
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

    public KnockbackHandler getKnockbackHandler() {
        if (!knockbackEnabled) {
            throw new IllegalStateException("Knockback system not enabled!");
        }
        return knockbackHandler;
    }

    /**
     * @deprecated Use {@link #getKnockbackHandler()} instead. KnockbackManager is deprecated.
     */
    @Deprecated
    public KnockbackHandler getKnockbackManager() {
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
         * Initialize the configured systems
         */
        public MechanicsManager initialize() {
            MechanicsManager manager = MechanicsManager.this;
            
            log.info("Initializing...");
            
            int systemCount = 0;

            // Initialize gameplay if configured
            if (gameplayConfig != null) {
                log.info("Initializing Gameplay System...");
                manager.gameplayManager = GameplayManager.getInstance().initialize(gameplayConfig);
                manager.gameplayEnabled = true;
                systemCount++;
            }

            // Initialize damage if configured
            if (damageConfig != null) {
                log.info("Initializing Damage System...");
                manager.damageFeature = DamageFeature.initialize(damageConfig);
                manager.damageEnabled = true;
                systemCount++;
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
            if (knockbackConfig != null) {
                log.info("Initializing Knockback System...");
                manager.knockbackHandler = KnockbackHandler.initialize(knockbackConfig);
                manager.knockbackHandler.setKnockbackSyncEnabled(knockbackSyncEnabled);
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

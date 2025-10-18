package com.minestom.mechanics.manager;

import com.minestom.mechanics.projectile.features.BowFeature;
import com.minestom.mechanics.projectile.features.FishingRodFeature;
import com.minestom.mechanics.projectile.features.MiscProjectileFeature;
import com.minestom.mechanics.projectile.config.FishingRodConfig;
import com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig;
import com.minestom.mechanics.projectile.config.FishingRodKnockbackConfig;
import com.minestom.mechanics.events.ProjectileCleanupHandler;
import com.minestom.mechanics.config.combat.ProjectileConfig;

// TODO: Simplify, unnecessarily long...

/**
 * Projectile Manager - Initializes all projectile systems
 * ✅ REFACTORED: Now extends AbstractManager for consistency
 *
 * Handles:
 * - Bows &amp; Arrows (with focused components)
 * - Fishing Rods (with focused components)
 * - Snowballs, Eggs, Ender Pearls (with focused components)
 *
 * Usage:
 * <pre>
 * // In Main.java, after CombatManager.initialize()
 * ProjectileManager.getInstance().initialize();
 * </pre>
 */
public class ProjectileManager extends AbstractManager<ProjectileManager> {
    private static ProjectileManager instance;

    // Refactored projectile features with focused components
    private BowFeature bowFeature;
    private FishingRodFeature fishingRodFeature;
    private MiscProjectileFeature miscProjectileFeature;
    
    // Configuration
    private ProjectileConfig projectileConfig;

    private ProjectileManager() {
        super("ProjectileManager");
        // Initialize with default config
        this.projectileConfig = ProjectileConfig.builder().build();
    }

    public static ProjectileManager getInstance() {
        if (instance == null) {
            instance = new ProjectileManager();
        }
        return instance;
    }

    /**
     * Initialize all projectile systems with a specific configuration.
     * Call this once at server startup, after CombatManager.
     */
    public ProjectileManager initialize(ProjectileConfig config) {
        this.projectileConfig = config;
        
        return initializeWithWrapper(() -> {
            // Initialize bow feature
            log.debug("Initializing BowFeature...");
            bowFeature = BowFeature.initialize();

            // Initialize fishing rod feature
            log.debug("Initializing FishingRodFeature...");
            fishingRodFeature = FishingRodFeature.initialize();

            // Initialize misc projectile feature
            log.debug("Initializing MiscProjectileFeature...");
            miscProjectileFeature = MiscProjectileFeature.initialize();

            // Register centralized cleanup handlers
            log.debug("Registering cleanup handlers...");
            ProjectileCleanupHandler.registerCleanupListeners();
            
            // Set configurations on initialized features
            if (fishingRodFeature != null) {
                fishingRodFeature.setConfig(config.getFishingRodConfig());
            }
        });
    }

    /**
     * Initialize all projectile systems with default configuration.
     * Call this once at server startup, after CombatManager.
     */
    public ProjectileManager initialize() {
        return initialize(ProjectileConfig.builder().build());
    }

    // ===========================
    // ABSTRACT METHOD IMPLEMENTATIONS
    // ===========================

    @Override
    protected String getSystemName() {
        return "ProjectileManager";
    }

    @Override
    protected void logMinimalConfig() {
        log.debug("Bows & Arrows: ✓ (Refactored with focused components)");
        log.debug("Fishing Rods: ✓ (Refactored with focused components)");
        log.debug("Snowballs: ✓ (Refactored with focused components)");
        log.debug("Eggs: ✓ (Refactored with focused components)");
        log.debug("Ender Pearls: ✓ (Refactored with focused components)");
        log.debug("Version: 1.8 (Legacy)");
        log.debug("Architecture: Component-based (Phase 5 Complete)");
    }

    @Override
    protected void cleanup() {
        bowFeature = null;
        fishingRodFeature = null;
        miscProjectileFeature = null;
    }

    @Override
    public String getStatus() {
        if (!initialized) {
            return "Projectile Systems: NOT INITIALIZED";
        }

        StringBuilder status = new StringBuilder();
        status.append("Projectile Systems Status:\n");
        status.append("  BowFeature: ").append(bowFeature != null ? "✓" : "✗").append("\n");
        status.append("  FishingRodFeature: ").append(fishingRodFeature != null ? "✓" : "✗").append("\n");
        status.append("  MiscProjectileFeature: ").append(miscProjectileFeature != null ? "✓" : "✗").append("\n");

        return status.toString();
    }

    /**
     * Shutdown all systems (for server stop or reinitialize)
     */
    public void shutdown() {
        shutdownWithWrapper(() -> {
            // Reset references
            bowFeature = null;
            fishingRodFeature = null;
            miscProjectileFeature = null;
        });
    }

    // ===========================
    // SYSTEM ACCESS
    // ===========================

    public BowFeature getBowFeature() {
        requireInitialized();
        return bowFeature;
    }

    public FishingRodFeature getFishingRodFeature() {
        requireInitialized();
        return fishingRodFeature;
    }

    public MiscProjectileFeature getMiscProjectileFeature() {
        requireInitialized();
        return miscProjectileFeature;
    }
    
    // ===========================
    // CONFIGURATION GETTERS/SETTERS
    // ===========================
    
    public ProjectileConfig getProjectileConfig() {
        return projectileConfig;
    }
    
    public void setProjectileConfig(ProjectileConfig config) {
        this.projectileConfig = config;
        // Update individual features if already initialized
        if (fishingRodFeature != null) {
            fishingRodFeature.setConfig(config.getFishingRodConfig());
        }
    }
    
    // Convenience getters for individual configs
    public FishingRodConfig getFishingRodConfig() {
        return projectileConfig.getFishingRodConfig();
    }
    
    public ProjectileKnockbackConfig getArrowKnockbackConfig() {
        return projectileConfig.getArrowKnockbackConfig();
    }
    
    public ProjectileKnockbackConfig getSnowballKnockbackConfig() {
        return projectileConfig.getSnowballKnockbackConfig();
    }
    
    public ProjectileKnockbackConfig getEggKnockbackConfig() {
        return projectileConfig.getEggKnockbackConfig();
    }
    
    public ProjectileKnockbackConfig getEnderPearlKnockbackConfig() {
        return projectileConfig.getEnderPearlKnockbackConfig();
    }
    
    public FishingRodKnockbackConfig getFishingRodKnockbackConfig() {
        return projectileConfig.getFishingRodKnockbackConfig();
    }
}

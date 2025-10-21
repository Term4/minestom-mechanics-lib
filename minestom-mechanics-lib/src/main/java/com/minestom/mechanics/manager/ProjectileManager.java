package com.minestom.mechanics.manager;

import com.minestom.mechanics.projectile.ProjectileRegistry;
import com.minestom.mechanics.projectile.ProjectileVelocitySystem;
import com.minestom.mechanics.systems.knockback.KnockbackApplicator;
import com.minestom.mechanics.projectile.features.BowFeature;
import com.minestom.mechanics.projectile.features.FishingRodFeature;
import com.minestom.mechanics.projectile.features.MiscProjectileFeature;
import com.minestom.mechanics.events.ProjectileCleanupHandler;
import com.minestom.mechanics.config.projectiles.ProjectileConfig;

/**
 * Projectile Manager - Initializes all projectile systems
 */
public class ProjectileManager extends AbstractManager<ProjectileManager> {
    private static ProjectileManager instance;

    // Refactored projectile features with focused components
    private BowFeature bowFeature;
    private FishingRodFeature fishingRodFeature;
    private MiscProjectileFeature miscProjectileFeature;

    // Configuration
    private ProjectileConfig projectileConfig;
    private KnockbackApplicator knockbackApplicator;
    private ProjectileRegistry projectileRegistry;

    private ProjectileManager() {
        super("ProjectileManager");
        this.projectileConfig = ProjectileConfig.defaultConfig();
    }

    public static ProjectileManager getInstance() {
        if (instance == null) {
            instance = new ProjectileManager();
        }
        return instance;
    }

    public ProjectileManager initialize(ProjectileConfig config) {
        this.projectileConfig = config;

        return initializeWithWrapper(() -> {
            // Initialize knockback applicator
            log.debug("Initializing KnockbackApplicator for projectiles...");
            var combatConfig = CombatManager.getInstance().getCombatConfig();
            this.knockbackApplicator = new KnockbackApplicator(combatConfig.knockbackConfig());

            // Initialize unified projectile registry
            log.debug("Initializing ProjectileRegistry...");
            this.projectileRegistry = new ProjectileRegistry();
            this.projectileRegistry.initialize(config);

            // Initialize bow feature
            log.debug("Initializing BowFeature...");
            bowFeature = BowFeature.initialize();

            // Initialize fishing rod feature
            log.debug("Initializing FishingRodFeature...");
            fishingRodFeature = FishingRodFeature.initialize();

            // Initialize misc projectile feature
            log.debug("Initializing MiscProjectileFeature...");
            miscProjectileFeature = MiscProjectileFeature.initialize();

            log.debug("Initializing ProjectileVelocitySystem...");
            ProjectileVelocitySystem.initialize(config.snowballVelocity());

            // Register centralized cleanup handlers
            log.debug("Registering cleanup handlers...");
            ProjectileCleanupHandler.registerCleanupListeners();

            // Configure features
            if (fishingRodFeature != null) {
                fishingRodFeature.setConfig(config.getFishingRodVelocityConfig());
            }
        });
    }

    public ProjectileManager initialize() {
        return initialize(ProjectileConfig.defaultConfig());
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
        knockbackApplicator = null;
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
        status.append("  KnockbackApplicator: ").append(knockbackApplicator != null ? "✓" : "✗").append("\n");

        return status.toString();
    }

    public void shutdown() {
        shutdownWithWrapper(() -> {
            bowFeature = null;
            fishingRodFeature = null;
            miscProjectileFeature = null;
            knockbackApplicator = null;
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

    /**
     * Get the unified projectile registry.
     * Contains all projectile data: knockback, velocity, material mappings, etc.
     */
    public ProjectileRegistry getProjectileRegistry() {
        requireInitialized();
        return projectileRegistry;
    }

    /**
     * Get the shared knockback applicator for all projectiles.
     * Projectile entities should use this to apply knockback.
     */
    public KnockbackApplicator getKnockbackApplicator() {
        requireInitialized();
        return knockbackApplicator;
    }

    // ===========================
    // CONFIGURATION
    // ===========================

    /**
     * Get the base projectile configuration.
     * For runtime modifications, use the ProjectileRegistry.
     */
    public ProjectileConfig getProjectileConfig() {
        requireInitialized();
        return projectileConfig;
    }
}
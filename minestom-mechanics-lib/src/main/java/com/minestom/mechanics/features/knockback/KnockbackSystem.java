package com.minestom.mechanics.features.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.features.knockback.components.KnockbackStrength;
import com.minestom.mechanics.features.knockback.components.KnockbackApplicator;
import com.minestom.mechanics.util.ConfigurableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.ProjectileTagRegistry;
import net.minestom.server.entity.*;
import net.minestom.server.tag.Tag;

import java.util.List;

/**
 * Knockback configuration system.
 * Manages knockback config resolution through Universal Tag System.
 * For applying knockback, use {@link KnockbackApplicator}.
 *
 * Tag format: [horizontal, vertical, sprintH, sprintV, airH, airV]
 */
public class KnockbackSystem extends ConfigurableSystem<KnockbackConfig> {

    private static KnockbackSystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackSystem");

    private KnockbackSystem(KnockbackConfig config) {
        super(config);
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static KnockbackSystem initialize(KnockbackConfig config) {
        if (instance != null && instance.initialized) {
            LogUtil.logAlreadyInitialized("KnockbackSystem");
            return instance;
        }

        instance = new KnockbackSystem(config);
        instance.markInitialized();

        ProjectileTagRegistry.register(KnockbackSystem.class);
        LogUtil.logInit("KnockbackSystem");
        return instance;
    }

    public static KnockbackSystem getInstance() {
        if (instance == null || !instance.initialized) {
            throw new IllegalStateException("KnockbackSystem not initialized!");
        }
        return instance;
    }

    // ===========================
    // UNIVERSAL TAG SYSTEM
    // ===========================

    /**
     * Multiply knockback components: [horizontal, vertical, sprintH, sprintV, airH, airV]
     * Example for blocking: [0.4, 0.4, 0, 0, 1, 1] = 60% reduction
     */
    public static final Tag<List<Double>> MULTIPLIER = Tag.Double("knockback_multiplier").list();
    public static final Tag<List<Double>> MODIFY = Tag.Double("knockback_modify").list();
    public static final Tag<KnockbackConfig> CUSTOM = Tag.Transient("knockback_custom");

    public static final Tag<List<Double>> PROJECTILE_MULTIPLIER = Tag.Double("knockback_projectile_multiplier").list();
    public static final Tag<List<Double>> PROJECTILE_MODIFY = Tag.Double("knockback_projectile_modify").list();
    public static final Tag<KnockbackConfig> PROJECTILE_CUSTOM = Tag.Transient("knockback_projectile_custom");

    @Override protected Tag<List<Double>> getMultiplierTag() { return MULTIPLIER; }
    @Override protected Tag<List<Double>> getModifyTag() { return MODIFY; }
    @Override protected Tag<KnockbackConfig> getCustomTag() { return CUSTOM; }
    @Override protected Tag<List<Double>> getProjectileMultiplierTag() { return PROJECTILE_MULTIPLIER; }
    @Override protected Tag<List<Double>> getProjectileModifyTag() { return PROJECTILE_MODIFY; }
    @Override protected Tag<KnockbackConfig> getProjectileCustomTag() { return PROJECTILE_CUSTOM; }
    @Override protected int getModifyComponentCount() { return 6; }

    // ===========================
    // CONFIG RESOLUTION
    // ===========================

    /**
     * Resolve knockback config through tag system.
     * Public for use by KnockbackApplicator.
     */
    public KnockbackConfig resolveConfig(Entity attacker, LivingEntity victim, EquipmentSlot handUsed) {
        // Use the parent class resolution with component extraction
        double[] components = resolveComponents(attacker, victim, handUsed, config -> new double[] {
                config.horizontal(),
                config.vertical(),
                config.sprintBonusHorizontal(),
                config.sprintBonusVertical(),
                config.airMultiplierHorizontal(),
                config.airMultiplierVertical()
        });

        // Get base config for non-component properties
        KnockbackConfig baseConfig = getBaseConfigForProjectile(attacker);

        // Build config from resolved components
        return new KnockbackConfig(
                components[0], // horizontal
                components[1], // vertical
                baseConfig.verticalLimit(),
                components[2], // sprintBonusH
                components[3], // sprintBonusV
                components[4], // airMultH
                components[5], // airMultV
                baseConfig.lookWeight(),
                baseConfig.modern(),
                false // sync disabled
        );
    }

    @Override
    protected KnockbackConfig resolveBaseConfig(Entity attacker, LivingEntity victim, EquipmentSlot handUsed) {
        // If attacker is a projectile, use projectile-specific base config
        if (isProjectileAttacker(attacker)) {
            return getBaseConfigForProjectile(attacker);
        }

        // Otherwise use normal resolution (checks tags)
        return super.resolveBaseConfig(attacker, victim, handUsed);
    }

    /**
     * Get base config for projectiles from ProjectileManager.
     */
    private KnockbackConfig getBaseConfigForProjectile(Entity attacker) {
        EntityType type = attacker.getEntityType();

        // Check if it's a projectile type
        if (type != EntityType.ARROW &&
                type != EntityType.SPECTRAL_ARROW &&
                type != EntityType.SNOWBALL &&
                type != EntityType.EGG &&
                type != EntityType.ENDER_PEARL &&
                type != EntityType.FISHING_BOBBER) {
            return serverDefaultConfig;
        }

        try {
            var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
            var projectileConfig = projectileManager.getProjectileConfig();

            com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig pkbConfig = null;

            if (type == EntityType.ARROW || type == EntityType.SPECTRAL_ARROW) {
                pkbConfig = projectileConfig.getArrowKnockbackConfig();
            } else if (type == EntityType.SNOWBALL) {
                pkbConfig = projectileConfig.getSnowballKnockbackConfig();
            } else if (type == EntityType.EGG) {
                pkbConfig = projectileConfig.getEggKnockbackConfig();
            } else if (type == EntityType.ENDER_PEARL) {
                pkbConfig = projectileConfig.getEnderPearlKnockbackConfig();
            } else if (type == EntityType.FISHING_BOBBER) {
                pkbConfig = projectileConfig.getFishingRodKnockbackConfig();
            }

            if (pkbConfig != null && pkbConfig.enabled()) {
                return new KnockbackConfig(
                        pkbConfig.horizontalKnockback(),
                        pkbConfig.verticalKnockback(),
                        pkbConfig.verticalLimit(),
                        0.0, 0.0, 1.0, 1.0, 0.0, false, false
                );
            }
        } catch (Exception e) {
            log.debug("Could not get projectile config, using default");
        }

        return serverDefaultConfig;
    }

    // ===========================
    // API
    // ===========================

    public KnockbackConfig getConfig() {
        requireInitialized();
        return serverDefaultConfig;
    }

    public void shutdown() {
        log.debug("KnockbackSystem shutdown complete");
    }
}
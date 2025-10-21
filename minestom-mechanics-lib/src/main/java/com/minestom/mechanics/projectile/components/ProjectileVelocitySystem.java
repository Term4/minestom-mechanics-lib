package com.minestom.mechanics.projectile.components;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.projectile.tags.VelocityTagSerializer;
import com.minestom.mechanics.projectile.tags.VelocityTagValue;
import com.minestom.mechanics.util.ConfigTagWrapper;
import com.minestom.mechanics.util.ConfigurableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.ProjectileTagRegistry;
import net.minestom.server.entity.*;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Projectile velocity configuration system using unified tag approach.
 *
 * Tag Priority for Projectiles:
 * 1. Item tags (bow, snowball item, etc.) [HIGHEST]
 * 2. Projectile entity tags
 * 3. Shooter (player) tags
 * 4. World tags
 * 5. Registry/server default [LOWEST]
 *
 * Usage:
 * <pre>
 * import static VelocityTagValue.*;
 *
 * item.withTag(ProjectileVelocitySystem.CUSTOM, velMult(2.0))
 * item.withTag(ProjectileVelocitySystem.CUSTOM, VEL_LASER)
 * </pre>
 */
public class ProjectileVelocitySystem extends ConfigurableSystem<ProjectileVelocityConfig> {

    private static ProjectileVelocitySystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("ProjectileVelocitySystem");

    private ProjectileVelocitySystem(ProjectileVelocityConfig config) {
        super(config);
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static ProjectileVelocitySystem initialize(ProjectileVelocityConfig config) {
        if (instance != null && instance.initialized) {
            LogUtil.logAlreadyInitialized("ProjectileVelocitySystem");
            return instance;
        }

        instance = new ProjectileVelocitySystem(config);
        instance.markInitialized();

        ProjectileTagRegistry.register(ProjectileVelocitySystem.class);
        LogUtil.logInit("ProjectileVelocitySystem");
        return instance;
    }

    public static ProjectileVelocitySystem getInstance() {
        if (instance == null || !instance.initialized) {
            throw new IllegalStateException("ProjectileVelocitySystem not initialized!");
        }
        return instance;
    }

    // ===========================
    // UNIFIED TAG SYSTEM
    // ===========================

    public static final Tag<VelocityTagValue> CUSTOM =
            Tag.Structure("projectile_velocity_custom", new VelocityTagSerializer());

    @Override
    @SuppressWarnings("unchecked")
    protected Tag<ConfigTagWrapper<ProjectileVelocityConfig>> getWrapperTag(Entity attacker) {
        return (Tag<ConfigTagWrapper<ProjectileVelocityConfig>>) (Tag<?>) CUSTOM;
    }

    @Override
    protected int getComponentCount() {
        return 6; // [hMult, vMult, spread, gravity, hAirRes, vAirRes]
    }

    // ===========================
    // OVERRIDE: Use registry for projectile base config
    // ===========================

    @Override
    protected ProjectileVelocityConfig resolveBaseConfig(
            Entity attacker,
            @Nullable LivingEntity victim,
            @Nullable ItemStack item) {  // ← Fixed signature to match parent!

        // Projectiles get velocity from registry
        if (isProjectileAttacker(attacker)) {
            try {
                var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
                ProjectileVelocityConfig registryConfig = projectileManager.getProjectileRegistry()
                        .getVelocityConfig(attacker.getEntityType());

                if (registryConfig != null) {
                    log.debug("Using registry config for {}", attacker.getEntityType());
                    return registryConfig;
                }
            } catch (Exception e) {
                log.debug("Could not get registry config: {}", e.getMessage());
            }
        }

        // Fallback to server default
        return super.resolveBaseConfig(attacker, victim, item);  // ← Pass item, not handUsed!
    }

    // ===========================
    // PUBLIC API
    // ===========================

    /**
     * Resolve velocity config for projectiles.
     * Uses base class method with item parameter for proper priority.
     *
     * Priority: Item > Projectile > Shooter (if player) > World > Registry
     */
    public ProjectileVelocityConfig resolveConfig(Entity shooter, Entity projectile, ItemStack item) {
        // Use base class method that handles item priority!
        double[] components = resolveComponents(
                projectile,  // Pass projectile as "attacker" for registry lookup
                null,        // No victim for velocity resolution
                item,        // Item gets checked first!
                config -> new double[]{
                        config.horizontalMultiplier(),
                        config.verticalMultiplier(),
                        config.spreadMultiplier(),
                        config.gravity(),
                        config.horizontalAirResistance(),
                        config.verticalAirResistance()
                }
        );

        return new ProjectileVelocityConfig(
                components[0], components[1], components[2],
                components[3], components[4], components[5]
        );
    }
}
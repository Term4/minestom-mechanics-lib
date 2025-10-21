package com.minestom.mechanics.projectile;

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
 * Usage:
 * <pre>
 * import static VelocityTagValue.*;
 *
 * // Simple
 * item.withTag(ProjectileVelocitySystem.CUSTOM, velMult(2.0))
 *
 * // Combined
 * item.withTag(ProjectileVelocitySystem.CUSTOM, velMult(0.5).thenAdd(0, 0, 0, 0.01, 0, 0))
 *
 * // Presets
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

    /** Unified velocity tag (projectiles only) - USING CUSTOM SERIALIZER */
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
    protected ProjectileVelocityConfig resolveBaseConfig(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed) {
        // Projectiles get velocity from registry
        if (isProjectileAttacker(attacker)) {
            try {
                var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
                ProjectileVelocityConfig registryConfig = projectileManager.getProjectileRegistry()
                        .getVelocityConfig(attacker.getEntityType());

                if (registryConfig != null) {
                    return registryConfig;
                }
            } catch (Exception e) {
                log.debug("Could not get projectile velocity from registry: {}", e.getMessage());
            }
        }

        // Fallback to server default
        return super.resolveBaseConfig(attacker, victim, handUsed);
    }

    // ===========================
    // PUBLIC API
    // ===========================

    /**
     * Resolve velocity config through tag system.
     * Checks item, player, and projectile tags in priority order.
     */
    public ProjectileVelocityConfig resolveConfig(Entity shooter, Entity projectile, ItemStack item) {
        double[] components = resolveComponents(
                shooter,
                projectile instanceof LivingEntity le ? le : null,
                item != null ? EquipmentSlot.MAIN_HAND : null,
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
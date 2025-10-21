package com.minestom.mechanics.projectile;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.util.ConfigurableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.ProjectileTagRegistry;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;

import java.util.List;

/**
 * Projectile velocity configuration system using Universal Tag System.
 * Manages velocity config resolution through tags on items, players, and projectiles.
 *
 * Tag format: [horizontalMult, verticalMult, spreadMult, gravity, horizontalAirRes, verticalAirRes]
 *
 * Priority order (same as KnockbackSystem):
 * 1. Custom configs (item > player > projectile > server)
 * 2. Modifications (additive)
 * 3. Multipliers (multiplicative)
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
    // UNIVERSAL TAG SYSTEM
    // ===========================

    /**
     * Multiply velocity components: [horizontalMult, verticalMult, spreadMult, gravity, horizontalAirRes, verticalAirRes]
     * Example for slow-mo: [0.5, 0.5, 1.0, 0.5, 1.0, 1.0] = 50% speed and gravity
     */
    public static final Tag<List<Double>> MULTIPLIER = Tag.Double("projectile_velocity_multiplier").list();
    public static final Tag<List<Double>> MODIFY = Tag.Double("projectile_velocity_modify").list();
    public static final Tag<ProjectileVelocityConfig> CUSTOM = Tag.Transient("projectile_velocity_custom");

    @Override protected Tag<List<Double>> getMultiplierTag() { return MULTIPLIER; }
    @Override protected Tag<List<Double>> getModifyTag() { return MODIFY; }
    @Override protected Tag<ProjectileVelocityConfig> getCustomTag() { return CUSTOM; }
    @Override protected Tag<List<Double>> getProjectileMultiplierTag() { return MULTIPLIER; }
    @Override protected Tag<List<Double>> getProjectileModifyTag() { return MODIFY; }
    @Override protected Tag<ProjectileVelocityConfig> getProjectileCustomTag() { return CUSTOM; }
    @Override protected int getModifyComponentCount() { return 6; }

    // ===========================
    // CONFIG RESOLUTION
    // ===========================

    /**
     * Resolve velocity config through tag system.
     * Checks item, player, and projectile tags in priority order.
     *
     * @param shooter The entity shooting (for player tags)
     * @param projectile The projectile entity (can be null before spawn)
     * @param item The item being used (for item tags)
     * @return Resolved velocity config
     */
    public ProjectileVelocityConfig resolveConfig(Entity shooter, Entity projectile, ItemStack item) {
        // Use resolveComponents to get final values
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

    // ===========================
    // HELPER METHODS
    // ===========================

    /**
     * Create a multiplier for global speed changes (affects horizontal and vertical equally).
     */
    public static List<Double> createSpeedMultiplier(double speedMultiplier) {
        return List.of(speedMultiplier, speedMultiplier, 1.0, 1.0, 1.0, 1.0);
    }

    /**
     * Create a multiplier for gravity changes.
     */
    public static List<Double> createGravityMultiplier(double gravityMultiplier) {
        return List.of(1.0, 1.0, 1.0, gravityMultiplier, 1.0, 1.0);
    }

    /**
     * Create a multiplier for air resistance/drag changes.
     */
    public static List<Double> createAirResistanceMultiplier(double resistanceMultiplier) {
        return List.of(1.0, 1.0, 1.0, 1.0, resistanceMultiplier, resistanceMultiplier);
    }
}
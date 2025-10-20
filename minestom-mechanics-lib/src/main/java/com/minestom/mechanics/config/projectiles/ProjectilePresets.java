package com.minestom.mechanics.config.projectiles;

// TODO: Make minemen, hypixel, and scrims presets

import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackPresets;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityPresets;

/**
 * Preset configurations for projectiles with convenient tweaking helpers.
 *
 * Usage:
 * <pre>
 * // Use preset directly
 * ProjectileManager.initialize(ProjectilePresets.VANILLA);
 *
 * // Quick tweaks with static helpers
 * import static ProjectilePresets.*;
 * ProjectileConfig config = withArrowSpeed(VANILLA, 5.0);
 *
 * // Chain tweaks
 * ProjectileConfig config = withSnowballKnockback(
 *     withArrowSpeed(VANILLA, 5.0),
 *     0.8, 0.5
 * );
 * </pre>
 */
public class ProjectilePresets {

    // ===========================
    // BASE PRESETS
    // ===========================

    /** Vanilla 1.8.9 projectile behavior */
    public static final ProjectileConfig VANILLA18 = ProjectileConfig.defaultConfig()
            .withInheritPlayerMomentum(false);

    /** Fast-paced combat - faster projectiles, higher knockback */
    public static final ProjectileConfig FAST = ProjectileConfig.defaultConfig()
            .withArrowVelocity(ProjectileVelocityPresets.LASER_ARROW)
            .withSnowballVelocity(ProjectileVelocityPresets.FLOATY_SNOWBALL)
            .withArrowKnockback(ProjectileKnockbackPresets.HEAVY)
            .withSnowballKnockback(ProjectileKnockbackPresets.HEAVY);

    /** Floaty/magical - reduced gravity, softer knockback */
    public static final ProjectileConfig FLOATY = ProjectileConfig.defaultConfig()
            .withArrowVelocity(ProjectileVelocityPresets.ARROW.withGravity(0.02))
            .withSnowballVelocity(ProjectileVelocityPresets.FLOATY_SNOWBALL)
            .withArrowKnockback(ProjectileKnockbackPresets.LIGHT)
            .withSnowballKnockback(ProjectileKnockbackPresets.LIGHT);

    private ProjectilePresets() {
        // Prevent instantiation
    }
}
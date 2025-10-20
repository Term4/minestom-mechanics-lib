package com.minestom.mechanics.projectile.config;

import com.minestom.mechanics.config.projectiles.ProjectileConfig;
import com.minestom.mechanics.projectile.config.ProjectileKnockbackPresets;
import com.minestom.mechanics.projectile.config.ProjectileVelocityPresets;

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
    public static final ProjectileConfig VANILLA = ProjectileConfig.defaultConfig();

    /** Fast-paced combat - faster projectiles, higher knockback */
    public static final ProjectileConfig FAST_PACED = ProjectileConfig.defaultConfig()
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

    // ===========================
    // CONVENIENCE TWEAKERS
    // ===========================

    // ----- ARROW -----

    public static ProjectileConfig withArrowSpeed(ProjectileConfig base, double speed) {
        return base.withArrowVelocity(base.arrowVelocity().withMultiplier(speed));
    }

    public static ProjectileConfig withArrowKnockback(ProjectileConfig base, double horizontal, double vertical) {
        return base.withArrowKnockback(base.arrowKnockback().withKnockback(horizontal, vertical));
    }

    public static ProjectileConfig withArrowGravity(ProjectileConfig base, double gravity) {
        return base.withArrowVelocity(base.arrowVelocity().withGravity(gravity));
    }

    // ----- SNOWBALL -----

    public static ProjectileConfig withSnowballSpeed(ProjectileConfig base, double speed) {
        return base.withSnowballVelocity(base.snowballVelocity().withMultiplier(speed));
    }

    public static ProjectileConfig withSnowballKnockback(ProjectileConfig base, double horizontal, double vertical) {
        return base.withSnowballKnockback(base.snowballKnockback().withKnockback(horizontal, vertical));
    }

    public static ProjectileConfig withSnowballGravity(ProjectileConfig base, double gravity) {
        return base.withSnowballVelocity(base.snowballVelocity().withGravity(gravity));
    }

    // ----- EGG -----

    public static ProjectileConfig withEggSpeed(ProjectileConfig base, double speed) {
        return base.withEggVelocity(base.eggVelocity().withMultiplier(speed));
    }

    public static ProjectileConfig withEggKnockback(ProjectileConfig base, double horizontal, double vertical) {
        return base.withEggKnockback(base.eggKnockback().withKnockback(horizontal, vertical));
    }

    public static ProjectileConfig withEggGravity(ProjectileConfig base, double gravity) {
        return base.withEggVelocity(base.eggVelocity().withGravity(gravity));
    }

    // ----- ENDER PEARL -----

    public static ProjectileConfig withEnderPearlSpeed(ProjectileConfig base, double speed) {
        return base.withEnderPearlVelocity(base.enderPearlVelocity().withMultiplier(speed));
    }

    public static ProjectileConfig withEnderPearlKnockback(ProjectileConfig base, double horizontal, double vertical) {
        return base.withEnderPearlKnockback(base.enderPearlKnockback().withKnockback(horizontal, vertical));
    }

    public static ProjectileConfig withEnderPearlGravity(ProjectileConfig base, double gravity) {
        return base.withEnderPearlVelocity(base.enderPearlVelocity().withGravity(gravity));
    }

    // ----- FISHING ROD -----

    public static ProjectileConfig withFishingRodSpeed(ProjectileConfig base, double speed) {
        return base.withFishingRodVelocity(base.fishingRodVelocity().withMultiplier(speed));
    }

    public static ProjectileConfig withFishingRodKnockback(ProjectileConfig base, double horizontal, double vertical) {
        return base.withFishingRodKnockback(base.fishingRodKnockback().withKnockback(horizontal, vertical));
    }

    public static ProjectileConfig withFishingRodGravity(ProjectileConfig base, double gravity) {
        return base.withFishingRodVelocity(base.fishingRodVelocity().withGravity(gravity));
    }

    public static ProjectileConfig withFishingRodArc(ProjectileConfig base, double horizontal, double vertical) {
        return base.withFishingRodVelocity(base.fishingRodVelocity().withCustom(horizontal, vertical));
    }

    private ProjectilePresets() {
        // Prevent instantiation
    }
}
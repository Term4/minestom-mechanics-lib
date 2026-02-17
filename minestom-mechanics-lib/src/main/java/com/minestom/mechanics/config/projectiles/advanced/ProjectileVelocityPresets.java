package com.minestom.mechanics.config.projectiles.advanced;

/**
 * Preset configurations for projectile velocities.
 * Provides common defaults and specialized presets.
 */
public class ProjectileVelocityPresets {

    // ===========================
    // DEFAULT PRESETS
    // ===========================

    /** Default arrow velocity (3.0 speed, normal gravity) */
    public static final ProjectileVelocityConfig ARROW =
            new ProjectileVelocityConfig(3.0, 3.0, 0, 0.05, 0.99, 0.99);

    /** Default snowball velocity (1.5 speed, light gravity) */
    public static final ProjectileVelocityConfig SNOWBALL =
            new ProjectileVelocityConfig(1.0, 1.0, 1.0, 0.03, 0.99, 0.99);

    /** Default egg velocity (1.5 speed, light gravity) */
    public static final ProjectileVelocityConfig EGG =
            new ProjectileVelocityConfig(1.0, 1.0, 1.0, 0.03, 0.99, 0.99);

    /** Default ender pearl velocity (1.5 speed, light gravity) */
    public static final ProjectileVelocityConfig ENDER_PEARL =
            new ProjectileVelocityConfig(1.0, 1.0, 1.0, 0.03, 0.99, 0.99);

    /** Fishing rod bobber velocity */
    public static final ProjectileVelocityConfig FISHING_ROD =
            new ProjectileVelocityConfig(5, 3.5, 1.0, 0.04, 0.92, 0.92);

    // ===========================
    // SPECIALIZED PRESETS
    // ===========================

    /** Laser arrow - fast, flat trajectory, very accurate */
    public static final ProjectileVelocityConfig LASER_ARROW =
            new ProjectileVelocityConfig(5.0, 1.5, 0.3, 0.02, 1, 1);

    /** Mortar arrow - high arc trajectory */
    public static final ProjectileVelocityConfig MORTAR_ARROW =
            new ProjectileVelocityConfig(2.0, 6.0, 1.0, 0.06, 0.99, 0.99);

    /** Sniper arrow - very long range, accurate */
    public static final ProjectileVelocityConfig SNIPER_ARROW =
            new ProjectileVelocityConfig(6.0, 3.0, 0.2, 0.04, 0.99, 0.99);

    /** Floaty snowball - slow, magical feel */
    public static final ProjectileVelocityConfig FLOATY_SNOWBALL =
            new ProjectileVelocityConfig(1.0, 1.2, 1.5, 0.01, 0.95, 0.95);

    /** Lob throw - high arc for area denial */
    public static final ProjectileVelocityConfig LOB_THROW =
            new ProjectileVelocityConfig(1.0, 3.0, 1.5, 0.04, 0.99, 0.99);

    /** Fast pitch - baseball throw style */
    public static final ProjectileVelocityConfig strongRod =
            new ProjectileVelocityConfig(2.5, 2.5, 0.8, 0.05, 0.95, 0.95);

    /** Heavy projectile - drops quickly */
    public static final ProjectileVelocityConfig normalCast =
            new ProjectileVelocityConfig(1.5, 1.5, 1.0, 0.10, 0.99, 0.99);

    private ProjectileVelocityPresets() {
        // Prevent instantiation
    }
}
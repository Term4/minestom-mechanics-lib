package com.minestom.mechanics.projectile.config;

/**
 * Preset configurations for projectile knockback.
 * Provides common defaults and specialized presets.
 */
public class ProjectileKnockbackPresets {

    // ===========================
    // DEFAULT PRESETS
    // ===========================

    /** Default arrow knockback (vanilla 1.8.9 values) */
    public static final ProjectileKnockbackConfig ARROW =
            new ProjectileKnockbackConfig(true, 0.525, 0.365, 0.45);

    /** Default snowball knockback */
    public static final ProjectileKnockbackConfig SNOWBALL =
            new ProjectileKnockbackConfig(true, 0.525, 0.365, 0.45);

    /** Default egg knockback */
    public static final ProjectileKnockbackConfig EGG =
            new ProjectileKnockbackConfig(true, 0.525, 0.365, 0.45);

    /** Default ender pearl knockback (disabled) */
    public static final ProjectileKnockbackConfig ENDER_PEARL =
            new ProjectileKnockbackConfig(false, 0.0, 0.0, 0.0);

    /** Default fishing rod knockback */
    public static final ProjectileKnockbackConfig FISHING_ROD =
            new ProjectileKnockbackConfig(true, 0.525, 0.365, 0.45);

    // ===========================
    // SPECIALIZED PRESETS
    // ===========================

    /** No knockback */
    public static final ProjectileKnockbackConfig DISABLED =
            new ProjectileKnockbackConfig(false, 0.0, 0.0, 0.0);

    /** Heavy knockback - sends targets flying */
    public static final ProjectileKnockbackConfig HEAVY =
            new ProjectileKnockbackConfig(true, 1.0, 0.6, 0.7);

    /** Light knockback - gentle push */
    public static final ProjectileKnockbackConfig LIGHT =
            new ProjectileKnockbackConfig(true, 0.3, 0.2, 0.3);

    /** Launcher - high vertical knockback */
    public static final ProjectileKnockbackConfig LAUNCHER =
            new ProjectileKnockbackConfig(true, 0.4, 1.0, 1.2);

    /** Pusher - high horizontal, low vertical */
    public static final ProjectileKnockbackConfig PUSHER =
            new ProjectileKnockbackConfig(true, 0.8, 0.1, 0.2);

    private ProjectileKnockbackPresets() {
        // Prevent instantiation
    }
}
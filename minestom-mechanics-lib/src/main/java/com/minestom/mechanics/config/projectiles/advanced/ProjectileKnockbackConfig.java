package com.minestom.mechanics.config.projectiles.advanced;

/**
 * Immutable configuration for projectile knockback settings.
 * Follows the same pattern as DamageConfig.
 *
 * Usage:
 * <pre>
 * ProjectileKnockbackConfig config = ProjectileKnockbackPresets.ARROW
 *     .withHorizontalKnockback(0.6)
 *     .withEnabled(false);
 * </pre>
 */
public record ProjectileKnockbackConfig(
        boolean enabled,
        double horizontalKnockback,
        double verticalKnockback,
        double verticalLimit
) {

    // Validation
    public ProjectileKnockbackConfig {
        if (horizontalKnockback < 0) throw new IllegalArgumentException("Horizontal knockback must be >= 0");
        if (verticalKnockback < 0) throw new IllegalArgumentException("Vertical knockback must be >= 0");
        if (verticalLimit < 0) throw new IllegalArgumentException("Vertical limit must be >= 0");
    }

    // TODO: Add air multipliers?
    // ===== IMMUTABLE "WITH" METHODS =====

    public ProjectileKnockbackConfig withEnabled(boolean enabled) {
        return new ProjectileKnockbackConfig(enabled, horizontalKnockback, verticalKnockback, verticalLimit);
    }

    public ProjectileKnockbackConfig withHorizontalKnockback(double knockback) {
        return new ProjectileKnockbackConfig(enabled, knockback, verticalKnockback, verticalLimit);
    }

    public ProjectileKnockbackConfig withVerticalKnockback(double knockback) {
        return new ProjectileKnockbackConfig(enabled, horizontalKnockback, knockback, verticalLimit);
    }

    public ProjectileKnockbackConfig withVerticalLimit(double limit) {
        return new ProjectileKnockbackConfig(enabled, horizontalKnockback, verticalKnockback, limit);
    }

    public ProjectileKnockbackConfig withKnockback(double horizontal, double vertical) {
        return new ProjectileKnockbackConfig(enabled, horizontal, vertical, verticalLimit);
    }

    public ProjectileKnockbackConfig withKnockback(double horizontal, double vertical, double limit) {
        return new ProjectileKnockbackConfig(enabled, horizontal, vertical, limit);
    }
}
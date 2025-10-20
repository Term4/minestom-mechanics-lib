package com.minestom.mechanics.projectile.config;

/**
 * Immutable configuration for projectile velocity/flight behavior.
 * Provides granular control over projectile trajectory.
 *
 * Usage:
 * <pre>
 * ProjectileVelocityConfig config = ProjectileVelocityPresets.ARROW
 *     .withHorizontalMultiplier(5.0)
 *     .withGravity(0.02);
 * </pre>
 */
public record ProjectileVelocityConfig(
        double horizontalMultiplier,  // X/Z axis (distance)
        double verticalMultiplier,    // Y axis (height/arc)
        double spreadMultiplier,      // Inaccuracy
        double gravity                // Drop rate
) {

    // Validation
    public ProjectileVelocityConfig {
        if (horizontalMultiplier < 0) throw new IllegalArgumentException("Horizontal multiplier must be >= 0");
        if (verticalMultiplier < 0) throw new IllegalArgumentException("Vertical multiplier must be >= 0");
        if (spreadMultiplier < 0) throw new IllegalArgumentException("Spread multiplier must be >= 0");
    }

    // ===== IMMUTABLE "WITH" METHODS =====

    /**
     * Set both horizontal and vertical multipliers uniformly (convenience method).
     */
    public ProjectileVelocityConfig withMultiplier(double multiplier) {
        return new ProjectileVelocityConfig(multiplier, multiplier, spreadMultiplier, gravity);
    }

    public ProjectileVelocityConfig withHorizontalMultiplier(double multiplier) {
        return new ProjectileVelocityConfig(multiplier, verticalMultiplier, spreadMultiplier, gravity);
    }

    public ProjectileVelocityConfig withVerticalMultiplier(double multiplier) {
        return new ProjectileVelocityConfig(horizontalMultiplier, multiplier, spreadMultiplier, gravity);
    }

    public ProjectileVelocityConfig withSpreadMultiplier(double multiplier) {
        return new ProjectileVelocityConfig(horizontalMultiplier, verticalMultiplier, multiplier, gravity);
    }

    public ProjectileVelocityConfig withGravity(double gravity) {
        return new ProjectileVelocityConfig(horizontalMultiplier, verticalMultiplier, spreadMultiplier, gravity);
    }

    /**
     * Set custom horizontal and vertical multipliers independently.
     */
    public ProjectileVelocityConfig withCustom(double horizontal, double vertical) {
        return new ProjectileVelocityConfig(horizontal, vertical, spreadMultiplier, gravity);
    }
}
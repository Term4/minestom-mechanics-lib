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
        double gravity,               // Drop rate
        double horizontalAirResistance, // Horizontal Drag (how much it slows down in the air)
        double verticalAirResistance    // Vertical Drag
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
        return new ProjectileVelocityConfig(multiplier, multiplier, spreadMultiplier, gravity, horizontalAirResistance, verticalAirResistance);
    }

    public ProjectileVelocityConfig withMultipliers(double horizontal, double vertical) {
        return new ProjectileVelocityConfig(horizontal, vertical, spreadMultiplier, gravity, horizontalAirResistance, verticalAirResistance);
    }

    public ProjectileVelocityConfig withSpread(double multiplier) {
        return new ProjectileVelocityConfig(horizontalMultiplier, verticalMultiplier, multiplier, gravity, horizontalAirResistance, verticalAirResistance);
    }

    public ProjectileVelocityConfig withGravity(double gravity) {
        return new ProjectileVelocityConfig(horizontalMultiplier, verticalMultiplier, spreadMultiplier, gravity, horizontalAirResistance, verticalAirResistance);
    }

    public ProjectileVelocityConfig withDrag(double horizontalDrag, double verticalDrag) {
        return new ProjectileVelocityConfig(horizontalMultiplier, verticalMultiplier, spreadMultiplier, gravity, horizontalDrag, verticalDrag);
    }

    /**
     * Set custom horizontal and vertical multipliers independently.
     */
    public ProjectileVelocityConfig withCustom(double horizontal, double vertical, double spread, double gravity, double horizontalDrag, double verticalDrag) {
        return new ProjectileVelocityConfig(horizontal, vertical, spread, gravity, horizontalDrag, verticalDrag);
    }
}
package com.minestom.mechanics.config.projectiles.advanced;


/**
 * Configuration for projectile velocity physics.
 *
 * <h2>⚠️ High Velocity Visual Desync (Legacy 1.8 Clients Only)</h2>
 *
 * <p><b>Modern clients (1.21+):</b> Handle high velocities correctly, no issues.</p>
 *
 * <p><b>Legacy 1.8 clients via ViaVersion:</b> Velocities exceeding ~2.5 blocks/tick per axis
 * may cause visual desync due to velocity packet translation differences. The projectile may
 * appear to fly backwards or "bounce" on the client, but server-side physics remain accurate.
 * Hit detection, collision, and trajectory are unaffected.</p>
 *
 * <h3>Observed Behavior (1.8 Clients)</h3>
 * <ul>
 *   <li><b>0.0 - 2.5 blocks/tick:</b> No issues</li>
 *   <li><b>2.5 - 4.0 blocks/tick:</b> Usually visual desyncs</li>
 *   <li><b>4.0+ blocks/tick:</b> Projectile appears backwards, then corrects</li>
 * </ul>
 *
 * <p><b>Note:</b> Server physics are always correct. Players will still hit their targets
 * accurately regardless of what the client renders. This is purely a visual compatibility
 * issue with legacy protocol translation.</p>
 *
 * <h3>Design Considerations</h3>
 * <ul>
 *   <li>If supporting 1.8 clients: Keep under 2.5 blocks/tick for clean visuals</li>
 *   <li>If modern clients only: No velocity restrictions needed</li>
 *   <li>If instant-hit weapons: High velocity is fine, visuals don't matter</li>
 * </ul>
 *
 * @param horizontalMultiplier Horizontal speed multiplier (1.0 = vanilla, ~1.5 blocks/tick)
 * @param verticalMultiplier Vertical speed multiplier (1.0 = vanilla, ~1.5 blocks/tick)
 * @param spreadMultiplier Random spread multiplier (1.0 = vanilla accuracy)
 * @param gravity Gravity applied per tick (0.05 = vanilla arrow)
 * @param horizontalAirResistance Horizontal drag (0.99 = vanilla)
 * @param verticalAirResistance Vertical drag (0.99 = vanilla)
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
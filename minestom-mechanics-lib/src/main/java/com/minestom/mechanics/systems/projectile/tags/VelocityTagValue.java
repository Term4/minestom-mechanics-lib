package com.minestom.mechanics.systems.projectile.tags;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.systems.ConfigTagWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Combined tag value for projectile velocity modifications - RECORD VERSION.
 * Works with Tag.Structure() for proper serialization.
 *
 * Usage:
 * <pre>
 * import static VelocityTagValue.*;
 *
 * // Just multiplier
 * .withTag(ProjectileVelocitySystem.CUSTOM, velMult(2.0, 1.5))
 *
 * // Just modify
 * .withTag(ProjectileVelocitySystem.CUSTOM, velAdd(0, 0, 0, 0.01, 0, 0))
 *
 * // Combined (chainable)
 * .withTag(ProjectileVelocitySystem.CUSTOM, velMult(0.5).thenAdd(0, 0, 0, 0.01, 0, 0))
 *
 * // Presets
 * .withTag(ProjectileVelocitySystem.CUSTOM, VEL_LASER)
 * </pre>
 */

// TODO: See if spawning the misc projectile with the players
//  yaw / pitch solves the high velocity desync on legacy clients
public record VelocityTagValue(
        @Nullable List<Double> multiplier,
        @Nullable List<Double> modify,
        @Nullable ProjectileVelocityConfig custom
) implements ConfigTagWrapper<ProjectileVelocityConfig> {

    // ===========================
    // STATIC FACTORY METHODS
    // ===========================

    /**
     * Multiplier for all 6 components: [hMult, vMult, spread, gravity, hAirRes, vAirRes]
     */
    public static VelocityTagValue velMult(double h, double v, double spread, double gravity, double hAirRes, double vAirRes) {
        return new VelocityTagValue(List.of(h, v, spread, gravity, hAirRes, vAirRes), null, null);
    }

    /** Multiplier for horizontal and vertical (others = 1.0) */
    public static VelocityTagValue velMult(double horizontal, double vertical) {
        return velMult(horizontal, vertical, 1.0, 1.0, 1.0, 1.0);
    }

    /** Uniform multiplier (affects horizontal and vertical equally) */
    public static VelocityTagValue velMult(double scale) {
        return velMult(scale, scale, 1.0, 1.0, 1.0, 1.0);
    }

    /**
     * Modify all 6 components: [hMult, vMult, spread, gravity, hAirRes, vAirRes]
     */
    public static VelocityTagValue velAdd(double h, double v, double spread, double gravity, double hAirRes, double vAirRes) {
        return new VelocityTagValue(null, List.of(h, v, spread, gravity, hAirRes, vAirRes), null);
    }

    /** Full config override */
    public static VelocityTagValue velSet(ProjectileVelocityConfig config) {
        return new VelocityTagValue(null, null, config);
    }

    // ===========================
    // CHAINABLE METHODS (return new records)
    // ===========================

    /** Chain: add multiplier (returns new record) */
    public VelocityTagValue thenMult(double h, double v, double spread, double gravity, double hAirRes, double vAirRes) {
        return new VelocityTagValue(List.of(h, v, spread, gravity, hAirRes, vAirRes), this.modify, this.custom);
    }

    public VelocityTagValue thenMult(double horizontal, double vertical) {
        return thenMult(horizontal, vertical, 1.0, 1.0, 1.0, 1.0);
    }

    /** Chain: add modify (returns new record) */
    public VelocityTagValue thenAdd(double h, double v, double spread, double gravity, double hAirRes, double vAirRes) {
        return new VelocityTagValue(this.multiplier, List.of(h, v, spread, gravity, hAirRes, vAirRes), this.custom);
    }

    /** Chain: set full config (returns new record) */
    public VelocityTagValue thenSet(ProjectileVelocityConfig config) {
        return new VelocityTagValue(this.multiplier, this.modify, config);
    }

    // ===========================
    // COMMON PRESETS
    // ===========================

    /** Laser: Fast, no gravity, no air resistance, no spread */
    public static final VelocityTagValue VEL_LASER = velMult(2.5, 2.5, 0.1, 0.0, 1.0, 1.0);

    /** Floaty: Slow, low gravity */
    public static final VelocityTagValue VEL_FLOATY = velMult(0.5, 0.5, 1.0, 0.01, 1.0, 1.0);

    /** Heavy: Slow, high gravity */
    public static final VelocityTagValue VEL_HEAVY = velMult(0.5, 0.5, 1.0, 1.2, 0.9, 0.9);

    /** Fast: 2x speed, normal everything else */
    public static final VelocityTagValue VEL_FAST = velMult(2.0);

    /** Slow: 0.5x speed */
    public static final VelocityTagValue VEL_SLOW = velMult(0.5);

    // ===========================
    // ConfigTagWrapper INTERFACE
    // ===========================

    @Override
    public List<Double> getMultiplier() {
        return multiplier;
    }

    @Override
    public List<Double> getModify() {
        return modify;
    }

    @Override
    public ProjectileVelocityConfig getCustom() {
        return custom;
    }
}
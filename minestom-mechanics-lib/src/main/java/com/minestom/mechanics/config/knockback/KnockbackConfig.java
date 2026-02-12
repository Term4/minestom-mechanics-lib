package com.minestom.mechanics.config.knockback;

import com.minestom.mechanics.systems.knockback.KnockbackSystem;

import java.util.Collections;
import java.util.Map;

public record KnockbackConfig(
        double horizontal,
        double vertical,
        double verticalLimit,
        double sprintBonusHorizontal,
        double sprintBonusVertical,
        double airMultiplierHorizontal,
        double airMultiplierVertical,
        double lookWeight,
        boolean modern,
        boolean knockbackSyncSupported,
        KnockbackSystem.KnockbackDirectionMode meleeDirection,
        KnockbackSystem.KnockbackDirectionMode projectileDirection,
        KnockbackSystem.DegenerateFallback degenerateFallback,
        Double sprintLookWeight,
        double horizontalFriction,
        double verticalFriction,
        KnockbackSystem.VelocityApplyMode velocityApplyMode,
        Map<KnockbackSystem.KnockbackVictimState, KnockbackSystem.KnockbackStateOverride> stateOverrides
) {
    public KnockbackConfig {}

    /** Backward-compatible constructor (defaults to ATTACKER_POSITION / SHOOTER_ORIGIN, LOOK, null, friction 2/2, SET, empty overrides). */
    public KnockbackConfig(double horizontal, double vertical, double verticalLimit,
                           double sprintBonusHorizontal, double sprintBonusVertical,
                           double airMultiplierHorizontal, double airMultiplierVertical,
                           double lookWeight, boolean modern, boolean knockbackSyncSupported) {
        this(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported,
                KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION, KnockbackSystem.KnockbackDirectionMode.SHOOTER_ORIGIN,
                KnockbackSystem.DegenerateFallback.LOOK, null,
                2.0, 2.0, KnockbackSystem.VelocityApplyMode.SET,
                Collections.emptyMap());
    }

    // ===========================
    // FACTORY METHODS
    // ===========================

    public static KnockbackConfig validated(
            double horizontal, double vertical, double verticalLimit,
            double sprintBonusHorizontal, double sprintBonusVertical,
            double airMultiplierHorizontal, double airMultiplierVertical,
            double lookWeight, boolean modern, boolean knockbackSyncSupported) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported,
                2.0, 2.0, KnockbackSystem.VelocityApplyMode.SET);
    }

    /**
     * Full validated constructor. Friction 0 = ignore old velocity for that axis.
     * velocityApplyMode: SET = replace velocity; ADD = add to current.
     * sprintLookWeight: null = default; 0..1 = weight of look vs sprint direction on sprint hits.
     */
    public static KnockbackConfig validated(
            double horizontal, double vertical, double verticalLimit,
            double sprintBonusHorizontal, double sprintBonusVertical,
            double airMultiplierHorizontal, double airMultiplierVertical,
            double lookWeight, boolean modern, boolean knockbackSyncSupported,
            double horizontalFriction, double verticalFriction,
            KnockbackSystem.VelocityApplyMode velocityApplyMode,
            Double sprintLookWeight) {
        validate(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight, horizontalFriction, verticalFriction);
        if (sprintLookWeight != null && (sprintLookWeight < 0 || sprintLookWeight > 1)) {
            throw new IllegalArgumentException("Sprint look weight must be between 0 and 1 when specified");
        }
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal,
                sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight,
                modern, knockbackSyncSupported,
                KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION,
                KnockbackSystem.KnockbackDirectionMode.SHOOTER_ORIGIN,
                KnockbackSystem.DegenerateFallback.LOOK, sprintLookWeight,
                horizontalFriction, verticalFriction, velocityApplyMode,
                Collections.emptyMap());
    }

    /** Full validated without sprint look weight (uses null = default). */
    public static KnockbackConfig validated(
            double horizontal, double vertical, double verticalLimit,
            double sprintBonusHorizontal, double sprintBonusVertical,
            double airMultiplierHorizontal, double airMultiplierVertical,
            double lookWeight, boolean modern, boolean knockbackSyncSupported,
            double horizontalFriction, double verticalFriction,
            KnockbackSystem.VelocityApplyMode velocityApplyMode) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported,
                horizontalFriction, verticalFriction, velocityApplyMode, null);
    }

    public static KnockbackConfig unvalidated(
            double horizontal, double vertical, double verticalLimit,
            double sprintBonusHorizontal, double sprintBonusVertical,
            double airMultiplierHorizontal, double airMultiplierVertical,
            double lookWeight, boolean modern, boolean knockbackSyncSupported) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal,
                sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight,
                modern, knockbackSyncSupported);
    }

    private static void validate(double horizontal, double vertical, double verticalLimit,
                                 double sprintBonusHorizontal, double sprintBonusVertical,
                                 double airMultiplierHorizontal, double airMultiplierVertical, double lookWeight,
                                 double horizontalFriction, double verticalFriction) {
        if (horizontal < 0) throw new IllegalArgumentException("Horizontal knockback cannot be negative");
        if (vertical < 0) throw new IllegalArgumentException("Vertical knockback cannot be negative");
        if (verticalLimit < 0) throw new IllegalArgumentException("Vertical limit cannot be negative");
        if (sprintBonusHorizontal < 0) throw new IllegalArgumentException("Sprint bonus horizontal cannot be negative");
        if (sprintBonusVertical < 0) throw new IllegalArgumentException("Sprint bonus vertical cannot be negative");
        if (airMultiplierHorizontal < 0) throw new IllegalArgumentException("Air multiplier horizontal cannot be negative");
        if (airMultiplierVertical < 0) throw new IllegalArgumentException("Air multiplier vertical cannot be negative");
        if (lookWeight < 0 || lookWeight > 1) throw new IllegalArgumentException("Look weight must be between 0 and 1");
        if (horizontalFriction < 0) throw new IllegalArgumentException("Horizontal friction cannot be negative (0 = ignore old velocity)");
        if (verticalFriction < 0) throw new IllegalArgumentException("Vertical friction cannot be negative (0 = ignore old velocity)");
    }

    // ===========================
    // WITH METHODS
    // ===========================

    public KnockbackConfig withKnockback(double horizontal, double vertical) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withKnockback(double horizontal, double vertical, double verticalLimit) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withHorizontal(double horizontal) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withVertical(double vertical) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withVerticalLimit(double verticalLimit) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withSprintBonus(double horizontal, double vertical) {
        return new KnockbackConfig(this.horizontal, this.vertical, verticalLimit, horizontal, vertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withSprintBonusHorizontal(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, v, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withSprintBonusVertical(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, v, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withAirMultipliers(double h, double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, h, v, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withAirMultiplierHorizontal(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, v, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withAirMultiplierVertical(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, v, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withLookWeight(double lookWeight) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withModern(boolean modern) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withKnockbackSyncSupported(boolean v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, v, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withMeleeDirection(KnockbackSystem.KnockbackDirectionMode mode) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, mode, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withProjectileDirection(KnockbackSystem.KnockbackDirectionMode mode) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, mode, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withDegenerateFallback(KnockbackSystem.DegenerateFallback degenerateFallback) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withSprintLookWeight(Double sprintLookWeight) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withHorizontalFriction(double horizontalFriction) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withVerticalFriction(double verticalFriction) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withVelocityApplyMode(KnockbackSystem.VelocityApplyMode velocityApplyMode) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }

    public KnockbackConfig withStateOverrides(Map<KnockbackSystem.KnockbackVictimState, KnockbackSystem.KnockbackStateOverride> stateOverrides) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight, horizontalFriction, verticalFriction, velocityApplyMode, stateOverrides);
    }
}

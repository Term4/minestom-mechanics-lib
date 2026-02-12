package com.minestom.mechanics.config.knockback;

import com.minestom.mechanics.systems.knockback.KnockbackSystem;

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
        Double sprintLookWeight
) {
    public KnockbackConfig {}

    /** Backward-compatible constructor (defaults to ATTACKER_POSITION / SHOOTER_ORIGIN, LOOK, null). */
    public KnockbackConfig(double horizontal, double vertical, double verticalLimit,
                           double sprintBonusHorizontal, double sprintBonusVertical,
                           double airMultiplierHorizontal, double airMultiplierVertical,
                           double lookWeight, boolean modern, boolean knockbackSyncSupported) {
        this(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported,
                KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION, KnockbackSystem.KnockbackDirectionMode.SHOOTER_ORIGIN,
                KnockbackSystem.DegenerateFallback.LOOK, null);
    }

    // ===========================
    // FACTORY METHODS
    // ===========================

    public static KnockbackConfig validated(
            double horizontal, double vertical, double verticalLimit,
            double sprintBonusHorizontal, double sprintBonusVertical,
            double airMultiplierHorizontal, double airMultiplierVertical,
            double lookWeight, boolean modern, boolean knockbackSyncSupported) {
        validate(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight);
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal,
                sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight,
                modern, knockbackSyncSupported);
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
                                 double airMultiplierHorizontal, double airMultiplierVertical, double lookWeight) {
        if (horizontal < 0) throw new IllegalArgumentException("Horizontal knockback cannot be negative");
        if (vertical < 0) throw new IllegalArgumentException("Vertical knockback cannot be negative");
        if (verticalLimit < 0) throw new IllegalArgumentException("Vertical limit cannot be negative");
        if (sprintBonusHorizontal < 0) throw new IllegalArgumentException("Sprint bonus horizontal cannot be negative");
        if (sprintBonusVertical < 0) throw new IllegalArgumentException("Sprint bonus vertical cannot be negative");
        if (airMultiplierHorizontal < 0) throw new IllegalArgumentException("Air multiplier horizontal cannot be negative");
        if (airMultiplierVertical < 0) throw new IllegalArgumentException("Air multiplier vertical cannot be negative");
        if (lookWeight < 0 || lookWeight > 1) throw new IllegalArgumentException("Look weight must be between 0 and 1");
    }

    // ===========================
    // WITH METHODS
    // ===========================

    public KnockbackConfig withKnockback(double horizontal, double vertical) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withKnockback(double horizontal, double vertical, double verticalLimit) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withHorizontal(double horizontal) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withVertical(double vertical) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withVerticalLimit(double verticalLimit) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withSprintBonus(double horizontal, double vertical) {
        return new KnockbackConfig(this.horizontal, this.vertical, verticalLimit, horizontal, vertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withSprintBonusHorizontal(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, v, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withSprintBonusVertical(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, v, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withAirMultipliers(double h, double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, h, v, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withAirMultiplierHorizontal(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, v, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withAirMultiplierVertical(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, v, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withLookWeight(double lookWeight) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withModern(boolean modern) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withKnockbackSyncSupported(boolean v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, v, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withMeleeDirection(KnockbackSystem.KnockbackDirectionMode mode) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, mode, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withProjectileDirection(KnockbackSystem.KnockbackDirectionMode mode) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, mode, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withDegenerateFallback(KnockbackSystem.DegenerateFallback degenerateFallback) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }

    public KnockbackConfig withSprintLookWeight(Double sprintLookWeight) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, sprintLookWeight);
    }
}

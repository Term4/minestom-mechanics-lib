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
        KnockbackSystem.DirectionBlendMode directionBlendMode,
        Double sprintLookWeight,
        double horizontalFriction,
        double verticalFriction,
        Double sprintHorizontalFriction,
        Double sprintVerticalFriction,
        KnockbackSystem.VelocityApplyMode velocityApplyMode,
        Map<KnockbackSystem.KnockbackVictimState, KnockbackSystem.KnockbackStateOverride> stateOverrides,
        KnockbackSystem.RangeReductionConfig rangeReduction,
        KnockbackSystem.RangeReductionConfig sprintRangeReduction,
        int sprintBufferTicks
) {
    public KnockbackConfig {}

    /** Backward-compatible constructor (defaults to ATTACKER_POSITION / SHOOTER_ORIGIN, LOOK, null, friction 2/2, SET, empty overrides, no range reduction). */
    public KnockbackConfig(double horizontal, double vertical, double verticalLimit,
                           double sprintBonusHorizontal, double sprintBonusVertical,
                           double airMultiplierHorizontal, double airMultiplierVertical,
                           double lookWeight, boolean modern, boolean knockbackSyncSupported) {
        this(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported,
                KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION, KnockbackSystem.KnockbackDirectionMode.SHOOTER_ORIGIN,
                KnockbackSystem.DegenerateFallback.LOOK, KnockbackSystem.DirectionBlendMode.BLEND_DIRECTION, null,
                2.0, 2.0, null, null, KnockbackSystem.VelocityApplyMode.SET,
                Collections.emptyMap(),
                KnockbackSystem.RangeReductionConfig.none(), KnockbackSystem.RangeReductionConfig.none(), 0);
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
     * lookWeight, sprintLookWeight: together (sprintLookWeight null = use lookWeight).
     * velocityApplyMode: SET = replace velocity; ADD = add to current.
     */
    public static KnockbackConfig validated(
            double horizontal, double vertical, double verticalLimit,
            double sprintBonusHorizontal, double sprintBonusVertical,
            double airMultiplierHorizontal, double airMultiplierVertical,
            double lookWeight, Double sprintLookWeight,
            boolean modern, boolean knockbackSyncSupported,
            double horizontalFriction, double verticalFriction,
            KnockbackSystem.VelocityApplyMode velocityApplyMode) {
        validate(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight, horizontalFriction, verticalFriction);
        if (sprintLookWeight != null && (sprintLookWeight < 0 || sprintLookWeight > 1)) {
            throw new IllegalArgumentException("Sprint look weight must be between 0 and 1 when specified");
        }
        var none = KnockbackSystem.RangeReductionConfig.none();
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal,
                sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight,
                modern, knockbackSyncSupported,
                KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION,
                KnockbackSystem.KnockbackDirectionMode.SHOOTER_ORIGIN,
                KnockbackSystem.DegenerateFallback.LOOK, KnockbackSystem.DirectionBlendMode.BLEND_DIRECTION, sprintLookWeight,
                horizontalFriction, verticalFriction, null, null, velocityApplyMode,
                Collections.emptyMap(), none, none, 0);
    }

    /**
     * Full validated with range reduction (separate for sprint/non-sprint).
     * lookWeight, sprintLookWeight: together (sprintLookWeight null = use lookWeight).
     * meleeDirection, projectileDirection, directionBlendMode: direction options.
     * sprintHorizontalFriction, sprintVerticalFriction: null = use base friction.
     */
    public static KnockbackConfig validated(
            double horizontal, double vertical, double verticalLimit,
            double sprintBonusHorizontal, double sprintBonusVertical,
            double airMultiplierHorizontal, double airMultiplierVertical,
            double lookWeight, Double sprintLookWeight,
            boolean modern, boolean knockbackSyncSupported,
            KnockbackSystem.KnockbackDirectionMode meleeDirection,
            KnockbackSystem.KnockbackDirectionMode projectileDirection,
            KnockbackSystem.DirectionBlendMode directionBlendMode,
            double horizontalFriction, double verticalFriction,
            Double sprintHorizontalFriction, Double sprintVerticalFriction,
            KnockbackSystem.VelocityApplyMode velocityApplyMode,
            double rangeStartH, double rangeStartV, double rangeFactorH, double rangeFactorV,
            double rangeMaxH, double rangeMaxV,
            double sprintRangeStartH, double sprintRangeStartV, double sprintRangeFactorH, double sprintRangeFactorV,
            double sprintRangeMaxH, double sprintRangeMaxV,
            int sprintBufferTicks) {
        validate(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight, horizontalFriction, verticalFriction);
        if (sprintLookWeight != null && (sprintLookWeight < 0 || sprintLookWeight > 1)) {
            throw new IllegalArgumentException("Sprint look weight must be between 0 and 1 when specified");
        }
        if (sprintHorizontalFriction != null && sprintHorizontalFriction < 0)
            throw new IllegalArgumentException("Sprint horizontal friction cannot be negative");
        if (sprintVerticalFriction != null && sprintVerticalFriction < 0)
            throw new IllegalArgumentException("Sprint vertical friction cannot be negative");
        if (rangeStartH < 0 || rangeStartV < 0 || rangeFactorH < 0 || rangeFactorV < 0
                || sprintRangeStartH < 0 || sprintRangeStartV < 0 || sprintRangeFactorH < 0 || sprintRangeFactorV < 0) {
            throw new IllegalArgumentException("Range start distances and factors cannot be negative");
        }
        if (rangeMaxH < 0 || rangeMaxV < 0 || sprintRangeMaxH < 0 || sprintRangeMaxV < 0) {
            throw new IllegalArgumentException("Range max cannot be negative");
        }
        if (sprintBufferTicks < 0) throw new IllegalArgumentException("Sprint buffer ticks cannot be negative");
        var baseRange = new KnockbackSystem.RangeReductionConfig(rangeStartH, rangeStartV, rangeFactorH, rangeFactorV, rangeMaxH, rangeMaxV);
        var sprintRange = new KnockbackSystem.RangeReductionConfig(sprintRangeStartH, sprintRangeStartV, sprintRangeFactorH, sprintRangeFactorV, sprintRangeMaxH, sprintRangeMaxV);
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal,
                sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight,
                modern, knockbackSyncSupported,
                meleeDirection, projectileDirection,
                KnockbackSystem.DegenerateFallback.LOOK, directionBlendMode, sprintLookWeight,
                horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode,
                Collections.emptyMap(),
                baseRange, sprintRange, sprintBufferTicks);
    }

    /** Same as above but direction modes default to ATTACKER_POSITION, SHOOTER_ORIGIN, BLEND_DIRECTION and sprint friction to null. */
    public static KnockbackConfig validated(
            double horizontal, double vertical, double verticalLimit,
            double sprintBonusHorizontal, double sprintBonusVertical,
            double airMultiplierHorizontal, double airMultiplierVertical,
            double lookWeight, Double sprintLookWeight,
            boolean modern, boolean knockbackSyncSupported,
            double horizontalFriction, double verticalFriction,
            KnockbackSystem.VelocityApplyMode velocityApplyMode,
            double rangeStartH, double rangeStartV, double rangeFactorH, double rangeFactorV,
            double rangeMaxH, double rangeMaxV,
            double sprintRangeStartH, double sprintRangeStartV, double sprintRangeFactorH, double sprintRangeFactorV,
            double sprintRangeMaxH, double sprintRangeMaxV,
            int sprintBufferTicks) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight, sprintLookWeight,
                modern, knockbackSyncSupported,
                KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION,
                KnockbackSystem.KnockbackDirectionMode.SHOOTER_ORIGIN,
                KnockbackSystem.DirectionBlendMode.BLEND_DIRECTION,
                horizontalFriction, verticalFriction, null, null, velocityApplyMode,
                rangeStartH, rangeStartV, rangeFactorH, rangeFactorV, rangeMaxH, rangeMaxV,
                sprintRangeStartH, sprintRangeStartV, sprintRangeFactorH, sprintRangeFactorV, sprintRangeMaxH, sprintRangeMaxV,
                sprintBufferTicks);
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
                airMultiplierHorizontal, airMultiplierVertical, lookWeight, null,
                modern, knockbackSyncSupported,
                horizontalFriction, verticalFriction, velocityApplyMode);
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
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withKnockback(double horizontal, double vertical, double verticalLimit) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withHorizontal(double horizontal) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withVertical(double vertical) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withVerticalLimit(double verticalLimit) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withSprintBonus(double horizontal, double vertical) {
        return new KnockbackConfig(this.horizontal, this.vertical, verticalLimit, horizontal, vertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withSprintBonusHorizontal(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, v, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withSprintBonusVertical(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, v, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withAirMultipliers(double h, double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, h, v, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withAirMultiplierHorizontal(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, v, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withAirMultiplierVertical(double v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, v, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withLookWeight(double lookWeight) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withModern(boolean modern) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withKnockbackSyncSupported(boolean v) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, v, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withMeleeDirection(KnockbackSystem.KnockbackDirectionMode mode) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, mode, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withProjectileDirection(KnockbackSystem.KnockbackDirectionMode mode) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, mode, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withDegenerateFallback(KnockbackSystem.DegenerateFallback degenerateFallback) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withDirectionBlendMode(KnockbackSystem.DirectionBlendMode directionBlendMode) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, directionBlendMode, sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withSprintLookWeight(Double sprintLookWeight) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withHorizontalFriction(double horizontalFriction) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withVerticalFriction(double verticalFriction) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withSprintFriction(double sprintHorizontalFriction, double sprintVerticalFriction) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withVelocityApplyMode(KnockbackSystem.VelocityApplyMode velocityApplyMode) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withStateOverrides(Map<KnockbackSystem.KnockbackVictimState, KnockbackSystem.KnockbackStateOverride> stateOverrides) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    public KnockbackConfig withSprintBufferTicks(int sprintBufferTicks) {
        if (sprintBufferTicks < 0) throw new IllegalArgumentException("Sprint buffer ticks cannot be negative");
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, rangeReduction, sprintRangeReduction, sprintBufferTicks);
    }

    /** Set range reduction with same start distance for both axes (same for sprint and non-sprint, no max cap). */
    public KnockbackConfig withRangeReduction(double startDistance, double factorHorizontal, double factorVertical) {
        return withRangeReduction(startDistance, startDistance, factorHorizontal, factorVertical);
    }

    /** Set range reduction with per-axis start/factor (same for sprint and non-sprint, no max cap). */
    public KnockbackConfig withRangeReduction(double startH, double startV, double factorH, double factorV) {
        var cfg = new KnockbackSystem.RangeReductionConfig(startH, startV, factorH, factorV, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        return withRangeReduction(cfg, cfg);
    }

    /** Set range reduction for both sprint and non-sprint (same config). Use RangeReductionConfig.none() for no reduction. */
    public KnockbackConfig withRangeReduction(KnockbackSystem.RangeReductionConfig base,
                                             KnockbackSystem.RangeReductionConfig sprint) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, meleeDirection, projectileDirection, degenerateFallback, this.directionBlendMode(), sprintLookWeight, horizontalFriction, verticalFriction, sprintHorizontalFriction, sprintVerticalFriction, velocityApplyMode, stateOverrides, base, sprint, sprintBufferTicks);
    }

    /** Set range reduction with same config for sprint and non-sprint. */
    public KnockbackConfig withRangeReduction(KnockbackSystem.RangeReductionConfig config) {
        return withRangeReduction(config, config);
    }
}

package com.minestom.mechanics.config.knockback;

import com.minestom.mechanics.systems.knockback.KnockbackSystem;

import java.util.Map;

public final class KnockbackPresets {
    private KnockbackPresets() {} // Prevent instantiation

    /** Friction 2.0 = retain 50% of old velocity. 0 = ignore old velocity. */
    private static final double LEGACY_FRICTION = 2.0;

    /** Minemen-style with sprint look weight 0.5 (blend look/sprint on sprint hits). */
    public static KnockbackConfig minemen() {
        return KnockbackConfig.validated(
                0.375, 0.365, 0.45,
                0.34, 0.0,
                1.0, 1.0, 0.5,
                false, false,
                LEGACY_FRICTION, LEGACY_FRICTION,
                KnockbackSystem.VelocityApplyMode.SET,
                0.5  // sprintLookWeight
        );
    }

    public static KnockbackConfig vanilla18() {
        return KnockbackConfig.validated(
                0.4, 0.4, 0.4,
                0.0, 0.0,
                1.0, 1.0, 0.0,
                false, false,
                LEGACY_FRICTION, LEGACY_FRICTION,
                KnockbackSystem.VelocityApplyMode.SET
        );
    }

    public static KnockbackConfig hypixel() {
        return KnockbackConfig.validated(
                0.42, 0.36, 0.38,
                0.08, 0.04,
                1.0, 1.0, 0.0,
                false, false,
                LEGACY_FRICTION, LEGACY_FRICTION,
                KnockbackSystem.VelocityApplyMode.SET
        );
    }

    /**
     * Pure knockback: ignore old velocity (friction=0), SET result.
     */
    public static KnockbackConfig zeroOldVelocity() {
        return KnockbackConfig.validated(
                0.4, 0.4, 0.4,
                0.0, 0.0,
                1.0, 1.0, 0.0,
                false, false,
                0, 0,  // friction 0 = ignore old
                KnockbackSystem.VelocityApplyMode.SET
        );
    }

    /**
     * Add knockback to current velocity: ignore old (friction=0), ADD result.
     */
    public static KnockbackConfig addToVelocity() {
        return KnockbackConfig.validated(
                0.4, 0.4, 0.4,
                0.0, 0.0,
                1.0, 1.0, 0.0,
                false, false,
                0, 0,
                KnockbackSystem.VelocityApplyMode.ADD
        );
    }

    /**
     * Example preset with state overrides: reduced vertical when falling.
     */
    public static KnockbackConfig withFallingOverride(KnockbackConfig base) {
        var fallOverride = new KnockbackSystem.KnockbackStateOverride(
                null, null, null,
                1.0, 0.6,  // 60% vertical when falling
                null
        );
        return base.withStateOverrides(Map.of(
                KnockbackSystem.KnockbackVictimState.FALLING, fallOverride
        ));
    }
}
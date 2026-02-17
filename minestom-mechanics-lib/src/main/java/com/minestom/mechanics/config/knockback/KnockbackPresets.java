package com.minestom.mechanics.config.knockback;

import com.minestom.mechanics.systems.knockback.KnockbackSystem;

import java.util.Map;

public final class KnockbackPresets {
    private KnockbackPresets() {} // Prevent instantiation

    /** Friction 2.0 = retain 50% of old velocity. 0 = ignore old velocity. */
    private static final double LEGACY_FRICTION = 2.0;

    /** Range reduction: start 2.5 blocks, modest falloff for PvP (non-sprint and sprint). */
    private static final double RANGE_START_PVP = 2.5;
    private static final double RANGE_FACTOR_H_PVP = 0.04;
    private static final double RANGE_FACTOR_V_PVP = 0.025;
    private static final double RANGE_MAX = Double.POSITIVE_INFINITY;

    /** Sprint buffer: ticks after stopping sprint we still apply sprint bonus (e.g. 5 = 250ms at 20 TPS). */
    private static final int SPRINT_BUFFER_TICKS = 8;

    /** Minemen-style with sprint look weight 0.5, vector addition, range reduction, sprint buffer. */
    public static KnockbackConfig minemen() {
        return KnockbackConfig.validated(
                0.525, 0.4, 0.365,
                0.3534, 0.0,
                1.0, 1.0,
                0.5, 0.5,
                false, false,
                KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION,
                KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION,
                KnockbackSystem.DirectionBlendMode.ADD_VECTORS,
                7, 6.5,
                6.0, 6.5,
                KnockbackSystem.VelocityApplyMode.SET,
                0, 0, 0, 0, 0, 0,
                4, 0, 0.25, 0, 0.4, 0,
                8
        );
    }

    /** Vanilla 1.8 — no range reduction. */
    public static KnockbackConfig vanilla18() {
        return KnockbackConfig.validated(
                0.4, 0.4, 0.4,
                0.0, 0.0,
                1.0, 1.0,
                0.0, 1.0,
                false, false,
                LEGACY_FRICTION, LEGACY_FRICTION,
                KnockbackSystem.VelocityApplyMode.SET
        );
    }

    /** Hypixel-style with range reduction (separate sprint/non-sprint), sprint buffer. */
    public static KnockbackConfig hypixel() {
        return KnockbackConfig.validated(
                0.42, 0.36, 0.38,
                0.08, 0.04,
                1.0, 1.0,
                0.0, 1.0,
                false, false,
                LEGACY_FRICTION, LEGACY_FRICTION,
                KnockbackSystem.VelocityApplyMode.SET,
                RANGE_START_PVP, RANGE_START_PVP, RANGE_FACTOR_H_PVP, RANGE_FACTOR_V_PVP, RANGE_MAX, RANGE_MAX,
                RANGE_START_PVP, RANGE_START_PVP, RANGE_FACTOR_H_PVP, RANGE_FACTOR_V_PVP, RANGE_MAX, RANGE_MAX,
                SPRINT_BUFFER_TICKS
        );
    }
}
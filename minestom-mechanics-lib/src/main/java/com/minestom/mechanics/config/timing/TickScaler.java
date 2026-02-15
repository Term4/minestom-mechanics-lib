package com.minestom.mechanics.config.timing;

import net.minestom.server.ServerFlag;

/**
 * Scales tick-based values according to {@link TickScalingMode} and server TPS.
 * All base values are assumed to be "at 20 TPS".
 */
public final class TickScaler {

    private static final int BASE_TPS = 20;

    private TickScaler() {}

    /**
     * Scale a tick count. SCALED: baseTicks * (tps/20). UNSCALED: baseTicks.
     */
    public static int scale(int baseTicksAt20, TickScalingMode mode) {
        if (mode == TickScalingMode.UNSCALED) return baseTicksAt20;
        int tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        return (int) Math.round(baseTicksAt20 * (double) tps / BASE_TPS);
    }

    /**
     * Scale a tick count. SCALED: baseTicks * (tps/20). UNSCALED: baseTicks.
     */
    public static long scale(long baseTicksAt20, TickScalingMode mode) {
        if (mode == TickScalingMode.UNSCALED) return baseTicksAt20;
        int tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        return Math.round(baseTicksAt20 * (double) tps / BASE_TPS);
    }

    /**
     * Multiplier for velocity conversion (e.g. toPerTickVelocity).
     * SCALED: 20 (constant blocks/s regardless of TPS).
     * UNSCALED: actual TPS.
     */
    public static double velocityMultiplier(TickScalingMode mode) {
        if (mode == TickScalingMode.UNSCALED) return ServerFlag.SERVER_TICKS_PER_SECOND;
        return BASE_TPS;
    }

    /**
     * Multiplier for gravity term applied each tick.
     * SCALED: 20/tps so that (gravity * mult) * tps = gravity*20 per second (constant).
     * UNSCALED: actual TPS.
     */
    public static double gravityMultiplier(TickScalingMode mode) {
        if (mode == TickScalingMode.UNSCALED) return ServerFlag.SERVER_TICKS_PER_SECOND;
        int tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        return (double) BASE_TPS * BASE_TPS / tps;
    }

    /**
     * Per-tick drag so that over 1 second the effective drag matches 20 ticks of the base value.
     * SCALED: pow(drag, 20/tps).
     * UNSCALED: drag as-is.
     */
    public static double dragPerTick(double dragAt20Ticks, TickScalingMode mode) {
        if (mode == TickScalingMode.UNSCALED) return dragAt20Ticks;
        int tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        return Math.pow(dragAt20Ticks, (double) BASE_TPS / tps);
    }

    /**
     * Divisor for converting stored velocity to per-tick movement.
     * Always uses actual TPS since velocity storage format is TPS-dependent.
     */
    public static double velocityDivisor() {
        return ServerFlag.SERVER_TICKS_PER_SECOND;
    }

    /**
     * Divisor for velocity sent to clients in packets (SpawnEntity, EntityVelocity).
     * Clients run at 20 TPS for physics, so use 20 when SCALED so client interpolation matches server.
     */
    public static double velocityPacketDivisor(TickScalingMode mode) {
        if (mode == TickScalingMode.UNSCALED) return ServerFlag.SERVER_TICKS_PER_SECOND;
        return BASE_TPS;
    }
}

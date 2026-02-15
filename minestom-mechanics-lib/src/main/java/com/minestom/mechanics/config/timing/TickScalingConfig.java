package com.minestom.mechanics.config.timing;

/**
 * Global tick-scaling mode. Call {@link #initialize(TickScalingMode)} before systems that use
 * tick values (HealthSystem, KnockbackSystem, projectiles, etc.). If never initialized,
 * defaults to {@link TickScalingMode#SCALED}.
 */
public final class TickScalingConfig {

    private static volatile TickScalingMode mode = TickScalingMode.SCALED;

    private TickScalingConfig() {}

    /**
     * Set the tick-scaling mode. Call early, before initializing systems.
     */
    public static void initialize(TickScalingMode m) {
        if (m == null) throw new IllegalArgumentException("TickScalingMode cannot be null");
        mode = m;
    }

    /**
     * Get the current tick-scaling mode.
     */
    public static TickScalingMode getMode() {
        return mode;
    }
}

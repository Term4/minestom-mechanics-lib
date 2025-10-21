package com.minestom.mechanics.util;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Generic class for combined tag wrappers.
 * Allows ConfigurableSystem to work with any wrapper type generically.
 *
 * @param <TConfig> The config type this wrapper holds (e.g., KnockbackConfig)
 */
public interface ConfigTagWrapper<TConfig> {

    /**
     * Get the multiplier array, or null if not set.
     */
    @Nullable
    List<Double> getMultiplier();

    /**
     * Get the modify array, or null if not set.
     */
    @Nullable
    List<Double> getModify();

    /**
     * Get the full config override, or null if not set.
     */
    @Nullable
    TConfig getCustom();

    /**
     * Check if this wrapper is empty (no values set).
     */
    default boolean isEmpty() {
        return getMultiplier() == null && getModify() == null && getCustom() == null;
    }
}
package com.minestom.mechanics.manager;

import net.minestom.server.entity.Player;

/**
 * Unified lifecycle interface for all managers and systems.
 * Provides common lifecycle methods that AbstractManager can call automatically.
 *
 * All methods have default implementations, so components only implement what they need.
 * Used by both:
 * - Managers (via AbstractManager)
 * - Systems (via InitializableSystem)
 */
public interface Lifecycle {

    /**
     * Clean up resources for a specific player.
     * Called automatically when a player disconnects.
     *
     * @param player the player to clean up
     */
    default void cleanupPlayer(Player player) {
        // Default: no-op
    }

    /**
     * Shutdown this component and release all resources.
     * Called automatically when the parent manager shuts down.
     */
    default void shutdown() {
        // Default: no-op
    }

    /**
     * Check if this component is initialized and ready for use.
     *
     * @return true if initialized, false otherwise
     */
    default boolean isInitialized() {
        return true; // Assume initialized by default
    }

    /**
     * Get a human-readable status string.
     * Useful for debugging and monitoring.
     *
     * @return status string describing the current state
     */
    default String getStatus() {
        return getClass().getSimpleName() + ": Active";
    }
}
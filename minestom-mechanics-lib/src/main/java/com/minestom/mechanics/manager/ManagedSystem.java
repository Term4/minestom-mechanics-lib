package com.minestom.mechanics.manager;

import net.minestom.server.entity.Player;

/**
 * Interface for systems that are managed by a manager.
 * Provides lifecycle methods that AbstractManager can call automatically.
 *
 * All methods have default implementations, so systems only implement what they need.
 */
public interface ManagedSystem {

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
     * Shutdown this system and release all resources.
     * Called automatically when the manager shuts down.
     */
    default void shutdown() {
        // Default: no-op
    }
}
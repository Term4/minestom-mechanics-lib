package com.minestom.mechanics.manager;

import net.minestom.server.entity.Player;

/**
 * Lifecycle interface for all manager classes.
 * Provides a consistent contract for initialization, cleanup, and status reporting.
 *
 * Implemented by AbstractManager, which provides default implementations and helpers.
 * All managers should extend AbstractManager rather than implementing this directly.
 */
public interface ManagerLifecycle {
    
    /**
     * Check if the manager is currently initialized and ready for use.
     * 
     * @return true if initialized, false otherwise
     */
    boolean isInitialized();
    
    /**
     * Clean up resources for a specific player.
     * This should be called when a player disconnects or leaves the server.
     * 
     * @param player the player to clean up
     */
    void cleanupPlayer(Player player);
    
    /**
     * Shutdown the manager and clean up all resources.
     * This should be called when the server is shutting down.
     */
    void shutdown();
    
    /**
     * Get a human-readable status string for this manager.
     * Useful for debugging and monitoring.
     * 
     * @return status string describing the current state
     */
    String getStatus();
}


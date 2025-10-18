package com.minestom.mechanics.manager;

import net.minestom.server.entity.Player;

/**
 * Lifecycle interface for all manager classes.
 * Provides a consistent pattern for initialization, cleanup, and status reporting.
 * 
 * This interface replaces the AbstractManager class to provide more flexibility
 * while maintaining consistent lifecycle management across all managers.
 */
public interface ManagerLifecycle {
    
    /**
     * Initialize the manager with required configuration.
     * This method should be called once before the manager is used.
     * 
     * @return true if initialization was successful, false otherwise
     */
    boolean initialize();
    
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


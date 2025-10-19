package com.minestom.mechanics.projectile.features;

import net.minestom.server.entity.Player;

// TODO: Add the ability to enable / disable individual projectiles
//  (i.e handle snowballs and eggs, but disable ender pearls)

/**
 * Interface for projectile features that can be managed by ProjectileManager.
 * Provides common methods for cleanup and lifecycle management.
 */
public interface ProjectileFeature {
    
    /**
     * Clean up any resources associated with a player.
     * Called when a player disconnects or when the feature needs to reset.
     * 
     * @param player The player to clean up
     */
    void cleanup(Player player);
    
    /**
     * Shutdown the feature and clean up any global resources.
     * Called when the server is shutting down.
     */
    default void shutdown() {
        // Optional implementation - most features don't need global cleanup
    }
}

package com.minestom.mechanics.config;

import com.minestom.mechanics.config.world.WorldInteractionConfig;

/**
 * Static class to hold server-wide configuration.
 * Stores settings that apply to the entire server, not per-combat-mode.
 * 
 * This class manages:
 * - World interaction settings (block reach, raycasting)
 * - Server-wide configuration that doesn't change per combat mode
 */
public class ServerConfig {
    
    private static WorldInteractionConfig worldInteractionConfig;
    
    private ServerConfig() {
        // Static class - prevent instantiation
    }
    
    /**
     * Get the world interaction configuration.
     * 
     * @return the world interaction config, or null if not set
     */
    public static WorldInteractionConfig getWorldInteraction() {
        return worldInteractionConfig;
    }
    
    /**
     * Set the world interaction configuration.
     * 
     * @param config the world interaction config to set
     */
    public static void setWorldInteraction(WorldInteractionConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("World interaction config cannot be null");
        }
        worldInteractionConfig = config;
    }
    
    /**
     * Check if world interaction configuration is set.
     * 
     * @return true if world interaction config is set, false otherwise
     */
    public static boolean isWorldInteractionSet() {
        return worldInteractionConfig != null;
    }
    
    /**
     * Reset all server configuration to defaults.
     * Useful for testing or reinitialization.
     */
    public static void reset() {
        worldInteractionConfig = null;
    }
}

package com.minestom.mechanics.config.gameplay;

/**
 * Configuration for player collision settings.
 * Controls whether players can collide with each other.
 * 
 * @param enabled Whether player collisions are enabled
 */
public record PlayerCollisionConfig(
    boolean enabled
) {
    
    /**
     * Default configuration with collisions enabled
     */
    public static PlayerCollisionConfig defaultConfig() {
        return new PlayerCollisionConfig(true);
    }
    
    /**
     * Configuration with collisions disabled
     */
    public static PlayerCollisionConfig noCollisions() {
        return new PlayerCollisionConfig(false);
    }
}

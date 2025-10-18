package com.minestom.mechanics.util;

import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;

/**
 * Consolidated gameplay utilities.
 * ✅ CONSOLIDATED: Merged EntityUtil functionality into general gameplay utilities.
 */
public class GameplayUtils {
    
    /**
     * Get a display name for an entity.
     * Returns the player's username for players, or the class name for other entities.
     * 
     * @param entity The entity to get the name for
     * @return A display name for the entity
     */
    public static String getEntityName(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getUsername();
        }
        return entity.getClass().getSimpleName();
    }
}


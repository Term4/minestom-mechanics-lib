package com.minestom.mechanics.projectile.components;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import com.minestom.mechanics.projectile.entities.FishingBobber;

/**
 * Manages fishing bobber state for players.
 * Handles tracking and retrieving fishing bobbers.
 */
public class FishingBobberManager {
    private static final LogUtil.SystemLogger log = LogUtil.system("FishingBobberManager");
    
    public static final Tag<FishingBobber> FISHING_BOBBER = Tag.Transient("fishingBobber");
    
    /**
     * Check if a player has an active fishing bobber
     * @param player The player to check
     * @return true if player has an active bobber
     */
    public boolean hasActiveBobber(Player player) {
        return player.hasTag(FISHING_BOBBER);
    }
    
    /**
     * Get the player's active fishing bobber
     * @param player The player
     * @return The fishing bobber, or null if none active
     */
    public FishingBobber getActiveBobber(Player player) {
        return player.getTag(FISHING_BOBBER);
    }
    
    /**
     * Set a player's active fishing bobber
     * @param player The player
     * @param bobber The fishing bobber
     */
    public void setActiveBobber(Player player, FishingBobber bobber) {
        player.setTag(FISHING_BOBBER, bobber);
        log.debug("Set active bobber for {}", player.getUsername());
    }
    
    /**
     * Remove a player's active fishing bobber
     * @param player The player
     */
    public void removeActiveBobber(Player player) {
        FishingBobber bobber = getActiveBobber(player);
        if (bobber != null) {
            // Remove the bobber entity from the world
            bobber.remove();
        }
        player.removeTag(FISHING_BOBBER);
        log.debug("Removed active bobber for {}", player.getUsername());
    }
    
    /**
     * Retrieve a fishing bobber and return durability damage
     * @param player The player
     * @return The durability damage amount
     */
    public int retrieveBobber(Player player) {
        FishingBobber bobber = getActiveBobber(player);
        if (bobber == null) {
            return 0;
        }
        
        int durability = bobber.retrieve();
        removeActiveBobber(player);
        
        log.debug("Retrieved bobber for {} with {} durability damage", 
                player.getUsername(), durability);
        
        return durability;
    }
}

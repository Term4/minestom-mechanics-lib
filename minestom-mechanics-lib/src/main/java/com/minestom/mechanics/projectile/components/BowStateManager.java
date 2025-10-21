package com.minestom.mechanics.projectile.components;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.Player;

import java.util.UUID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: This is actually pretty good.

/**
 * Manages bow drawing state for players.
 * Handles tracking which players are currently drawing bows.
 */
@Deprecated
public class BowStateManager {
    private static final LogUtil.SystemLogger log = LogUtil.system("BowStateManager");
    
    private final Map<UUID, Boolean> drawingBows = new ConcurrentHashMap<>();
    
    /**
     * Start tracking a player as drawing a bow
     */
    public void startDrawing(Player player) {
        UUID uuid = player.getUuid();
        
        if (isDrawingBow(player)) {
            return; // Already drawing
        }
        
        drawingBows.put(uuid, true);
        log.debug("{} started drawing bow", player.getUsername());
    }
    
    /**
     * Stop tracking a player as drawing a bow
     */
    public void stopDrawing(Player player) {
        UUID uuid = player.getUuid();
        
        if (!isDrawingBow(player)) {
            return; // Not drawing
        }
        
        drawingBows.remove(uuid);
        log.debug("{} stopped drawing bow", player.getUsername());
    }
    
    /**
     * Check if a player is currently drawing a bow
     */
    public boolean isDrawingBow(Player player) {
        return Boolean.TRUE.equals(drawingBows.get(player.getUuid()));
    }
    
    /**
     * Clean up player data
     */
    public void cleanup(Player player) {
        drawingBows.remove(player.getUuid());
    }
    
    /**
     * Get the number of players currently drawing bows
     */
    public int getDrawingPlayersCount() {
        return drawingBows.size();
    }
}

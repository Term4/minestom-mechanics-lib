package com.minestom.mechanics.util;

import net.minestom.server.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for managing player-specific state.
 * ✅ ABSTRACTION: Provides common pattern for state management across different systems.
 * 
 * @param <T> The type of state to manage for each player
 */
public abstract class PlayerStateManager<T> {
    protected final Map<UUID, T> playerStates = new ConcurrentHashMap<>();
    
    /**
     * Get the current state for a player.
     * 
     * @param player The player to get state for
     * @return The player's current state, or null if not set
     */
    public T getState(Player player) {
        return playerStates.get(player.getUuid());
    }
    
    /**
     * Set the state for a player.
     * 
     * @param player The player to set state for
     * @param state The new state
     */
    public void setState(Player player, T state) {
        if (state == null) {
            playerStates.remove(player.getUuid());
        } else {
            playerStates.put(player.getUuid(), state);
        }
    }
    
    /**
     * Clear the state for a player.
     * 
     * @param player The player to clear state for
     */
    public void clearState(Player player) {
        playerStates.remove(player.getUuid());
    }
    
    /**
     * Check if a player has a state set.
     * 
     * @param player The player to check
     * @return true if the player has a state, false otherwise
     */
    public boolean hasState(Player player) {
        return playerStates.containsKey(player.getUuid());
    }
    
    /**
     * Get the number of players with active states.
     * 
     * @return The number of active states
     */
    public int getActiveStateCount() {
        return playerStates.size();
    }
    
    /**
     * Clear all states (useful for shutdown).
     */
    public void clearAllStates() {
        playerStates.clear();
    }
}


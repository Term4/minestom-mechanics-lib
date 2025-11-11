package com.minestom.mechanics.manager;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

// TODO: Make this more useful, or remove it...

/**
 * Common utilities for manager implementations.
 * Consolidates repeated patterns across all manager classes.
 */
public final class ManagerUtils {
    
    private ManagerUtils() {
        // Utility class
    }
    
    /**
     * Build a standardized status string for a manager.
     * 
     * @param managerName the name of the manager
     * @param initialized whether the manager is initialized
     * @param additionalInfo any additional status information
     * @return formatted status string
     */
    public static String buildStatus(String managerName, boolean initialized, String additionalInfo) {
        StringBuilder status = new StringBuilder();
        status.append(managerName).append(": ");
        status.append(initialized ? "✓ Initialized" : "✗ Not Initialized");
        
        if (additionalInfo != null && !additionalInfo.trim().isEmpty()) {
            status.append(" (").append(additionalInfo).append(")");
        }
        
        return status.toString();
    }
    
    /**
     * Build a standardized status string for a manager with player count.
     * 
     * @param managerName the name of the manager
     * @param initialized whether the manager is initialized
     * @param playerCount the number of players being tracked
     * @return formatted status string
     */
    public static String buildStatusWithPlayerCount(String managerName, boolean initialized, int playerCount) {
        return buildStatus(managerName, initialized, playerCount + " players tracked");
    }
    
    /**
     * Safely execute a cleanup operation with error handling.
     * 
     * @param operation the cleanup operation to execute
     * @param operationName the name of the operation for logging
     * @param logger the logger to use for error reporting
     */
    public static void safeCleanup(Runnable operation, String operationName, LogUtil.SystemLogger logger) {
        try {
            operation.run();
        } catch (Exception e) {
            logger.error("Error during " + operationName + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Safely execute a shutdown operation with error handling.
     * 
     * @param operation the shutdown operation to execute
     * @param operationName the name of the operation for logging
     * @param logger the logger to use for error reporting
     */
    public static void safeShutdown(Runnable operation, String operationName, LogUtil.SystemLogger logger) {
        try {
            operation.run();
        } catch (Exception e) {
            logger.error("Error during " + operationName + " shutdown: " + e.getMessage(), e);
        }
    }
    
    /**
     * A generic player state tracker that can be used by managers.
     * 
     * @param <T> the type of state to track per player
     */
    public static class PlayerStateTracker<T> {
        private final Map<UUID, T> playerStates = new ConcurrentHashMap<>();
        
        /**
         * Get the state for a specific player.
         * 
         * @param player the player
         * @return the player's state, or null if not tracked
         */
        public T getState(Player player) {
            return playerStates.get(player.getUuid());
        }
        
        /**
         * Set the state for a specific player.
         * 
         * @param player the player
         * @param state the state to set
         */
        public void setState(Player player, T state) {
            if (state == null) {
                playerStates.remove(player.getUuid());
            } else {
                playerStates.put(player.getUuid(), state);
            }
        }
        
        /**
         * Remove the state for a specific player.
         * 
         * @param player the player
         * @return the previous state, or null if not tracked
         */
        public T removeState(Player player) {
            return playerStates.remove(player.getUuid());
        }
        
        /**
         * Clear all player states.
         */
        public void clearAll() {
            playerStates.clear();
        }
        
        /**
         * Get the number of players being tracked.
         * 
         * @return the number of tracked players
         */
        public int size() {
            return playerStates.size();
        }
        
        /**
         * Check if a player is being tracked.
         * 
         * @param player the player to check
         * @return true if tracked, false otherwise
         */
        public boolean isTracked(Player player) {
            return playerStates.containsKey(player.getUuid());
        }
    }
}


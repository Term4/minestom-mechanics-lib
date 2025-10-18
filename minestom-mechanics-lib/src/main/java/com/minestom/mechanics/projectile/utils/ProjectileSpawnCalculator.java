package com.minestom.mechanics.projectile.utils;

import com.minestom.mechanics.features.gameplay.EyeHeightSystem;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

/**
 * Utility class for calculating projectile spawn positions.
 * Consolidates spawn position logic to eliminate code duplication.
 */
public class ProjectileSpawnCalculator {
    
    /**
     * Calculate spawn position for projectiles with 1.8 compatibility.
     * Uses legacy eye height and proper forward offset to avoid collision with player.
     * 
     * @param player The shooting/throwing player
     * @return The calculated spawn position
     */
    public static Pos calculateSpawnPosition(Player player) {
        // Use EyeHeightSystem for correct eye height (configurable standing/sneaking heights)
        Pos eyePos = EyeHeightSystem.getInstance().getEyePosition(player);
        
        // 1.8 compatibility: spawn directly from player's face, not in front
        double yawRad = Math.toRadians(eyePos.yaw());
        double pitchRad = Math.toRadians(eyePos.pitch());
        
        // Calculate forward direction from player's view
        double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad) * 0.1; // Small offset
        double forwardY = -Math.sin(pitchRad) * 0.1;
        double forwardZ = Math.cos(yawRad) * Math.cos(pitchRad) * 0.1;
        
        // Add vertical offset for 1.8 style spawning
        return eyePos.add(forwardX, forwardY - 0.05, forwardZ);
    }
}

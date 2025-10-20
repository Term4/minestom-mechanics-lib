package com.minestom.mechanics.projectile.utils;

import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for common velocity calculations across projectile systems.
 * Provides shared mathematical operations for directional velocity, spread, and scaling.
 */
public class VelocityCalculator {
    
    /**
     * Calculate directional velocity from pitch and yaw angles.
     * Uses standard trigonometric calculations for 3D direction vectors.
     * 
     * @param pitch The pitch angle in degrees
     * @param yaw The yaw angle in degrees  
     * @param power The velocity magnitude
     * @return The calculated velocity vector
     */
    public static Vec calculateDirectionalVelocity(float pitch, float yaw, double power) {
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);
        
        double x = -Math.sin(yawRad) * Math.cos(pitchRad) * power;
        double y = -Math.sin(pitchRad) * power;
        double z = Math.cos(yawRad) * Math.cos(pitchRad) * power;
        
        return new Vec(x, y, z);
    }
    
    /**
     * Apply random spread to a velocity vector.
     * Adds Gaussian noise to simulate inaccuracy.
     * 
     * @param velocity The base velocity vector
     * @param spread The spread amount (standard deviation)
     * @return The velocity with applied spread
     */
    public static Vec applySpread(Vec velocity, double spread) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        double length = velocity.length();
        if (length == 0) return velocity;
        
        return velocity
                .div(length)
                .add(
                        random.nextGaussian() * spread,
                        random.nextGaussian() * spread,
                        random.nextGaussian() * spread
                )
                .mul(length);
    }
    
    /**
     * Normalize and scale a velocity vector to a target magnitude.
     * 
     * @param velocity The velocity vector to normalize and scale
     * @param targetMagnitude The target magnitude
     * @return The normalized and scaled velocity
     */
    public static Vec normalizeAndScale(Vec velocity, double targetMagnitude) {
        double currentLength = velocity.length();
        if (currentLength == 0) return Vec.ZERO;
        
        return velocity.div(currentLength).mul(targetMagnitude);
    }
    
    /**
     * Convert velocity to per-tick velocity for Minestom.
     * Multiplies by server ticks per second.
     * 
     * @param velocity The velocity vector
     * @param multiplier Additional multiplier (e.g., 0.75 for fishing rods)
     * @return The per-tick velocity
     */
    public static Vec toPerTickVelocity(Vec velocity, double multiplier) {
        return velocity.mul(ServerFlag.SERVER_TICKS_PER_SECOND * multiplier);
    }
    
    /**
     * Convert velocity to per-tick velocity with default multiplier of 1.0.
     * 
     * @param velocity The velocity vector
     * @return The per-tick velocity
     */
    public static Vec toPerTickVelocity(Vec velocity) {
        return toPerTickVelocity(velocity, 1.0);
    }
    
    /**
     * Calculate spawn position offset from player position.
     * Uses trigonometric calculations to position projectile in front of player.
     * 
     * @param playerPos The player's position
     * @param yaw The player's yaw angle
     * @param offsetDistance The distance to offset from player
     * @param eyeHeight The height offset (typically player eye height)
     * @return The calculated spawn position
     */
    public static Pos calculateSpawnOffset(Pos playerPos, float yaw, double offsetDistance, double eyeHeight) {
        float zDir = (float) Math.cos(Math.toRadians(-yaw) - Math.PI);
        float xDir = (float) Math.sin(Math.toRadians(-yaw) - Math.PI);
        
        double x = playerPos.x() - (double) xDir * offsetDistance;
        double y = playerPos.y() + eyeHeight;
        double z = playerPos.z() - (double) zDir * offsetDistance;
        
        return new Pos(x, y, z);
    }
    
    /**
     * Calculate spawn position with default offset distance and configured eye height.
     * 
     * @param player The player
     * @return The calculated spawn position
     */
    public static Pos calculateSpawnOffset(net.minestom.server.entity.Player player) {
        Pos playerPos = player.getPosition();
        float yaw = playerPos.yaw();
        
        // Use configured eye height instead of hard-coded 1.6
        Pos eyePos = com.minestom.mechanics.systems.gameplay.EyeHeightSystem.getInstance().getEyePosition(player);
        double eyeHeight = eyePos.y() - playerPos.y();
        
        return calculateSpawnOffset(playerPos, yaw, 0.3, eyeHeight);
    }
    
    /**
     * Calculate spawn position with default offset distance and hard-coded eye height.
     * @deprecated Use calculateSpawnOffset(Player) for configured eye heights
     * 
     * @param playerPos The player's position
     * @param yaw The player's yaw angle
     * @return The calculated spawn position
     */
    @Deprecated
    public static Pos calculateSpawnOffset(Pos playerPos, float yaw) {
        return calculateSpawnOffset(playerPos, yaw, 0.3, 1.6);
    }
}

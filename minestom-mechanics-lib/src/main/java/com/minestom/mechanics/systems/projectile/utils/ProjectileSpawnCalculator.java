package com.minestom.mechanics.systems.projectile.utils;

import com.minestom.mechanics.systems.compatibility.hitbox.EyeHeightSystem;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

// TODO: Fix projectile spawn 'direction' (so that the projectile / arrow spawns "looking" the correct direction)

/**
 * Utility class for calculating projectile spawn positions.
 * Consolidates spawn position logic to eliminate code duplication.
 */
public class ProjectileSpawnCalculator {

    // ===========================
    // HIGH-LEVEL SPAWN METHODS
    // ===========================

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

    /**
     * Calculate spawn position for arrows (slightly below eye level).
     * Uses configured eye height from EyeHeightSystem.
     *
     * @param player The shooting player
     * @param heightOffset Offset below eye height (typically 0.1 for arrows)
     * @return The calculated spawn position
     */
    public static Pos calculateArrowSpawnPosition(Player player, double heightOffset) {
        Pos eyePos = EyeHeightSystem.getInstance().getEyePosition(player);
        return eyePos.sub(0, heightOffset, 0);
    }

    /**
     * Calculate spawn position for arrows with default offset.
     *
     * @param player The shooting player
     * @return The calculated spawn position (0.1 blocks below eye level)
     */
    public static Pos calculateArrowSpawnPosition(Player player) {
        return calculateArrowSpawnPosition(player, 0.1);
    }

    // ===========================
    // LOW-LEVEL UTILITIES
    // ===========================

    /**
     * Calculate spawn position offset from player position.
     * Uses trigonometric calculations to position projectile in front of player.
     *
     * @param playerPos The player's position
     * @param yaw The player's yaw angle
     * @param offsetDistance The distance to offset from player (typically 0.3)
     * @param eyeHeight The height offset (player eye height)
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
     * @param offsetDistance Distance in front of player (default: 0.3)
     * @return The calculated spawn position
     */
    public static Pos calculateSpawnOffset(Player player, double offsetDistance) {
        Pos playerPos = player.getPosition();
        float yaw = playerPos.yaw();

        // Use configured eye height from EyeHeightSystem
        double eyeHeight;
        try {
            Pos eyePos = EyeHeightSystem.getInstance().getEyePosition(player);
            eyeHeight = eyePos.y() - playerPos.y();
        } catch (IllegalStateException e) {
            // EyeHeightSystem not initialized, use default
            eyeHeight = 1.62; // Default player eye height
        }

        return calculateSpawnOffset(playerPos, yaw, offsetDistance, eyeHeight);
    }

    /**
     * Calculate spawn position with default offset distance (0.3 blocks).
     *
     * @param player The player
     * @return The calculated spawn position
     */
    public static Pos calculateSpawnOffset(Player player) {
        return calculateSpawnOffset(player, 0.3);
    }

    // ===========================
    // SPECIALIZED SPAWN METHODS
    // ===========================

    /**
     * Calculate spawn position for fishing bobbers.
     * Similar to arrows but with different offset.
     *
     * @param player The fishing player
     * @return The calculated spawn position
     */
    public static Pos calculateFishingBobberSpawnPosition(Player player) {
        return calculateSpawnOffset(player, 0.3);
    }

    /**
     * Calculate spawn position for throwable projectiles (snowballs, eggs, ender pearls).
     * Uses standard eye position with small forward offset.
     *
     * @param player The throwing player
     * @return The calculated spawn position
     */
    public static Pos calculateThrowableSpawnPosition(Player player) {
        return calculateSpawnPosition(player); // Use 1.8-compatible method
    }
}
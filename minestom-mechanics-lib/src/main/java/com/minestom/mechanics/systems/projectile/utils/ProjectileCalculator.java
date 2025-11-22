package com.minestom.mechanics.systems.projectile.utils;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.systems.compatibility.hitbox.EyeHeightSystem;
import com.minestom.mechanics.systems.projectile.components.ProjectileVelocity;
import com.minestom.mechanics.systems.projectile.entities.CustomEntityProjectile;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Unified calculator for ALL projectile types.
 * Handles both velocity calculation (with config resolution, spread, player momentum) 
 * and spawn position calculation.
 *
 * Consolidates all projectile calculation logic to eliminate code duplication.
 */
public class ProjectileCalculator {

    // ===========================
    // HIGH-LEVEL API (Use these for projectile spawning)
    // ===========================

    /**
     * Calculate complete projectile velocity with all features:
     * - Tag-based config resolution
     * - Direction from player aim
     * - Speed and gravity from config
     * - Optional spread
     * - Optional player momentum inheritance
     *
     * This is the primary method for most projectiles (arrows, snowballs, etc.)
     *
     * @param shooter The player shooting
     * @param item The item being used (bow, snowball item, etc.)
     * @param projectile The projectile entity (can be null before spawn)
     * @param baseConfig Default config if no tags present
     * @param power Bow power or throw strength (0.0 to 1.0+)
     * @param inheritMomentum Whether to add player velocity
     * @return Final velocity vector (already in per-tick format)
     */
    public static Vec calculateProjectileVelocity(
            Player shooter,
            ItemStack item,
            Entity projectile,
            ProjectileVelocityConfig baseConfig,
            double power,
            boolean inheritMomentum) {

        // Resolve config through tag system
        ProjectileVelocityConfig config;
        try {
            config = ProjectileVelocity.getInstance()
                    .resolveConfig(shooter, projectile, item);
        } catch (IllegalStateException e) {
            // System not initialized, use base config
            config = baseConfig;
        }

        // Apply resolved config to projectile aerodynamics
        if (projectile instanceof CustomEntityProjectile customProjectile) {
            customProjectile.setAerodynamics(
                    customProjectile.getAerodynamics()
                            .withGravity(config.gravity())
                            .withHorizontalAirResistance(config.horizontalAirResistance())
                            .withVerticalAirResistance(config.verticalAirResistance())
            );
        }

        // Calculate direction from aim
        Vec velocity = calculateDirectionalVelocity(
                shooter.getPosition().pitch(),
                shooter.getPosition().yaw(),
                power
        );

        // Apply config multipliers (horizontal/vertical)
        velocity = applyVelocityConfig(velocity, config);

        // Apply spread if configured
        if (config.spreadMultiplier() > 0) {
            double spread = 0.0075 * config.spreadMultiplier();
            velocity = applySpread(velocity, spread);
        }

        // Add player momentum if enabled
        if (inheritMomentum) {
            velocity = addPlayerMomentum(velocity, shooter);
        }

        // Convert to per-tick velocity
        return toPerTickVelocity(velocity);
    }

    /**
     * Simpler version for projectiles without power mechanics (snowballs, eggs, etc.)
     */
    public static Vec calculateThrowableVelocity(
            Player shooter,
            ItemStack item,
            Entity projectile,
            ProjectileVelocityConfig baseConfig,
            boolean inheritMomentum) {

        return calculateProjectileVelocity(
                shooter, item, projectile, baseConfig,
                1.5, // Standard throw power
                inheritMomentum
        );
    }

    // ===========================
    // CONFIG APPLICATION
    // ===========================

    /**
     * Apply velocity config to a base velocity vector.
     * Applies horizontal/vertical speed multipliers.
     *
     * @param velocity Base velocity vector
     * @param config Velocity config with multipliers
     * @return Modified velocity
     */
    public static Vec applyVelocityConfig(Vec velocity, ProjectileVelocityConfig config) {
        return new Vec(
                velocity.x() * config.horizontalMultiplier(),
                velocity.y() * config.verticalMultiplier(),
                velocity.z() * config.horizontalMultiplier()
        );
    }

    // ===========================
    // CORE CALCULATIONS (Lower-level utilities)
    // ===========================

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
     * Add player momentum to projectile velocity.
     * Uses player's current velocity (only horizontal if on ground).
     *
     * @param velocity Base projectile velocity
     * @param player The shooting player
     * @return Velocity with player momentum added
     */
    public static Vec addPlayerMomentum(Vec velocity, Player player) {
        Vec playerVel = player.getVelocity();

        // Only add vertical momentum if player is in air
        double yMomentum = player.isOnGround() ? 0.0 : playerVel.y();

        return velocity.add(playerVel.x(), yMomentum, playerVel.z());
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

    // ===========================
    // SPAWN POSITION CALCULATIONS
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
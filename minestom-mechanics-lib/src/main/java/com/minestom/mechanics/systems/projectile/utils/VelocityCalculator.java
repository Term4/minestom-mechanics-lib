package com.minestom.mechanics.systems.projectile.utils;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.systems.projectile.components.ProjectileVelocitySystem;
import com.minestom.mechanics.systems.projectile.entities.CustomEntityProjectile;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Unified velocity calculator for ALL projectile types.
 * Handles velocity calculation with config resolution, spread, player momentum, and more.
 *
 * Replaces scattered velocity logic across different projectile features.
 */
public class VelocityCalculator {

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
            config = ProjectileVelocitySystem.getInstance()
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
}
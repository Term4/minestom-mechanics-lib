package com.minestom.mechanics.systems.projectile.components;

import com.minestom.mechanics.config.constants.ProjectileConstants;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.systems.compatibility.hitbox.EyeHeightSystem;
import com.minestom.mechanics.systems.projectile.entities.CustomEntityProjectile;
import com.minestom.mechanics.systems.projectile.utils.ProjectileSpawnCalculator;
import com.minestom.mechanics.systems.projectile.utils.VelocityCalculator;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.systems.projectile.tags.ProjectileTagRegistry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

import java.util.Objects;

/**
 * THE universal projectile creator.
 * Handles spawning for ALL projectile types: arrows, snowballs, eggs, fishing bobbers, etc.
 */
public class ProjectileCreator {
    private static final LogUtil.SystemLogger log = LogUtil.system("ProjectileCreator");

    /**
     * Spawn a throwable projectile with default spawn position calculation
     *
     * @param projectile The projectile entity to spawn
     * @param player The throwing player
     * @param sourceItem The item that created this projectile (for tags)
     * @param velocityConfig The velocity configuration
     */
    public void spawn(CustomEntityProjectile projectile, Player player, ItemStack sourceItem,
                      ProjectileVelocityConfig velocityConfig) {
        Pos spawnPos = ProjectileSpawnCalculator.calculateSpawnPosition(player);
        spawn(projectile, player, sourceItem, velocityConfig, spawnPos);
    }

    /**
     * Spawn a throwable projectile with custom spawn position
     *
     * @param projectile The projectile entity to spawn
     * @param player The throwing player
     * @param sourceItem The item that created this projectile (for tags)
     * @param velocityConfig The velocity configuration
     * @param spawnPos The spawn position
     */
    public void spawn(CustomEntityProjectile projectile, Player player, ItemStack sourceItem,
                      ProjectileVelocityConfig velocityConfig, Pos spawnPos) {
        // 1. Calculate velocity
        Vec velocity = VelocityCalculator.calculateThrowableVelocity(
                player, sourceItem, projectile, velocityConfig, shouldInheritPlayerMomentum()
        );

        // 2. Apply velocity and tags
        projectile.setVelocity(velocity);
        ProjectileTagRegistry.copyAllProjectileTags(sourceItem, projectile);

        // 3. Calculate direction from velocity vector (handles spread/momentum correctly!)
        float[] direction = calculateDirectionFromVelocity(velocity);

        // 4. Spawn with direction matching actual velocity
        projectile.setInstance(Objects.requireNonNull(player.getInstance()),
                spawnPos.withView(direction[0], direction[1])); // [yaw, pitch]

        log.debug("Spawned {} for {} at {} with velocity {}",
                projectile.getEntityType(), player.getUsername(), spawnPos, velocity);
    }

    /**
     * Spawn arrow with bow-specific logic (power multiplier, height offset)
     *
     * @param projectile The arrow entity
     * @param player The shooting player
     * @param bowStack The bow item
     * @param velocityConfig The base velocity config
     * @param power The bow power (0.0 to 1.0)
     */
    public void spawnArrow(CustomEntityProjectile projectile, Player player, ItemStack bowStack,
                           ProjectileVelocityConfig velocityConfig, double power) {
        // Calculate spawn position (arrows spawn slightly below eye)
        Pos eyePos = EyeHeightSystem.getInstance()
                .getEyePosition(player);
        Pos spawnPos = eyePos.add(0D, -ProjectileConstants.ARROW_SPAWN_HEIGHT_OFFSET, 0D);

        // FIX: Use calculateProjectileVelocity with power parameter instead of pre-multiplying!
        Vec velocity = VelocityCalculator.calculateProjectileVelocity(
                player,
                bowStack,
                projectile,
                velocityConfig,
                power,  // Pass power directly - don't pre-multiply config!
                shouldInheritPlayerMomentum()
        );

        // Apply velocity and tags
        projectile.setVelocity(velocity);
        ProjectileTagRegistry.copyAllProjectileTags(bowStack, projectile);

        // FIX: Calculate direction from velocity vector (not player yaw/pitch!)
        float[] direction = calculateDirectionFromVelocity(velocity);

        // Spawn with correct direction
        projectile.setInstance(Objects.requireNonNull(player.getInstance()),
                spawnPos.withView(direction[0], direction[1])); // [yaw, pitch]

        log.debug("Spawned arrow for {} with power {:.2f}, velocity {}",
                player.getUsername(), power, velocity);
    }

    /**
     * Calculate yaw and pitch from a velocity vector.
     * This ensures the projectile's visual rotation matches its actual flight direction.
     *
     * @param velocity The velocity vector
     * @return [yaw, pitch] in degrees
     */
    private float[] calculateDirectionFromVelocity(Vec velocity) {
        // Handle zero velocity edge case
        if (velocity.lengthSquared() < 0.0001) {
            return new float[]{0f, 0f};
        }

        // Calculate horizontal distance (for pitch calculation)
        double horizontalLength = Math.sqrt(velocity.x() * velocity.x() + velocity.z() * velocity.z());

        // Calculate pitch (vertical angle)
        // atan2(opposite, adjacent) = atan2(-y, horizontal_distance)
        float pitch = (float) Math.toDegrees(Math.atan2(-velocity.y(), horizontalLength));

        // Calculate yaw (horizontal angle)
        // atan2(-x, z) gives correct Minecraft yaw direction
        float yaw = (float) Math.toDegrees(Math.atan2(-velocity.x(), velocity.z()));

        return new float[]{yaw, pitch};
    }

    private boolean shouldInheritPlayerMomentum() {
        try {
            return com.minestom.mechanics.manager.ProjectileManager.getInstance()
                    .getProjectileConfig().shouldInheritPlayerMomentum();
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
package com.minestom.mechanics.systems.projectile.components;

import com.minestom.mechanics.config.constants.ProjectileConstants;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.systems.projectile.entities.CustomEntityProjectile;
import com.minestom.mechanics.systems.projectile.features.Bow;
import com.minestom.mechanics.systems.projectile.features.FishingRod;
import com.minestom.mechanics.systems.projectile.utils.ProjectileCalculator;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.systems.projectile.tags.ProjectileTagRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.item.ItemStack;

import java.util.Objects;

/**
 * THE universal projectile creator.
 * Handles spawning for ALL projectile types: arrows, snowballs, eggs, fishing bobbers, etc.
 */
public class ProjectileCreator {
    private static final LogUtil.SystemLogger log = LogUtil.system("ProjectileCreator");

    /**
     * Spawn a projectile with default spawn position and power.
     * Uses default throwable power (1.5) for non-arrow projectiles.
     *
     * @param projectile The projectile entity to spawn
     * @param player The throwing/shooting player
     * @param sourceItem The item that created this projectile (for tags)
     * @param velocityConfig The velocity configuration
     */
    public void spawn(CustomEntityProjectile projectile, Player player, ItemStack sourceItem,
                      ProjectileVelocityConfig velocityConfig) {
        Pos spawnPos = ProjectileCalculator.calculateSpawnPosition(player);
        spawn(projectile, player, sourceItem, velocityConfig, ProjectileConstants.MISC_PROJECTILE_POWER, spawnPos);
    }

    /**
     * Spawn a projectile with custom spawn position and default power.
     * Uses default throwable power (1.5) for non-arrow projectiles.
     *
     * @param projectile The projectile entity to spawn
     * @param player The throwing/shooting player
     * @param sourceItem The item that created this projectile (for tags)
     * @param velocityConfig The velocity configuration
     * @param spawnPos The spawn position
     */
    public void spawn(CustomEntityProjectile projectile, Player player, ItemStack sourceItem,
                      ProjectileVelocityConfig velocityConfig, Pos spawnPos) {
        spawn(projectile, player, sourceItem, velocityConfig, ProjectileConstants.MISC_PROJECTILE_POWER, spawnPos);
    }

    /**
     * Spawn a projectile with full control over power and spawn position.
     * This is the unified spawn method for all projectile types.
     * For arrows, pass power (0.0 to 1.0). For throwables, use MISC_PROJECTILE_POWER (1.5).
     *
     * @param projectile The projectile entity to spawn
     * @param player The throwing/shooting player
     * @param sourceItem The item that created this projectile (for tags)
     * @param velocityConfig The velocity configuration
     * @param power The projectile power (0.0-1.0 for arrows, 1.5 for throwables)
     * @param spawnPos The spawn position
     */
    public void spawn(CustomEntityProjectile projectile, Player player, ItemStack sourceItem,
                      ProjectileVelocityConfig velocityConfig, double power, Pos spawnPos) {
        // 1. Calculate velocity with power
        Vec velocity = ProjectileCalculator.calculateProjectileVelocity(
                player, sourceItem, projectile, velocityConfig, power, shouldInheritPlayerMomentum()
        );

        // 2. Apply velocity and tags
        projectile.setVelocity(velocity);
        ProjectileTagRegistry.copyAllProjectileTags(sourceItem, projectile);

        // 3. Calculate direction from velocity vector (handles spread/momentum correctly!)
        float[] direction = ProjectileCreator.calculateDirectionFromVelocity(velocity);

        // 4. Spawn with direction matching actual velocity
        projectile.setInstance(Objects.requireNonNull(player.getInstance()),
                spawnPos.withView(direction[0], direction[1])); // [yaw, pitch]

        log.debug("Spawned {} for {} at {} with power {:.2f}, velocity {}",
                projectile.getEntityType(), player.getUsername(), spawnPos, power, velocity);
    }

    /**
     * Calculate yaw and pitch from a velocity vector.
     * This ensures the projectile's visual rotation matches its actual flight direction.
     * 
     * This method converts a velocity vector to Minecraft's yaw/pitch coordinate system:
     * - Yaw: horizontal rotation (0° = south, increases counterclockwise)
     * - Pitch: vertical angle (-90° = straight up, 90° = straight down)
     *
     * @param velocity The velocity vector
     * @return [yaw, pitch] in degrees
     */
    public static float[] calculateDirectionFromVelocity(Vec velocity) {
        // Handle zero velocity edge case
        if (velocity.lengthSquared() < 0.0001) {
            return new float[]{0f, 0f};
        }

        // Calculate horizontal distance (for pitch calculation)
        double horizontalLength = Math.sqrt(velocity.x() * velocity.x() + velocity.z() * velocity.z());

        // Calculate pitch (vertical angle)
        // Matches old implementation: atan2(dy, sqrt(dx² + dz²)) where dy = -sin(pitch)
        float pitch = (float) Math.toDegrees(Math.atan2(velocity.y(), horizontalLength));

        // Calculate yaw (horizontal angle)
        // Matches old implementation: atan2(dx, dz) where dx = -sin(yaw)*cos(pitch), dz = cos(yaw)*cos(pitch)
        float yaw = (float) Math.toDegrees(Math.atan2(velocity.x(), velocity.z()));

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

    // ===========================
    // CLEANUP HANDLING
    // ===========================

    /**
     * Register cleanup event listeners for all projectile features.
     * Call this once during server initialization.
     */
    public static void registerCleanupListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();
        
        // Handle player disconnect - cleanup all projectile state
        handler.addListener(PlayerDisconnectEvent.class, event -> {
            Player player = event.getPlayer();
            cleanupAllProjectiles(player);
        });
        
        // Handle player death - only cleanup bow state, fishing rods should persist
        handler.addListener(PlayerDeathEvent.class, event -> {
            Player player = event.getPlayer();
            // Only cleanup bow drawing state on death
            // Fishing rod bobbers should stay in world until timeout
            try {
                if (Bow.getInstance().isDrawingBow(player)) {
                    Bow.getInstance().cleanup(player);
                    log.debug("Cleaned up bow state for {} on death", player.getUsername());
                }
            } catch (Exception e) {
                log.error("Error cleaning up bow for {}: {}", player.getUsername(), e.getMessage());
            }
        });
        
        log.info("Registered projectile cleanup handlers");
    }
    
    /**
     * Clean up all projectile features for a player.
     * 
     * @param player The player to clean up
     */
    private static void cleanupAllProjectiles(Player player) {
        try {
            // Clean up bow drawing state
            if (Bow.getInstance().isDrawingBow(player)) {
                Bow.getInstance().cleanup(player);
                log.debug("Cleaned up bow state for {}", player.getUsername());
            }
            
            // Clean up fishing rod state on disconnect
            if (FishingRod.getInstance().hasActiveBobber(player)) {
                FishingRod.getInstance().cleanup(player);
                log.debug("Cleaned up fishing rod state for {}", player.getUsername());
            }
            
            // Clean up misc projectile state (if any)
            
        } catch (Exception e) {
            log.error("Error cleaning up projectiles for {}: {}", player.getUsername(), e.getMessage());
        }
    }
}
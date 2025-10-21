package com.minestom.mechanics.projectile;

import com.minestom.mechanics.config.constants.ProjectileConstants;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.projectile.entities.CustomEntityProjectile;
import com.minestom.mechanics.projectile.utils.ProjectileSpawnCalculator;
import com.minestom.mechanics.projectile.utils.VelocityCalculator;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.ProjectileTagRegistry;
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
     * Spawn a projectile with default spawn position calculation
     *
     * @param projectile The projectile entity to spawn
     * @param player The shooting player
     * @param sourceItem The item that created this projectile (for tags)
     * @param velocityConfig The velocity configuration
     */
    public void spawn(CustomEntityProjectile projectile, Player player, ItemStack sourceItem,
                      ProjectileVelocityConfig velocityConfig) {
        Pos spawnPos = ProjectileSpawnCalculator.calculateSpawnPosition(player);
        spawn(projectile, player, sourceItem, velocityConfig, spawnPos);
    }

    /**
     * Spawn a projectile with custom spawn position (for arrows with offset)
     *
     * @param projectile The projectile entity to spawn
     * @param player The shooting player
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

        // 2. Apply velocity, tags, and spawn
        projectile.setVelocity(velocity);
        ProjectileTagRegistry.copyAllProjectileTags(sourceItem, projectile);

        // 3. Spawn with PLAYER's view direction (fixes arrow direction bug!)
        Pos playerPos = player.getPosition();
        projectile.setInstance(Objects.requireNonNull(player.getInstance()),
                spawnPos.withView(playerPos.yaw(), playerPos.pitch()));

        log.debug("Spawned {} for {} at {}", projectile.getEntityType(), player.getUsername(), spawnPos);
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
        Pos eyePos = com.minestom.mechanics.systems.gameplay.EyeHeightSystem.getInstance()
                .getEyePosition(player);
        Pos spawnPos = eyePos.add(0D, -ProjectileConstants.ARROW_SPAWN_HEIGHT_OFFSET, 0D);

        // Adjust config for bow power
        ProjectileVelocityConfig adjustedConfig = new ProjectileVelocityConfig(
                velocityConfig.horizontalMultiplier() * power,
                velocityConfig.verticalMultiplier() * power,
                velocityConfig.spreadMultiplier(),
                velocityConfig.gravity(),
                velocityConfig.horizontalAirResistance(),
                velocityConfig.verticalAirResistance()
        );

        // Spawn using normal flow
        spawn(projectile, player, bowStack, adjustedConfig, spawnPos);
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
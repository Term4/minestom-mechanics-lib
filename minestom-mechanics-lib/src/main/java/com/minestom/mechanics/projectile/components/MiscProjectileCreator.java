package com.minestom.mechanics.projectile.components;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityPresets;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.ProjectileTagRegistry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import com.minestom.mechanics.projectile.entities.CustomEntityProjectile;
import com.minestom.mechanics.projectile.entities.ItemHoldingProjectile;
import com.minestom.mechanics.projectile.entities.Snowball;
import com.minestom.mechanics.projectile.entities.ThrownEnderpearl;
import com.minestom.mechanics.projectile.entities.ThrownEgg;

import java.util.Objects;

// TODO: See THIS should be what we use for all projectiles.
//  Maybe we wanna change the spawn location and stuff on a per
//  projectile basis, but we could just pass any offsets or such to this method
//  when it's called

/**
 * Creates miscellaneous projectiles (snowballs, eggs, ender pearls).
 * Handles projectile creation and spawning logic.
 */
public class MiscProjectileCreator {
    private static final LogUtil.SystemLogger log = LogUtil.system("MiscProjectileCreator");

    /**
     * Create and spawn a miscellaneous projectile
     * @param player The throwing player
     * @param stack The item stack being thrown
     * @param hand The hand used to throw
     */
    public void createAndSpawnProjectile(Player player, ItemStack stack, PlayerHand hand) {
        Material material = stack.material();

        // Create projectile
        CustomEntityProjectile projectile = createProjectile(material, player);

        // Set item appearance
        ((ItemHoldingProjectile) projectile).setItem(stack);

        // Calculate spawn position using configured eye height
        Pos playerPos = player.getPosition();
        Pos eyePos = com.minestom.mechanics.systems.gameplay.EyeHeightSystem.getInstance().getEyePosition(player);
        Pos spawnPos = eyePos;

        // Get velocity config for this projectile type
        var velocityConfig = getVelocityConfigForMaterial(material);

        // Set velocity with config values
        projectile.shootFromRotation(playerPos.pitch(), playerPos.yaw(), 0f,
                velocityConfig.horizontalMultiplier(), velocityConfig.spreadMultiplier());

        // Set custom gravity from config
        projectile.setAerodynamics(projectile.getAerodynamics().withGravity(velocityConfig.gravity()));

        // Add player momentum if configured (modern feature, disabled by default for legacy 1.8)
        if (shouldInheritPlayerMomentum()) {
            Vec playerVel = player.getVelocity();
            projectile.setVelocity(projectile.getVelocity().add(playerVel.x(),
                    player.isOnGround() ? 0.0D : playerVel.y(), playerVel.z()));
        }

        ProjectileTagRegistry.copyAllProjectileTags(stack, projectile);

        // Spawn projectile with view direction copied from projectile entity (fixes wrong direction)
        projectile.setInstance(Objects.requireNonNull(player.getInstance()),
                spawnPos.withView(projectile.getPosition()));

        // Consume item
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setItemInHand(hand, stack.withAmount(stack.amount() - 1));
        }

        log.debug("Created and spawned {} projectile for {} with velocity config", material, player.getUsername());
    }

    private ProjectileVelocityConfig getVelocityConfigForMaterial(Material material) {
        try {
            var manager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
            var config = manager.getProjectileConfig();

            if (material == Material.SNOWBALL) {
                return config.getSnowballVelocityConfig();
            } else if (material == Material.EGG) {
                return config.getEggVelocityConfig();
            } else if (material == Material.ENDER_PEARL) {
                return config.getEnderPearlVelocityConfig();
            } else {
                return ProjectileVelocityPresets.SNOWBALL;
            }
        } catch (IllegalStateException e) {
            // ProjectileManager not initialized, return default based on material
            if (material == Material.SNOWBALL) {
                return ProjectileVelocityPresets.SNOWBALL;
            } else if (material == Material.EGG) {
                return ProjectileVelocityPresets.EGG;
            } else if (material == Material.ENDER_PEARL) {
                return ProjectileVelocityPresets.ENDER_PEARL;
            } else {
                return ProjectileVelocityPresets.SNOWBALL;
            }
        }
    }
    
    /**
     * Create a projectile based on material
     * @param material The material being thrown
     * @param player The throwing player
     * @return The created projectile
     */
    private CustomEntityProjectile createProjectile(Material material, Player player) {
        CustomEntityProjectile projectile;

        if (material == Material.SNOWBALL) {
            projectile = new Snowball(player);
            projectile.setUseKnockbackHandler(true);
        } else if (material == Material.ENDER_PEARL) {
            projectile = new ThrownEnderpearl(player);
            // Ender pearl has knockback disabled by default
        } else if (material == Material.EGG) {
            projectile = new ThrownEgg(player);
            projectile.setUseKnockbackHandler(true);
        } else {
            throw new IllegalArgumentException("Unsupported projectile material: " + material);
        }
        
        return projectile;
    }

    // TODO: Duplicate method, is in projectile config
    private boolean shouldInheritPlayerMomentum() {
        try {
            return com.minestom.mechanics.manager.ProjectileManager.getInstance()
                .getProjectileConfig().shouldInheritPlayerMomentum();
        } catch (IllegalStateException e) {
            // TODO: Should probably set default to modern ngl
            return false; // Default to false for legacy 1.8 compatibility
        }
    }
}

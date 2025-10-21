package com.minestom.mechanics.projectile.components;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackPresets;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityPresets;
import com.minestom.mechanics.projectile.entities.*;
import com.minestom.mechanics.projectile.utils.ProjectileSpawnCalculator;
import com.minestom.mechanics.projectile.utils.VelocityCalculator;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.ProjectileTagRegistry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.Objects;

public class MiscProjectileCreator {
    private static final LogUtil.SystemLogger log = LogUtil.system("MiscProjectileCreator");

    /**
     * Creates and spawns a misc projectile (snowball, egg, ender pearl)
     */
    public void createProjectile(Player player, ItemStack stack, PlayerHand hand) {
        Material material = stack.material();

        // Validate material
        if (!isValidProjectileMaterial(material)) {
            throw new IllegalArgumentException("Unsupported projectile material: " + material);
        }

        // 1. Create projectile entity
        CustomEntityProjectile projectile = createProjectileEntity(material, player);

        // 2. Configure projectile knockback
        configureProjectile(projectile, material, stack);

        // 3. Calculate spawn position
        Pos spawnPos = ProjectileSpawnCalculator.calculateSpawnPosition(player);

        // 4. Calculate velocity
        Vec velocity = VelocityCalculator.calculateThrowableVelocity(
                player, stack, projectile, getVelocityConfigForMaterial(material), shouldInheritPlayerMomentum()
        );

        // 5. Apply and spawn (no need for separate spawner class)
        projectile.setVelocity(velocity);
        ProjectileTagRegistry.copyAllProjectileTags(stack, projectile);
        projectile.setInstance(Objects.requireNonNull(player.getInstance()),
                spawnPos.withView(projectile.getPosition()));

        // 6. Consume item
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setItemInHand(hand, stack.withAmount(stack.amount() - 1));
        }

        log.debug("Created {} projectile for {}", material, player.getUsername());
    }

    /**
     * Validates if material is a supported projectile type
     */
    private boolean isValidProjectileMaterial(Material material) {
        return material == Material.SNOWBALL ||
                material == Material.EGG ||
                material == Material.ENDER_PEARL;
    }

    /**
     * Creates the appropriate projectile entity based on material
     */
    private CustomEntityProjectile createProjectileEntity(Material material, Player player) {
        if (material == Material.SNOWBALL) {
            Snowball snowball = new Snowball(player);
            snowball.setUseKnockbackHandler(true);
            return snowball;
        } else if (material == Material.EGG) {
            ThrownEgg egg = new ThrownEgg(player);
            egg.setUseKnockbackHandler(true);
            return egg;
        } else if (material == Material.ENDER_PEARL) {
            return new ThrownEnderpearl(player);
        } else {
            throw new IllegalArgumentException("Unsupported projectile material: " + material);
        }
    }

    /**
     * Configures projectile with knockback settings
     */
    private void configureProjectile(CustomEntityProjectile projectile, Material material, ItemStack stack) {
        // Get knockback config
        ProjectileKnockbackConfig knockbackConfig = getKnockbackConfigForMaterial(material);

        // Apply to projectile if it supports knockback configuration
        if (projectile instanceof Snowball snowball) {
            snowball.setKnockbackConfig(knockbackConfig);
        } else if (projectile instanceof ThrownEgg egg) {
            egg.setKnockbackConfig(knockbackConfig);
        } else if (projectile instanceof ThrownEnderpearl pearl) {
            pearl.setKnockbackConfig(knockbackConfig);
        }
    }

    /**
     * Gets velocity config for a specific material
     */
    private ProjectileVelocityConfig getVelocityConfigForMaterial(Material material) {
        try {
            var config = com.minestom.mechanics.manager.ProjectileManager.getInstance().getProjectileConfig();
            if (material == Material.SNOWBALL) return config.getSnowballVelocityConfig();
            if (material == Material.EGG) return config.getEggVelocityConfig();
            if (material == Material.ENDER_PEARL) return config.getEnderPearlVelocityConfig();
            return ProjectileVelocityPresets.SNOWBALL;
        } catch (IllegalStateException e) {
            if (material == Material.SNOWBALL) return ProjectileVelocityPresets.SNOWBALL;
            if (material == Material.EGG) return ProjectileVelocityPresets.EGG;
            if (material == Material.ENDER_PEARL) return ProjectileVelocityPresets.ENDER_PEARL;
            return ProjectileVelocityPresets.SNOWBALL;
        }
    }

    /**
     * Gets knockback config for a specific material
     */
    private ProjectileKnockbackConfig getKnockbackConfigForMaterial(Material material) {
        try {
            var config = com.minestom.mechanics.manager.ProjectileManager.getInstance().getProjectileConfig();
            if (material == Material.SNOWBALL) return config.getSnowballKnockbackConfig();
            if (material == Material.EGG) return config.getEggKnockbackConfig();
            if (material == Material.ENDER_PEARL) return config.getEnderPearlKnockbackConfig();
            return ProjectileKnockbackPresets.SNOWBALL;
        } catch (IllegalStateException e) {
            if (material == Material.SNOWBALL) return ProjectileKnockbackPresets.SNOWBALL;
            if (material == Material.EGG) return ProjectileKnockbackPresets.EGG;
            if (material == Material.ENDER_PEARL) return ProjectileKnockbackPresets.ENDER_PEARL;
            return ProjectileKnockbackPresets.SNOWBALL;
        }
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
package com.minestom.mechanics.projectile.components;

import com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig;
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
        Pos eyePos = com.minestom.mechanics.features.gameplay.EyeHeightSystem.getInstance().getEyePosition(player);
        Pos spawnPos = eyePos;
        
        // Set velocity - this calculates the correct view direction
        projectile.shootFromRotation(playerPos.pitch(), playerPos.yaw(), 0f, 
            com.minestom.mechanics.projectile.ProjectileConstants.MISC_PROJECTILE_POWER, 1.0);
        
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
        
        log.debug("Created and spawned {} projectile for {}", material, player.getUsername());
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
            // Configure snowball knockback
            ((Snowball) projectile).setUseKnockbackHandler(true);
            ((Snowball) projectile).setKnockbackConfig(getSnowballKnockbackConfig());
        } else if (material == Material.ENDER_PEARL) {
            projectile = new ThrownEnderpearl(player);
            // Ender pearl has knockback disabled by default (useKnockbackHandler = false)
        } else if (material == Material.EGG) {
            projectile = new ThrownEgg(player);
            // Configure egg knockback
            ((ThrownEgg) projectile).setUseKnockbackHandler(true);
            ((ThrownEgg) projectile).setKnockbackConfig(getEggKnockbackConfig());
        } else {
            throw new IllegalArgumentException("Unsupported projectile material: " + material);
        }
        
        return projectile;
    }
    
    // ===========================
    // CONFIGURATION HELPERS
    // ===========================
    
    private ProjectileKnockbackConfig getSnowballKnockbackConfig() {
        try {
            return com.minestom.mechanics.manager.ProjectileManager.getInstance().getSnowballKnockbackConfig();
        } catch (IllegalStateException e) {
            return ProjectileKnockbackConfig.defaultSnowballKnockback();
        }
    }
    
    private ProjectileKnockbackConfig getEggKnockbackConfig() {
        try {
            return com.minestom.mechanics.manager.ProjectileManager.getInstance().getEggKnockbackConfig();
        } catch (IllegalStateException e) {
            return ProjectileKnockbackConfig.defaultEggKnockback();
        }
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

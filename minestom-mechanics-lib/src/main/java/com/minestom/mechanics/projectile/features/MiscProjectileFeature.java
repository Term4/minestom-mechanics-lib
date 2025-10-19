package com.minestom.mechanics.projectile.features;

import com.minestom.mechanics.projectile.components.*;
import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

// TODO: Ensure no repeated methods / unnecessary calls or methods

/**
 * Refactored MiscProjectileFeature using focused components.
 * Orchestrates miscellaneous projectile functionality through specialized components.
 */
public class MiscProjectileFeature extends InitializableSystem implements ProjectileFeature {
    
    private static MiscProjectileFeature instance;
    
    // Component references
    private final MiscProjectileCreator projectileCreator;
    private final ProjectileSoundHandler soundHandler;
    
    private MiscProjectileFeature() {
        // Initialize components
        this.projectileCreator = new MiscProjectileCreator();
        this.soundHandler = new ProjectileSoundHandler();
    }
    
    public static MiscProjectileFeature initialize() {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("MiscProjectileFeature");
            return instance;
        }
        
        instance = new MiscProjectileFeature();
        instance.registerListeners();
        instance.markInitialized();
        
        LogUtil.logInit("MiscProjectileFeature");
        return instance;
    }
    
    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();
        
        handler.addListener(PlayerUseItemEvent.class, event -> {
            Material material = event.getItemStack().material();
            
            if (material != Material.SNOWBALL && material != Material.EGG
                    && material != Material.ENDER_PEARL) {
                return;
            }
            
            handleThrowProjectile(event.getPlayer(), event.getItemStack(), event.getHand());
        });
    }
    
    private void handleThrowProjectile(Player player, ItemStack stack, PlayerHand hand) {
        Material material = stack.material();
        
        // Play sound
        soundHandler.playThrowSound(player, material);
        
        // Create and spawn projectile
        projectileCreator.createAndSpawnProjectile(player, stack, hand);
    }
    
    // ===========================
    // PUBLIC API
    // ===========================
    
    public static MiscProjectileFeature getInstance() {
        return requireInstance(instance, "MiscProjectileFeature");
    }
}

package com.minestom.mechanics.events;

import com.minestom.mechanics.projectile.features.BowFeature;
import com.minestom.mechanics.projectile.features.FishingRodFeature;
import com.minestom.mechanics.projectile.features.MiscProjectileFeature;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;

// TODO: Probably good. Maybe could / should make an abstract cleanup handler? But
//  to be honest, most systems / features do clean up in house anyways.
//  Two options: 1. move projectile cleanup in house, or 2: Move cleanup for the other systems out.
//  Unsure of which would be more efficient / better, but I do know which one is easier...

/**
 * Centralized handler for projectile cleanup events.
 * Manages cleanup for all projectile features when players disconnect or die.
 */
public class ProjectileCleanupHandler {
    private static final LogUtil.SystemLogger log = LogUtil.system("ProjectileCleanupHandler");
    
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
                if (BowFeature.getInstance().isDrawingBow(player)) {
                    BowFeature.getInstance().cleanup(player);
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
            if (BowFeature.getInstance().isDrawingBow(player)) {
                BowFeature.getInstance().cleanup(player);
                log.debug("Cleaned up bow state for {}", player.getUsername());
            }
            
            // Clean up fishing rod state on disconnect
            if (FishingRodFeature.getInstance().hasActiveBobber(player)) {
                FishingRodFeature.getInstance().cleanup(player);
                log.debug("Cleaned up fishing rod state for {}", player.getUsername());
            }
            
            // Clean up misc projectile state (if any)
            
        } catch (Exception e) {
            log.error("Error cleaning up projectiles for {}: {}", player.getUsername(), e.getMessage());
        }
    }
}

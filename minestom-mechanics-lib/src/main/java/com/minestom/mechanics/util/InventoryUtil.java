package com.minestom.mechanics.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

// TODO: I like this a lot, we should make it more obvious why this is necessary though,
//  Commonly used to fix client desync for legacy (1.8) clients (update javadocs)

/**
 * Utility class for inventory operations.
 * Provides common inventory management functions for the combat system.
 */
public class InventoryUtil {
    
    /**
     * Force a complete inventory sync for a player.
     * Useful for fixing client-side rendering issues, especially with 1.8 clients.
     * 
     * This method resends all inventory slots to ensure the client has the correct state.
     * Particularly useful for fixing invisibility bugs in legacy clients.
     * 
     * @param player The player to sync inventory for
     */
    public static void forceInventorySync(Player player) {
        player.getInventory().update();
    }
    
    /**
     * Force inventory sync with a delay.
     * Useful when the sync needs to happen after other operations complete.
     * 
     * @param player The player to sync inventory for
     * @param delayTicks The delay in ticks before syncing
     */
    public static void forceInventorySyncDelayed(Player player, int delayTicks) {
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            forceInventorySync(player);
        }).delay(net.minestom.server.timer.TaskSchedule.tick(delayTicks)).schedule();
    }
}

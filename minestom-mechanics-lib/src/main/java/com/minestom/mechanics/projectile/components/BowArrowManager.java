package com.minestom.mechanics.projectile.components;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import com.minestom.mechanics.projectile.entities.Arrow;

/**
 * Manages arrow inventory and consumption for bow shooting.
 * Handles finding arrows in player inventory and consuming them.
 */
@Deprecated
public class BowArrowManager {
    private static final LogUtil.SystemLogger log = LogUtil.system("BowArrowManager");

    // TODO: Maybe generalize this for all projectiles
    //  (they all need to be in the inventory, all get removed on use,
    //  unless creative mode) ALSO bows can be used in creative mode
    //  even without arrows in the inventory
    /**
     * Find an arrow in the player's inventory
     * @param player The player to search
     * @return ArrowResult with stack and slot, or null if no arrow found
     */
    public ArrowResult getArrowFromInventory(Player player) {
        // Check offhand first
        ItemStack offhand = player.getItemInOffHand();
        if (isArrow(offhand)) {
            return new ArrowResult(offhand, 40);
        }
        
        // Check main hand
        ItemStack mainHand = player.getItemInMainHand();
        if (isArrow(mainHand)) {
            return new ArrowResult(mainHand, player.getHeldSlot());
        }
        
        // Search inventory
        ItemStack[] items = player.getInventory().getItemStacks();
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item != null && !item.isAir() && isArrow(item)) {
                return new ArrowResult(item, i);
            }
        }
        
        // Creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return new ArrowResult(Arrow.DEFAULT_ARROW, -1);
        }
        
        return null;
    }
    
    /**
     * Check if an item stack is an arrow
     */
    public boolean isArrow(ItemStack stack) {
        if (stack == null || stack.isAir()) return false;
        return stack.material() == Material.ARROW;
    }

    // TODO: Same here, could be generalized
    /**
     * Consume an arrow from the player's inventory
     * @param player The player
     * @param arrowResult The arrow to consume
     * @param isInfinite Whether the bow has infinity enchantment
     */
    public void consumeArrow(Player player, ArrowResult arrowResult, boolean isInfinite) {
        if (isInfinite && player.getGameMode() != GameMode.CREATIVE) {
            // Infinity bow - don't consume arrow
            return;
        }
        
        if (player.getGameMode() == GameMode.CREATIVE) {
            // Creative mode - don't consume
            return;
        }
        
        if (arrowResult.slot >= 0) {
            ItemStack newStack = arrowResult.stack.withAmount(arrowResult.stack.amount() - 1);
            player.getInventory().setItemStack(arrowResult.slot, newStack);
            log.debug("Consumed arrow from slot {} for {}", arrowResult.slot, player.getUsername());
        }
    }
    
    /**
     * Result of arrow search in inventory
     * 
     * @param stack the arrow item stack found
     * @param slot the inventory slot where the arrow was found
     */
    public record ArrowResult(ItemStack stack, int slot) {}
}

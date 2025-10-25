package com.minestom.mechanics.systems.projectile.entities;

import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

// TODO: Ensure this is necessary

/**
 * Interface for projectiles that display an item
 * (snowballs, eggs, ender pearls)
 */
public interface ItemHoldingProjectile {

    /**
     * Set the item this projectile should display
     */
    void setItem(@NotNull ItemStack item);
}

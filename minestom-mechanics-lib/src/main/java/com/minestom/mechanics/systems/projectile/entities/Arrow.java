package com.minestom.mechanics.systems.projectile.entities;

// TODO: Make sure this is working properly with any updates to abstract arrow

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

/**
 * Regular arrow projectile - 1.8 only (no tipped/spectral arrows)
 */
public class Arrow extends AbstractArrow {
	public static final ItemStack DEFAULT_ARROW = ItemStack.of(Material.ARROW);

	public Arrow(@Nullable Entity shooter) {
		super(shooter, EntityType.ARROW);
	}

	@Override
	protected ItemStack getPickupItem() {
		return DEFAULT_ARROW;
	}
}

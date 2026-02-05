package com.minestom.mechanics.systems.blocking;

import com.minestom.mechanics.systems.blocking.tags.BlockableTagValue;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.BlocksAttacks;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Helper to apply the mechanics-lib blockable tag and vanilla BLOCKS_ATTACKS to items.
 * Use this so items work with the blocking system and with vanilla blocking behavior.
 * <p>
 * When {@link #registerAutoPresetMaterials(Material...)} is used (e.g. by BlockingSystem),
 * any item with one of those materials set into a player inventory is automatically given
 * the sword preset (blockable tag + BLOCKS_ATTACKS), so you can use {@code ItemStack.of(Material.DIAMOND_SWORD)}
 * and it will work as blockable.
 */
public final class BlockableItem {

    private BlockableItem() {}

    /** Materials that get the sword preset applied automatically when set in a player inventory. */
    private static final Set<Material> AUTO_PRESET_MATERIALS = new CopyOnWriteArraySet<>(Set.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
    ));

    /**
     * Register materials that should automatically receive the sword preset when set in a player inventory.
     * When blocking is enabled and an item with one of these materials is put in a player inventory
     * (without already having the blockable tag), it is replaced with the blockable version.
     * Call from {@link BlockingSystem} init. Default already includes all sword materials.
     */
    public static void registerAutoPresetMaterials(Material... materials) {
        if (materials != null) {
            for (Material m : materials) {
                if (m != null) AUTO_PRESET_MATERIALS.add(m);
            }
        }
    }

    /**
     * True if this item's material is one that gets the sword preset applied automatically.
     */
    public static boolean isAutoPresetMaterial(ItemStack stack) {
        return stack != null && !stack.isAir() && AUTO_PRESET_MATERIALS.contains(stack.material());
    }

    /**
     * True if the material gets the sword preset applied automatically (swords, etc.).
     * Legacy blocking slowdown is controlled by {@link BlockableTagValue#applyLegacySlowdown()} on the item tag, not by material.
     */
    public static boolean isSwordMaterial(Material material) {
        return material != null && AUTO_PRESET_MATERIALS.contains(material);
    }

    /**
     * If the stack is an auto-preset material and has no blockable tag, return the blockable version; else return the stack.
     */
    public static ItemStack applyPresetIfNeeded(ItemStack stack) {
        if (stack == null || stack.isAir()) return stack;
        if (!AUTO_PRESET_MATERIALS.contains(stack.material())) return stack;
        if (stack.getTag(BlockingSystem.BLOCKABLE) != null) return stack;
        return withSwordPreset(stack);
    }

    private static final BlocksAttacks VANILLA_BLOCKS_ATTACKS = new BlocksAttacks(
            0, 0, List.of(),
            new BlocksAttacks.ItemDamageFunction(0, 0, 0),
            null, null, null
    );

    /**
     * Apply blockable tag and BLOCKS_ATTACKS to an item. The item can then be used to block
     * (right-click use) and will use the given modifiers, or config defaults when null.
     */
    public static ItemStack withBlockable(ItemStack stack, BlockableTagValue value) {
        if (stack == null || stack.isAir()) return stack;
        return stack
                .withTag(BlockingSystem.BLOCKABLE, value)
                .with(DataComponents.BLOCKS_ATTACKS, VANILLA_BLOCKS_ATTACKS);
    }

    /**
     * Sword/PvP preset: typical blocking (50% damage reduction, 40% knockback).
     * Use with {@link #withBlockable(ItemStack, BlockableTagValue)} or {@link #withSwordPreset(Material)}.
     */
    public static BlockableTagValue swordPreset() {
        return BlockableTagValue.SWORD_PRESET;
    }

    /**
     * Create a blockable item from a material using the sword preset. The preset adds the blockable tag
     * and BLOCKS_ATTACKS so the item can be used to block (right-click) with default PvP modifiers.
     * Usage: {@code BlockableItem.withSwordPreset(Material.DIAMOND_SWORD)}
     */
    public static ItemStack withSwordPreset(Material material) {
        return withBlockable(ItemStack.of(material), swordPreset());
    }

    /**
     * Apply sword preset to an existing item (adds blockable tag and BLOCKS_ATTACKS).
     */
    public static ItemStack withSwordPreset(ItemStack stack) {
        return withBlockable(stack, swordPreset());
    }
}

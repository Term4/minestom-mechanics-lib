package com.minestom.mechanics.projectile.utils;

import net.minestom.server.item.Material;

import java.util.Set;

/**
 * Centralized material checking for projectiles.
 * Makes it easy to check what type of projectile an item is.
 */
public class ProjectileMaterials {

    // Throwable projectiles (snowballs, eggs, ender pearls)
    public static final Set<Material> THROWABLES = Set.of(
            Material.SNOWBALL,
            Material.EGG,
            Material.ENDER_PEARL
    );

    // Bow projectiles
    public static final Set<Material> BOWS = Set.of(
            Material.BOW
    );

    // Arrow materials
    public static final Set<Material> ARROWS = Set.of(
            Material.ARROW,
            Material.SPECTRAL_ARROW,
            Material.TIPPED_ARROW
    );

    // Fishing rods
    public static final Set<Material> FISHING_RODS = Set.of(
            Material.FISHING_ROD
    );

    // All projectile launchers (items that shoot projectiles)
    public static final Set<Material> ALL_LAUNCHERS = Set.copyOf(
            new java.util.HashSet<>() {{
                addAll(BOWS);
                addAll(FISHING_RODS);
                addAll(THROWABLES);
            }}
    );

    /**
     * Check if a material is a throwable projectile
     */
    public static boolean isThrowable(Material material) {
        return THROWABLES.contains(material);
    }

    /**
     * Check if a material is a bow
     */
    public static boolean isBow(Material material) {
        return BOWS.contains(material);
    }

    /**
     * Check if a material is an arrow
     */
    public static boolean isArrow(Material material) {
        return ARROWS.contains(material);
    }

    /**
     * Check if a material is a fishing rod
     */
    public static boolean isFishingRod(Material material) {
        return FISHING_RODS.contains(material);
    }

    /**
     * Check if a material launches any type of projectile
     */
    public static boolean isProjectileLauncher(Material material) {
        return ALL_LAUNCHERS.contains(material);
    }

    private ProjectileMaterials() {
        // Prevent instantiation
    }
}
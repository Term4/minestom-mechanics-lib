package com.test.minestom;

import com.minestom.mechanics.systems.knockback.KnockbackSystem;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.component.DataComponents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

/**
 * Test items for Universal Config System
 */
public class TestItems {

    /**
     * Knockback Stick - 2x horizontal
     */
    public static ItemStack knockbackStick() {
        return ItemStack.builder(Material.STICK)
                .set(DataComponents.CUSTOM_NAME, Component.text("Knockback Stick", NamedTextColor.GOLD, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("2x Horizontal Knockback", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.MULTIPLIER, List.of(
                        5.0,  // horizontal × 2
                        1.0,  // vertical × 1 (no change)
                        1.0, 1.0, 1.0, 1.0 // others no change
                ));
    }

    /**
     * Knockback Egg - adds +0.5 horizontal, +0.3 vertical
     */
    public static ItemStack knockbackEgg() {
        return ItemStack.builder(Material.EGG)
                .set(DataComponents.CUSTOM_NAME, Component.text("Knockback Egg", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("+0.5 Horizontal, +0.3 Vertical", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_MODIFY, List.of(
                        100.0,  // horizontal × 2
                        1.0,  // vertical × 1 (no change)
                        0.0, 0.0, 0.0, 0.0 // others no change
                ));
    }

    /**
     * Grapple Snowball
     */
    public static ItemStack GrappleKnockbackSnowball() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Grapple Snowball", NamedTextColor.WHITE, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("Pull your enemies towards you!", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_MULTIPLIER, List.of(
                        -1.5, 1.0, 1.0, 1.0, 1.0, 1.0  // All components × 0
                ));
    }

    /**
     * Sky Snowball
     */
    public static ItemStack noHKnockbackSnowball() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Sky ball", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("Shoot your enemies to the sky!", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_MULTIPLIER, List.of(
                        0.0, 5.0, 1.0, 1.0, 1.0, 1.0  // All components × 0
                ));
    }

    /**
     * Cannon Bow - 10x horizontal, 1.5x vertical
     */
    public static ItemStack cannonBow() {
        return ItemStack.builder(Material.BOW)
                .set(DataComponents.CUSTOM_NAME, Component.text("Cannon Bow", NamedTextColor.RED, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("5x Horizontal, 3x Vertical", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_MULTIPLIER, List.of(
                        5.0,  // horizontal × 5
                        3.0,   // vertical × 1.5
                        1.0,  // sprint bonus horizontal × 10 (scales with base)
                        1.0,   // sprint bonus vertical × 1.5 (scales with base)
                        1.0,   // air multiplier horizontal (NO CHANGE - already a multiplier!)
                        1.0    // air multiplier vertical (NO CHANGE - already a multiplier!)
                ));
    }
}
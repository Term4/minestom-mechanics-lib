package com.test.minestom;

import com.minestom.mechanics.features.knockback.KnockbackSystem;
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
                        5.0,  // horizontal × 2
                        1.0,  // vertical × 1 (no change)
                        1.0, 1.0, 1.0, 1.0 // others no change
                ));
    }

    /**
     * Feather Snowball - NO knockback
     */
    public static ItemStack noKnockbackSnowball() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Feather Snowball", NamedTextColor.WHITE, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("No Knockback", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_MULTIPLIER, List.of(
                        -1.0, 1.0, 1.0, 1.0, 1.0, 1.0  // All components × 0
                ));
    }

    /**
     * Cannon Bow - 10x horizontal, 1.5x vertical
     */
    public static ItemStack cannonBow() {
        return ItemStack.builder(Material.BOW)
                .set(DataComponents.CUSTOM_NAME, Component.text("Cannon Bow", NamedTextColor.RED, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("10x Horizontal, 1.5x Vertical", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_MULTIPLIER, List.of(
                        10.0,  // horizontal × 10
                        1.5,   // vertical × 1.5
                        10.0,  // sprint bonus horizontal × 10 (scales with base)
                        1.5,   // sprint bonus vertical × 1.5 (scales with base)
                        1.0,   // air multiplier horizontal (NO CHANGE - already a multiplier!)
                        1.0    // air multiplier vertical (NO CHANGE - already a multiplier!)
                ));
    }
}
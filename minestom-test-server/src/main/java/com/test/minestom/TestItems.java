package com.test.minestom;

import com.minestom.mechanics.systems.knockback.KnockbackSystem;
import com.minestom.mechanics.systems.projectile.components.ProjectileVelocitySystem;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.component.DataComponents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

import static com.minestom.mechanics.systems.knockback.tags.KnockbackTagValue.*;
import static com.minestom.mechanics.systems.projectile.tags.VelocityTagValue.*;


/**
 * Test items for Universal Config System - Using prefixed methods!
 */
public class TestItems {

    /**
     * Knockback Stick - 5x horizontal knockback
     */
    public static ItemStack knockbackStick() {
        return ItemStack.builder(Material.STICK)
                .set(DataComponents.CUSTOM_NAME, Component.text("Knockback Stick", NamedTextColor.GOLD, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("5x Horizontal Knockback", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.CUSTOM, kbMult(5.0, 2.0));
    }

    /**
     * Knockback Egg - adds +100 horizontal, +1 vertical
     */
    public static ItemStack knockbackEgg() {
        return ItemStack.builder(Material.EGG)
                .set(DataComponents.CUSTOM_NAME, Component.text("Knockback Egg", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("+100 Horizontal, +1 Vertical", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_CUSTOM, kbAdd(4.0, 3.0));
    }

    /**
     * Knockback Egg - adds +100 horizontal, +1 vertical
     */
    public static ItemStack slowGrappleEgg() {
        return ItemStack.builder(Material.EGG)
                .set(DataComponents.CUSTOM_NAME, Component.text("Lazy Grapple Egg", NamedTextColor.DARK_RED, TextDecoration.BOLD, TextDecoration.ITALIC))
                .set(DataComponents.LORE, List.of(
                        Component.text("Feeling lazy?", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_CUSTOM, kbMult(-3, 1.0))
                .withTag(ProjectileVelocitySystem.CUSTOM,
                        velMult(0.0, 0.0, 0.0, 0, 1.0, 1.0));
    }

    /**
     * Grapple Snowball - pulls enemies toward you!
     */
    public static ItemStack grappleKnockbackSnowball() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Grapple Snowball", NamedTextColor.WHITE, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("Pull your enemies towards you!", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_CUSTOM, KB_GRAPPLE);
    }

    /**
     * Sky Snowball - launches enemies upward
     */
    public static ItemStack skySnowball() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Sky Ball", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("Shoot your enemies to the sky!", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_CUSTOM, kbMult(0.0, 5.0));
    }

    /**
     * Cannon Bow - massive knockback arrows
     */
    public static ItemStack cannonBow() {
        return ItemStack.builder(Material.BOW)
                .set(DataComponents.CUSTOM_NAME, Component.text("Cannon Bow", NamedTextColor.RED, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("5x Horizontal, 3x Vertical", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(KnockbackSystem.PROJECTILE_CUSTOM, kbMult(5.0, 3.0));
    }

    /**
     * Gravity Egg - slow moving with low gravity
     */
    public static ItemStack gravityEgg() {
        return ItemStack.builder(Material.EGG)
                .set(DataComponents.CUSTOM_NAME, Component.text("Gravity Egg", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("Floats through the air slowly", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(ProjectileVelocitySystem.CUSTOM,
                        velMult(0.25, 0.25, 1.0, 0.16, 1.0, 1.0)); // Low gravity
    }

    /**
     * Laser Snowball - super fast, no gravity, no air resistance
     */
    public static ItemStack laserSnowball() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Laser Snowball", NamedTextColor.AQUA, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("Flies straight and fast!", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(ProjectileVelocitySystem.CUSTOM, VEL_LASER);
    }

    /**
     * Heavy Rock Snowball - slow, heavy drop
     */
    public static ItemStack heavyRock() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Heavy Rock", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("Drops like a rock!", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(ProjectileVelocitySystem.CUSTOM, VEL_HEAVY)
                .withTag(KnockbackSystem.PROJECTILE_CUSTOM, KB_HEAVY);
    }

    /**
     * Combo Example - multiply velocity, add knockback
     */
    public static ItemStack comboEgg() {
        return ItemStack.builder(Material.EGG)
                .set(DataComponents.CUSTOM_NAME, Component.text("Combo Egg", NamedTextColor.GREEN, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("Fast + Extra Knockback", NamedTextColor.GRAY)
                ))
                .build()
                .withTag(ProjectileVelocitySystem.CUSTOM, velMult(1.5))
                .withTag(KnockbackSystem.PROJECTILE_CUSTOM,
                        kbMult(2.0, 1.5).thenAdd(0.2, 0.1));
    }
}
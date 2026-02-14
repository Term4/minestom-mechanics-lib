package com.test.minestom;

import com.minestom.mechanics.systems.health.damage.util.DamageOverride;
import com.minestom.mechanics.systems.health.damage.DamageType;
import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;
import com.minestom.mechanics.config.knockback.KnockbackPresets;
import com.minestom.mechanics.systems.knockback.KnockbackSystem;
import com.minestom.mechanics.systems.projectile.components.ProjectileVelocity;
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
 * Test items using per-system item tags.
 */
public class TestItems {

    // Damage bypass presets
    private static final DamageOverride BYPASS_ALL = DamageOverride.override(
            DamageTypeProperties.ATTACK_DEFAULT.withBypassInvulnerability(true).withBypassCreative(true)
    );
    private static final DamageOverride BYPASS_CREATIVE = DamageOverride.override(
            DamageTypeProperties.ATTACK_DEFAULT.withBypassCreative(true)
    );

    // ===========================
    // KNOCKBACK ITEMS
    // ===========================

    public static ItemStack knockbackStick() {
        return ItemStack.builder(Material.STICK)
                .set(DataComponents.CUSTOM_NAME, Component.text("Knockback Stick", NamedTextColor.GOLD, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(Component.text("5x Horizontal Knockback", NamedTextColor.GRAY)))
                .build()
                .withTag(KnockbackSystem.ITEM_CUSTOM, kbMult(5.0, 2.0));
    }

    /** Base KB = position only (lookWeight 0); Sprint KB = look direction (sprintLookWeight 1). */
    public static ItemStack sprintLookSword() {
        return ItemStack.builder(Material.STICK)
                .set(DataComponents.CUSTOM_NAME, Component.text("Sprint Look Sword", NamedTextColor.AQUA, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("Base: position-based KB", NamedTextColor.GRAY),
                        Component.text("Sprint: KB in your look direction", NamedTextColor.GRAY)))
                .build()
                .withTag(KnockbackSystem.ITEM_CUSTOM, kbSet(
                        KnockbackPresets.minemen().withLookWeight(0.0).withSprintLookWeight(1.0)));
    }

    /** Same knockback values as minemen, but uses ADD_VECTORS: adds (posMag*posDir) + (lookMag*lookDir) instead of blending directions. */
    public static ItemStack vectorAddSword() {
        return ItemStack.builder(Material.IRON_SWORD)
                .set(DataComponents.CUSTOM_NAME, Component.text("Vector Add Sword", NamedTextColor.GREEN, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("ADD_VECTORS mode: look+position vectors added", NamedTextColor.GRAY),
                        Component.text("(Same values as minemen preset)", NamedTextColor.GRAY)))
                .build()
                .withTag(KnockbackSystem.ITEM_CUSTOM, kbSet(
                        KnockbackPresets.minemen().withDirectionBlendMode(KnockbackSystem.DirectionBlendMode.ADD_VECTORS)));
    }

    public static ItemStack knockbackEgg() {
        return ItemStack.builder(Material.EGG)
                .set(DataComponents.CUSTOM_NAME, Component.text("Knockback Egg", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(Component.text("+100 Horizontal, +1 Vertical", NamedTextColor.GRAY)))
                .build()
                .withTag(KnockbackSystem.ITEM_PROJECTILE_CUSTOM, kbAdd(4.0, 3.0));
    }

    public static ItemStack slowGrappleEgg() {
        return ItemStack.builder(Material.EGG)
                .set(DataComponents.CUSTOM_NAME, Component.text("Lazy Grapple Egg", NamedTextColor.DARK_RED, TextDecoration.BOLD, TextDecoration.ITALIC))
                .set(DataComponents.LORE, List.of(Component.text("Feeling lazy?", NamedTextColor.GRAY)))
                .build()
                .withTag(KnockbackSystem.ITEM_PROJECTILE_CUSTOM, kbMult(-3, 1.0))
                .withTag(ProjectileVelocity.ITEM_CUSTOM, velMult(0.0, 0.0, 0.0, 0, 1.0, 1.0));
    }

    /** Zero velocity, zero gravity, knockback entirely in thrower's look direction at launch. */
    public static ItemStack knockbackMine() {
        return ItemStack.builder(Material.EGG)
                .set(DataComponents.CUSTOM_NAME, Component.text("Knockback Mine", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(
                        Component.text("Drops in place • KB in throw direction", NamedTextColor.GRAY)))
                .build()
                .withTag(KnockbackSystem.ITEM_PROJECTILE_CUSTOM, KB_LOOK_DIRECTION)
                .withTag(ProjectileVelocity.ITEM_CUSTOM, velMult(0.0, 0.0, 0.0, 0, 1.0, 1.0));
    }

    public static ItemStack grappleKnockbackSnowball() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Grapple Snowball", NamedTextColor.WHITE, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(Component.text("Pull your enemies towards you!", NamedTextColor.GRAY)))
                .build()
                .withTag(KnockbackSystem.ITEM_PROJECTILE_CUSTOM, KB_GRAPPLE);
    }

    public static ItemStack skySnowball() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Sky Ball", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(Component.text("Shoot your enemies to the sky!", NamedTextColor.GRAY)))
                .build()
                .withTag(KnockbackSystem.ITEM_PROJECTILE_CUSTOM, kbMult(0.0, 5.0));
    }

    public static ItemStack cannonBow() {
        return ItemStack.builder(Material.BOW)
                .set(DataComponents.CUSTOM_NAME, Component.text("Cannon Bow", NamedTextColor.RED, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(Component.text("5x Horizontal, 3x Vertical", NamedTextColor.GRAY)))
                .build()
                .withTag(KnockbackSystem.ITEM_PROJECTILE_CUSTOM, kbMult(5.0, 3.0));
    }

    // ===========================
    // VELOCITY ITEMS
    // ===========================

    public static ItemStack gravityEgg() {
        return ItemStack.builder(Material.EGG)
                .set(DataComponents.CUSTOM_NAME, Component.text("Gravity Egg", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(Component.text("Floats through the air slowly", NamedTextColor.GRAY)))
                .build()
                .withTag(ProjectileVelocity.ITEM_CUSTOM, velMult(0.25, 0.25, 1.0, 0.16, 1.0, 1.0));
    }

    public static ItemStack laserSnowball() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Laser Snowball", NamedTextColor.AQUA, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(Component.text("Flies straight and fast!", NamedTextColor.GRAY)))
                .build()
                .withTag(ProjectileVelocity.ITEM_CUSTOM, VEL_LASER);
    }

    public static ItemStack heavyRock() {
        return ItemStack.builder(Material.SNOWBALL)
                .set(DataComponents.CUSTOM_NAME, Component.text("Heavy Rock", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(Component.text("Drops like a rock!", NamedTextColor.GRAY)))
                .build()
                .withTag(ProjectileVelocity.ITEM_CUSTOM, VEL_HEAVY)
                .withTag(KnockbackSystem.ITEM_PROJECTILE_CUSTOM, KB_HEAVY);
    }

    public static ItemStack comboEgg() {
        return ItemStack.builder(Material.EGG)
                .set(DataComponents.CUSTOM_NAME, Component.text("Combo Egg", NamedTextColor.GREEN, TextDecoration.BOLD))
                .set(DataComponents.LORE, List.of(Component.text("Fast + Extra Knockback", NamedTextColor.GRAY)))
                .build()
                .withTag(ProjectileVelocity.ITEM_CUSTOM, velMult(1.5))
                .withTag(KnockbackSystem.ITEM_PROJECTILE_CUSTOM, kbMult(2.0, 1.5).thenAdd(0.2, 0.1));
    }

    // ===========================
    // BYPASS ITEMS
    // ===========================

    /** Helper: safely apply damage override to item (skips if type not registered). */
    private static ItemStack withDamageOverride(ItemStack item, String typeId, DamageOverride override) {
        DamageType dt = DamageType.get(typeId);
        return dt != null ? item.withTag(dt.getItemTag(), override) : item;
    }

    public static ItemStack bypassSword() {
        return withDamageOverride(
                ItemStack.builder(Material.STICK)
                        .set(DataComponents.CUSTOM_NAME, Component.text("Bypass Sword", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                        .set(DataComponents.LORE, List.of(
                                Component.text("Ignores invulnerability", NamedTextColor.GRAY),
                                Component.text("Hits creative players", NamedTextColor.GRAY)))
                        .build(),
                "melee", BYPASS_ALL
        );
    }

    public static ItemStack bypassBow() {
        return withDamageOverride(
                ItemStack.builder(Material.BOW)
                        .set(DataComponents.CUSTOM_NAME, Component.text("Bypass Bow", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                        .set(DataComponents.LORE, List.of(Component.text("Arrows hit creative players", NamedTextColor.GRAY)))
                        .build(),
                "arrow", BYPASS_CREATIVE
        );
    }

    public static ItemStack bypassFishingRod() {
        return withDamageOverride(
                ItemStack.builder(Material.FISHING_ROD)
                        .set(DataComponents.CUSTOM_NAME, Component.text("Bypass Rod", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                        .set(DataComponents.LORE, List.of(Component.text("Bobber hits creative players", NamedTextColor.GRAY)))
                        .build(),
                "generic", BYPASS_CREATIVE
        );
    }

    public static ItemStack bypassSnowballs() {
        return withDamageOverride(
                ItemStack.builder(Material.SNOWBALL)
                        .amount(16)
                        .set(DataComponents.CUSTOM_NAME, Component.text("Bypass Snowballs", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                        .set(DataComponents.LORE, List.of(Component.text("Hits creative players", NamedTextColor.GRAY)))
                        .build(),
                "thrown", BYPASS_CREATIVE
        );
    }
}

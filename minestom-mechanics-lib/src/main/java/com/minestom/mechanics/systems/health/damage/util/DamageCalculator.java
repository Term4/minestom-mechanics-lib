package com.minestom.mechanics.systems.health.damage.util;

import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;
import com.minestom.mechanics.systems.health.HealthSystem;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import static com.minestom.mechanics.config.constants.CombatConstants.*;

/**
 * Calculates final damage from base amount + weapon damage + crits + stacked tag overrides.
 *
 * <p>For melee damage (baseDamage == 0 and attacker is Player), automatically resolves
 * weapon damage from the attacker's held item and applies critical hit multiplier.</p>
 *
 * <p>Formula: {@code max(0, (resolvedDamage * props.multiplier * stackedMultipliers) + stackedModifiers)}</p>
 */
public final class DamageCalculator {

    private DamageCalculator() {}

    public static float calculate(DamageTypeProperties props, String damageTypeId,
                                  Tag<DamageOverride> itemTag, Tag<DamageOverride> entityTag,
                                  @Nullable Entity attacker, LivingEntity victim,
                                  @Nullable ItemStack item, float baseDamage) {
        if (!props.enabled()) return 0f;

        // Auto-resolve weapon damage + crits for melee only (not projectiles/environmental)
        if (baseDamage == 0 && "melee".equals(damageTypeId) && attacker instanceof Player player && item != null) {
            baseDamage = getWeaponDamage(item);
            if (isCriticalHit(player)) {
                baseDamage *= CRITICAL_HIT_MULTIPLIER;
            }
        }

        double stackedMult = getStackedMultiplier(itemTag, entityTag, attacker, victim, item);
        double stackedMod = getStackedModify(itemTag, entityTag, attacker, victim, item);

        // TODO: enchantment modifiers
        // TODO: attribute modifiers

        float damage = (float) ((baseDamage * props.multiplier() * stackedMult) + stackedMod);
        return Math.max(0f, damage);
    }

    // ===========================
    // WEAPON DAMAGE
    // ===========================

    /** Get base damage for a weapon. Returns fist damage for non-weapons. */
    public static float getWeaponDamage(ItemStack weapon) {
        if (weapon == null || weapon.isAir()) return DAMAGE_FIST;
        Material m = weapon.material();

        // Swords
        if (m == Material.WOODEN_SWORD) return DAMAGE_WOODEN_SWORD;
        if (m == Material.STONE_SWORD) return DAMAGE_STONE_SWORD;
        if (m == Material.IRON_SWORD) return DAMAGE_IRON_SWORD;
        if (m == Material.GOLDEN_SWORD) return DAMAGE_GOLDEN_SWORD;
        if (m == Material.DIAMOND_SWORD) return DAMAGE_DIAMOND_SWORD;
        if (m == Material.NETHERITE_SWORD) return DAMAGE_NETHERITE_SWORD;

        // Axes
        if (m == Material.WOODEN_AXE) return DAMAGE_WOODEN_AXE;
        if (m == Material.STONE_AXE) return DAMAGE_STONE_AXE;
        if (m == Material.IRON_AXE) return DAMAGE_IRON_AXE;
        if (m == Material.GOLDEN_AXE) return DAMAGE_GOLDEN_AXE;
        if (m == Material.DIAMOND_AXE) return DAMAGE_DIAMOND_AXE;
        if (m == Material.NETHERITE_AXE) return DAMAGE_NETHERITE_AXE;

        // Pickaxes
        if (m == Material.WOODEN_PICKAXE) return DAMAGE_WOODEN_PICKAXE;
        if (m == Material.STONE_PICKAXE) return DAMAGE_STONE_PICKAXE;
        if (m == Material.IRON_PICKAXE) return DAMAGE_IRON_PICKAXE;
        if (m == Material.GOLDEN_PICKAXE) return DAMAGE_GOLDEN_PICKAXE;
        if (m == Material.DIAMOND_PICKAXE) return DAMAGE_DIAMOND_PICKAXE;
        if (m == Material.NETHERITE_PICKAXE) return DAMAGE_NETHERITE_PICKAXE;

        // Shovels
        if (m == Material.WOODEN_SHOVEL) return DAMAGE_WOODEN_SHOVEL;
        if (m == Material.STONE_SHOVEL) return DAMAGE_STONE_SHOVEL;
        if (m == Material.IRON_SHOVEL) return DAMAGE_IRON_SHOVEL;
        if (m == Material.GOLDEN_SHOVEL) return DAMAGE_GOLDEN_SHOVEL;
        if (m == Material.DIAMOND_SHOVEL) return DAMAGE_DIAMOND_SHOVEL;
        if (m == Material.NETHERITE_SHOVEL) return DAMAGE_NETHERITE_SHOVEL;

        return DAMAGE_FIST;
    }

    // ===========================
    // CRITICAL HITS
    // ===========================

    /** Check if a player's attack should be a critical hit. */
    public static boolean isCriticalHit(Player attacker) {
        if (attacker.isOnGround()) return false;
        // TODO: config for allowSprintCrits
        try {
            double fallDistance = HealthSystem.getInstance().getFallDistance(attacker);
            return fallDistance > 0;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    // ===========================
    // TAG STACKING
    // ===========================

    private static double getStackedMultiplier(Tag<DamageOverride> itemTag, Tag<DamageOverride> entityTag,
                                               @Nullable Entity attacker, LivingEntity victim,
                                               @Nullable ItemStack item) {
        double result = 1.0;
        DamageOverride override;
        if (item != null && !item.isAir()) {
            override = item.getTag(itemTag);
            if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        }
        if (attacker != null) {
            override = attacker.getTag(entityTag);
            if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        }
        override = victim.getTag(entityTag);
        if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        if (victim.getInstance() != null) {
            override = victim.getInstance().getTag(entityTag);
            if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        }
        return result;
    }

    private static double getStackedModify(Tag<DamageOverride> itemTag, Tag<DamageOverride> entityTag,
                                           @Nullable Entity attacker, LivingEntity victim,
                                           @Nullable ItemStack item) {
        double result = 0.0;
        DamageOverride override;
        if (item != null && !item.isAir()) {
            override = item.getTag(itemTag);
            if (override != null && override.modify() != null) for (double m : override.modify()) result += m;
        }
        if (attacker != null) {
            override = attacker.getTag(entityTag);
            if (override != null && override.modify() != null) for (double m : override.modify()) result += m;
        }
        override = victim.getTag(entityTag);
        if (override != null && override.modify() != null) for (double m : override.modify()) result += m;
        if (victim.getInstance() != null) {
            override = victim.getInstance().getTag(entityTag);
            if (override != null && override.modify() != null) for (double m : override.modify()) result += m;
        }
        return result;
    }
}

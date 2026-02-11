package com.minestom.mechanics.systems.health.damage.util;

import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates final damage from base amount + properties multiplier + stacked tag overrides.
 *
 * <p>Formula: {@code max(0, (baseDamage * props.multiplier * stackedMultipliers) + stackedModifiers)}</p>
 */
public final class DamageCalculator {

    private DamageCalculator() {}

    public static float calculate(DamageTypeProperties props, Tag<DamageOverride> itemTag, Tag<DamageOverride> entityTag,
                                  @Nullable Entity attacker, LivingEntity victim,
                                  @Nullable ItemStack item, float baseDamage) {
        if (!props.enabled()) return 0f;

        double stackedMult = getStackedMultiplier(itemTag, entityTag, attacker, victim, item);
        double stackedMod = getStackedModify(itemTag, entityTag, attacker, victim, item);

        // TODO: enchantment modifiers
        // TODO: attribute modifiers

        float damage = (float) ((baseDamage * props.multiplier() * stackedMult) + stackedMod);
        return Math.max(0f, damage);
    }

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

package com.minestom.mechanics.systems.health.damage;

import com.minestom.mechanics.systems.health.damage.util.DamageOverride;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates final damage from base amount + properties multiplier + stacked tag overrides.
 * Extracted from DamageType so it can grow independently (enchants, attributes, etc.).
 *
 * <p>Formula: {@code max(0, (baseDamage * props.multiplier * stackedMultipliers) + stackedModifiers)}</p>
 */
public final class DamageCalculator {

    private DamageCalculator() {}

    /**
     * Calculate final damage with all modifiers applied.
     *
     * @param props      resolved properties for this context
     * @param itemTag    serialized tag for reading item overrides
     * @param entityTag  transient tag for reading entity/world overrides
     * @param attacker   attacker entity (nullable)
     * @param victim     victim entity
     * @param item       attacker's held item (nullable)
     * @param baseDamage raw damage amount
     * @return final damage (>= 0), or 0 if disabled
     */
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

    // ===========================
    // TAG STACKING
    // ===========================

    private static double getStackedMultiplier(Tag<DamageOverride> itemTag, Tag<DamageOverride> entityTag,
                                               @Nullable Entity attacker, LivingEntity victim,
                                               @Nullable ItemStack item) {
        double result = 1.0;
        DamageOverride override;
        // Item (serialized tag)
        if (item != null && !item.isAir()) {
            override = item.getTag(itemTag);
            if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        }
        // Attacker entity (transient tag)
        if (attacker != null) {
            override = attacker.getTag(entityTag);
            if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        }
        // Victim entity
        override = victim.getTag(entityTag);
        if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        // World
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

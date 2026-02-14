package com.minestom.mechanics.systems.health.damage;

import com.minestom.mechanics.systems.health.damage.util.DamageOverride;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Unified properties for a damage type.
 * Each {@link DamageType} carries a default set of these properties,
 * which can be overridden per item/entity/world via {@link DamageOverride} tags.
 *
 * <p>Usage:</p>
 * <pre>
 * // Using builder
 * DamageTypeProperties props = DamageTypeProperties.builder()
 *     .blockable(true)
 *     .damageReplacement(true)
 *     .build();
 *
 * // Deriving from a preset
 * DamageTypeProperties custom = DamageTypeProperties.ENVIRONMENTAL_DEFAULT
 *     .withBlockable(true)
 *     .withMultiplier(2.0f);
 * </pre>
 */
public record DamageTypeProperties(
        boolean enabled,
        float multiplier,
        boolean blockable,
        boolean penetratesArmor,
        boolean bypassInvulnerability,
        boolean bypassCreative,
        boolean damageReplacement,
        boolean knockbackOnReplacement,
        float replacementCutoff,
        boolean hurtEffect,
        int invulnerabilityBufferTicks,
        boolean noReplacementSameItem
) {

    // ===========================
    // PRESETS
    // ===========================

    /** Default properties for environmental damage (fall, fire, cactus, etc.) — no invuln buffer */
    public static final DamageTypeProperties ENVIRONMENTAL_DEFAULT = new DamageTypeProperties(
            true, 1.0f, false, true, false, false, true, false, 0f, false, 0, false
    );

    /** Default properties for attack damage (melee, projectile, etc.) — no invuln buffer by default */
    public static final DamageTypeProperties ATTACK_DEFAULT = new DamageTypeProperties(
            true, 1.0f, true, false, false, false, true, false, 0f, false, 0, false
    );

    /** Disabled damage type */
    public static final DamageTypeProperties DISABLED = new DamageTypeProperties(
            false, 0f, false, false, false, false, false, false, 0f, false, 0, false
    );

    // ===========================
    // WITH METHODS
    // ===========================

    public DamageTypeProperties withEnabled(boolean enabled) {
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    public DamageTypeProperties withMultiplier(float multiplier) {
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    public DamageTypeProperties withBlockable(boolean blockable) {
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    public DamageTypeProperties withPenetratesArmor(boolean penetratesArmor) {
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    public DamageTypeProperties withBypassInvulnerability(boolean bypassInvulnerability) {
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    public DamageTypeProperties withBypassCreative(boolean bypassCreative) {
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    public DamageTypeProperties withDamageReplacement(boolean damageReplacement) {
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    public DamageTypeProperties withKnockbackOnReplacement(boolean knockbackOnReplacement) {
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    /** Replacement requires incoming damage >= previous + cutoff. 0 = no cutoff. */
    public DamageTypeProperties withReplacementCutoff(float cutoff) {
        if (cutoff < 0) throw new IllegalArgumentException("Replacement cutoff cannot be negative");
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, cutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    /** When false, no camera shake/red flash for this damage type (replacement and normal). */
    public DamageTypeProperties withHurtEffect(boolean hurtEffect) {
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    /** Hits during the last N ticks of invulnerability are buffered to apply when invuln ends. 0 = no buffer. */
    public DamageTypeProperties withInvulnerabilityBufferTicks(int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("Invulnerability buffer ticks cannot be negative");
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, ticks, noReplacementSameItem);
    }

    /** When true (Minemen-style), no damage replacement if replacement hit uses same item (including fist) as initial. */
    public DamageTypeProperties withNoReplacementSameItem(boolean noReplacementSameItem) {
        return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
    }

    /** Check if two items are the same for replacement exclusion (fist = air). */
    public static boolean isSameItem(@Nullable ItemStack a, @Nullable ItemStack b) {
        boolean aFist = a == null || a.isAir();
        boolean bFist = b == null || b.isAir();
        if (aFist && bFist) return true;
        if (aFist != bFist) return false;
        return a.material().equals(b.material());
    }

    // ===========================
    // BUILDER
    // ===========================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = true;
        private float multiplier = 1.0f;
        private boolean blockable = false;
        private boolean penetratesArmor = false;
        private boolean bypassInvulnerability = false;
        private boolean bypassCreative = false;
        private boolean damageReplacement = false;
        private boolean knockbackOnReplacement = false;
        private float replacementCutoff = 0f;
        private boolean hurtEffect = false;
        private int invulnerabilityBufferTicks = 0;
        private boolean noReplacementSameItem = false;

        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder multiplier(float multiplier) { this.multiplier = multiplier; return this; }
        public Builder blockable(boolean blockable) { this.blockable = blockable; return this; }
        public Builder penetratesArmor(boolean penetratesArmor) { this.penetratesArmor = penetratesArmor; return this; }
        public Builder bypassInvulnerability(boolean bypassInvulnerability) { this.bypassInvulnerability = bypassInvulnerability; return this; }
        public Builder bypassCreative(boolean bypassCreative) { this.bypassCreative = bypassCreative; return this; }
        public Builder damageReplacement(boolean damageReplacement) { this.damageReplacement = damageReplacement; return this; }
        public Builder knockbackOnReplacement(boolean knockbackOnReplacement) { this.knockbackOnReplacement = knockbackOnReplacement; return this; }
        public Builder replacementCutoff(float cutoff) { this.replacementCutoff = cutoff; return this; }
        public Builder hurtEffect(boolean hurtEffect) { this.hurtEffect = hurtEffect; return this; }
        public Builder invulnerabilityBufferTicks(int ticks) { this.invulnerabilityBufferTicks = ticks; return this; }
        public Builder noReplacementSameItem(boolean noReplacementSameItem) { this.noReplacementSameItem = noReplacementSameItem; return this; }

        /**
         * Copy all values from a template.
         */
        public Builder from(DamageTypeProperties template) {
            this.enabled = template.enabled;
            this.multiplier = template.multiplier;
            this.blockable = template.blockable;
            this.penetratesArmor = template.penetratesArmor;
            this.bypassInvulnerability = template.bypassInvulnerability;
            this.bypassCreative = template.bypassCreative;
            this.damageReplacement = template.damageReplacement;
            this.knockbackOnReplacement = template.knockbackOnReplacement;
            this.replacementCutoff = template.replacementCutoff;
            this.hurtEffect = template.hurtEffect;
            this.invulnerabilityBufferTicks = template.invulnerabilityBufferTicks;
            this.noReplacementSameItem = template.noReplacementSameItem;
            return this;
        }

        public DamageTypeProperties build() {
            return new DamageTypeProperties(enabled, multiplier, blockable, penetratesArmor,
                    bypassInvulnerability, bypassCreative, damageReplacement, knockbackOnReplacement, replacementCutoff, hurtEffect, invulnerabilityBufferTicks, noReplacementSameItem);
        }
    }
}

package com.minestom.mechanics.systems.blocking.tags;

import org.jetbrains.annotations.Nullable;

/**
 * Tag value for blockable items. Presence of this tag on an item means the item can be used to block.
 * Optional fields (null) fall back to CombatConfig defaults.
 * {@code applyLegacySlowdown}: when true, legacy clients get movement slowdown while blocking with this item; when false, no slowdown (e.g. sword preset).
 * {@code knockbackHMultiplier}, {@code knockbackVMultiplier}: fraction of knockback retained (0 = full reduction, 1 = no reduction). Inverse of CombatConfig's reduction (0 = none, 1 = full).
 */
public record BlockableTagValue(
        @Nullable Boolean applyLegacySlowdown,
        @Nullable Double damageReduction,
        @Nullable Double knockbackHMultiplier,
        @Nullable Double knockbackVMultiplier
) {

    /**
     * Create a blockable tag with explicit legacy slowdown and damage/knockback modifiers.
     * When {@code applyLegacySlowdown} is true, legacy clients get movement slowdown while blocking; when false, they do not (e.g. sword preset).
     */
    public static BlockableTagValue blockable(boolean applyLegacySlowdown, double damageReduction, double knockbackH, double knockbackV) {
        return new BlockableTagValue(applyLegacySlowdown, damageReduction, knockbackH, knockbackV);
    }

    /**
     * Create with nullable modifiers (fall back to config). Legacy slowdown defaults to true so non-preset items get slowdown.
     */
    public static BlockableTagValue blockable(@Nullable Boolean applyLegacySlowdown,
                                              @Nullable Double damageReduction,
                                              @Nullable Double knockbackHMultiplier,
                                              @Nullable Double knockbackVMultiplier) {
        return new BlockableTagValue(applyLegacySlowdown, damageReduction, knockbackHMultiplier, knockbackVMultiplier);
    }

    /**
     * Create with damage/knockback only; legacy slowdown defaults to true (apply slowdown for legacy when blocking).
     */
    public static BlockableTagValue blockable(double damageReduction, double knockbackH, double knockbackV) {
        return new BlockableTagValue(true, damageReduction, knockbackH, knockbackV);
    }

    /**
     * Mark item as blockable with no overrides (all null = use config defaults, no legacy slowdown).
     */
    public static BlockableTagValue blockableDefault() {
        return new BlockableTagValue(null, null, null, null);
    }

    /**
     * PvP sword preset: no legacy slowdown, all modifiers null so CombatConfig applies.
     * Damage/knockback reduction come from the combat preset (e.g. CombatPresets.MINEMEN).
     */
    public static final BlockableTagValue SWORD_PRESET = blockable(false, null, null, null);
}

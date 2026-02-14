package com.minestom.mechanics.config.combat;

import com.minestom.mechanics.config.knockback.KnockbackPresets;

import static com.minestom.mechanics.config.constants.CombatConstants.*;

/**
 * Preset combat configurations for common PvP modes.
 * These only configure combat mechanics - see GameplayPresets for damage/invulnerability.
 */
public final class CombatPresets {

    private CombatPresets() {} // Utility class

    /**
     * MinemanClub
     */
    public static final CombatConfig MINEMEN = new CombatConfig(
            true, 1.5f, true,
            KnockbackPresets.minemen(),
            6,
            12,
            1,
            2, 1,
            true, 1,
            true, 0.5, 0, 0, true, true
    );

    /**
     * Vanilla 1.8
     */
    public static final CombatConfig VANILLA = new CombatConfig(
            true, CRITICAL_HIT_MULTIPLIER, false,
            KnockbackPresets.vanilla18(),
            0,
            0, 0,
            0, 0,
            false, 0,
            true, 0.5, 0,
            0, true, true
    );

    /**
     * Hypixel
     */
    public static final CombatConfig HYPIXEL = new CombatConfig(
            true, CRITICAL_HIT_MULTIPLIER, true,
            KnockbackPresets.hypixel(),
            5,
            0, 0,
            0, 0,
            false, 0,
            true,0.5, 0, 0, true, true
    );
}
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
            // Attack
            true, CRITICAL_HIT_MULTIPLIER, true,
            // Knockback
            KnockbackPresets.minemen(),
            // Sprint window
            true, true, 6,
            // Swing hit window (ticks after attack when swings can hit)
            12,
            // Swing look check (ticks after each swing to poll look — hit lands when crosshair passes over victim)
            1,
            // Blocking
            true, 0.5, 0, 0, true, true
    );

    /**
     * Vanilla 1.8
     */
    public static final CombatConfig VANILLA = new CombatConfig(
            // Attack
            true, CRITICAL_HIT_MULTIPLIER, false,
            // Knockback
            KnockbackPresets.vanilla18(),
            // Sprint window
            false, false, DEFAULT_SPRINT_WINDOW_MAX_TICKS,
            // Swing hit window
            0, 0,
            // Blocking
            true, DEFAULT_BLOCKING_DAMAGE_REDUCTION, DEFAULT_BLOCKING_KB_HORIZONTAL,
            DEFAULT_BLOCKING_KB_VERTICAL, true, true
    );

    /**
     * Hypixel
     */
    public static final CombatConfig HYPIXEL = new CombatConfig(
            // Attack
            true, CRITICAL_HIT_MULTIPLIER, true,
            // Knockback
            KnockbackPresets.hypixel(),
            // Sprint window
            true, false, 5,
            // Swing hit window
            0, 0,
            // Blocking
            true,0.5, 0.95, 0.95, true, true
    );
}
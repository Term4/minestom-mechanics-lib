package com.minestom.mechanics.config.combat;

import com.minestom.mechanics.config.knockback.KnockbackPresets;
import static com.minestom.mechanics.constants.CombatConstants.*;

/**
 * Preset combat configurations for common PvP modes.
 * These only configure combat mechanics - see GameplayPresets for damage/invulnerability.
 */
public final class CombatPresets {

    private CombatPresets() {} // Utility class

    /**
     * MinemanClub combat mode - fast combo PvP.
     * - No attack cooldown (1.8 style)
     * - Minemen knockback profile
     * - Sprint crits enabled
     * - Reduced blocking effectiveness
     */
    public static final CombatConfig MINEMEN = new CombatConfig(
            // Attack
            true, CRITICAL_HIT_MULTIPLIER, true,
            // Knockback
            KnockbackPresets.minemen(),
            // Sprint window
            true, false, 5,
            // Blocking
            true, 0.5, 0.05, 0.05, true, true
    );

    /**
     * Vanilla 1.8 combat mode - classic PvP.
     * - No attack cooldown
     * - Vanilla knockback
     * - Standard blocking
     */
    public static final CombatConfig VANILLA = new CombatConfig(
            // Attack
            true, CRITICAL_HIT_MULTIPLIER, false,
            // Knockback
            KnockbackPresets.vanilla18(),
            // Sprint window
            false, false, DEFAULT_SPRINT_WINDOW_MAX_TICKS,
            // Blocking
            true, DEFAULT_BLOCKING_DAMAGE_REDUCTION, DEFAULT_BLOCKING_KB_HORIZONTAL,
            DEFAULT_BLOCKING_KB_VERTICAL, true, true
    );

    /**
     * Hypixel-style combat mode.
     * - Fast combat with Hypixel knockback
     * - Moderate blocking effectiveness
     */
    public static final CombatConfig HYPIXEL = new CombatConfig(
            // Attack
            true, CRITICAL_HIT_MULTIPLIER, true,
            // Knockback
            KnockbackPresets.hypixel(),
            // Sprint window
            true, false, 5,
            // Blocking
            true,0.5, 0.05, 0.05, true, true
    );

    /**
     * Standard/balanced combat mode.
     * Good default for most servers - same as VANILLA.
     */
    public static final CombatConfig STANDARD = VANILLA;

    /**
     * No-combo mode for anticheat-heavy servers.
     * - Higher blocking effectiveness
     * - Less aggressive knockback
     * - Strict sprint mechanics
     */
    public static final CombatConfig ANTICHEAT_SAFE = new CombatConfig(
            // Attack
            true, CRITICAL_HIT_MULTIPLIER, false,
            // Knockback
            KnockbackPresets.vanilla18(),
            // Sprint window
            false, false, 3,
            // Blocking
            true,0.5, 0.05, 0.05, true, true
    );
}
package com.minestom.mechanics.config.combat;

import com.minestom.mechanics.features.knockback.KnockbackProfile;
import static com.minestom.mechanics.config.combat.CombatConstants.*;

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
            KnockbackProfile.MINEMEN,
            // Sprint window
            true, false, 5,
            // Blocking
            0.5, 0.5, 0.5, true, true
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
            KnockbackProfile.VANILLA,
            // Sprint window
            false, false, DEFAULT_SPRINT_WINDOW_MAX_TICKS,
            // Blocking
            DEFAULT_BLOCKING_DAMAGE_REDUCTION, DEFAULT_BLOCKING_KB_HORIZONTAL,
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
            KnockbackProfile.HYPIXEL,
            // Sprint window
            true, false, 5,
            // Blocking
            0.5, 0.6, 0.6, true, true
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
            KnockbackProfile.VANILLA,
            // Sprint window
            false, false, 3,
            // Blocking
            0.5, 0.8, 0.8, true, true
    );
}
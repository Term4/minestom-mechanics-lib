package com.minestom.mechanics.config.gameplay;

import static com.minestom.mechanics.config.constants.CombatConstants.*;

/**
 * Preset damage configurations for common server modes.
 */
public final class DamagePresets {

    private DamagePresets() {}

    /**
     * MinemanClub damage
     */
    public static final DamageConfig MINEMEN = new DamageConfig(
            10,
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,
            true, false,
            1f, false
    );

    /**
     * Vanilla 1.8 damage
     */
    public static final DamageConfig VANILLA = new DamageConfig(
            DEFAULT_INVULNERABILITY_TICKS,
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,
            DEFAULT_DAMAGE_REPLACEMENT, DEFAULT_KNOCKBACK_ON_REPLACEMENT,
            0f, false
    );

    /**
     * Hypixel
     */
    public static final DamageConfig HYPIXEL = new DamageConfig(
            8,
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,
            true, false,
            0f, true
    );
}
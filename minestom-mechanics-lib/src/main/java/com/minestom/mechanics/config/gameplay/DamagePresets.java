package com.minestom.mechanics.config.gameplay;

import static com.minestom.mechanics.config.constants.CombatConstants.*;

/**
 * Preset damage configurations for common server modes.
 */
public final class DamagePresets {

    private DamagePresets() {} // Utility class

    /**
     * MinemanClub damage - fast-paced combat.
     * - 10 tick invulnerability (50ms)
     * - Standard environmental damage
     * - Damage replacement enabled (cutoff 1 = replacement only if incoming is 1+ more)
     * - No hurt effect (silent damage, minemen-style)
     * - Hit queue and same-item exclusion are in CombatConfig (CombatPresets.MINEMEN)
     */
    public static final DamageConfig MINEMEN = new DamageConfig(
            10,   // invulnerabilityTicks (50ms)
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,
            true, false,  // damageReplacement, knockbackOnReplacement
            true,  // logReplacementDamage
            1f, false    // replacementCutoff (1), hurtEffect (off)
    );

    /**
     * Vanilla 1.8 damage - standard Minecraft.
     * - 20 tick invulnerability (100ms)
     * - Normal environmental damage
     * - Default damage replacement
     */
    public static final DamageConfig VANILLA = new DamageConfig(
            DEFAULT_INVULNERABILITY_TICKS,
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,
            DEFAULT_DAMAGE_REPLACEMENT, DEFAULT_KNOCKBACK_ON_REPLACEMENT,
            true,  // logReplacementDamage
            0f, false    // replacementCutoff (0), hurtEffect (off)
    );

    /**
     * Hypixel-style damage.
     * - 15 tick invulnerability (75ms)
     * - Standard environmental damage
     */
    public static final DamageConfig HYPIXEL = new DamageConfig(
            15,
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,
            true, false,
            true,  // logReplacementDamage
            0f, false    // replacementCutoff (0), hurtEffect (off)
    );

    /**
     * Standard/balanced damage.
     */
    public static final DamageConfig STANDARD = VANILLA;

    /**
     * No environmental damage - for minigames/arenas.
     * - Normal invulnerability
     * - All environmental damage disabled
     */
    public static final DamageConfig NO_ENVIRONMENT = new DamageConfig(
            DEFAULT_INVULNERABILITY_TICKS,
            false, 0.0f,  // No fall damage
            false, 0.0f,  // No fire damage
            false, false,
            true,  // logReplacementDamage
            0f, false    // replacementCutoff (0), hurtEffect (off)
    );

    /**
     * High invulnerability for slower-paced combat.
     * - 25 tick invulnerability (125ms)
     */
    public static final DamageConfig HIGH_INVULN = new DamageConfig(
            25,
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,
            false, false,
            true,  // logReplacementDamage
            0f, false    // replacementCutoff (0), hurtEffect (off)
    );
}
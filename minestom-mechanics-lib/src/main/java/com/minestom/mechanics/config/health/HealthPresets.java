package com.minestom.mechanics.config.health;

import static com.minestom.mechanics.config.constants.CombatConstants.*;

/**
 * Health system presets for common server configurations.
 */
public final class HealthPresets {
    
    private HealthPresets() {
        throw new AssertionError("Cannot instantiate presets class");
    }
    
    /**
     * MinemenClub competitive PvP configuration
     */
    public static final HealthConfig MINEMEN = new HealthConfig(
            10,  // Invulnerability ticks
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER, true,  // Fall damage (blockable=false)
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER, false,   // Fire damage (blockable=true)
            true, 1.0f, false,  // Cactus damage (blockable=true)
            true, false, true,  // Damage replacement (with knockback, with logging)
            false, 0.0f, 0  // Regeneration (disabled)
    );
    
    /**
     * Vanilla Minecraft configuration
     */
    public static final HealthConfig VANILLA = new HealthConfig(
            DEFAULT_INVULNERABILITY_TICKS,  // Invulnerability ticks
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER, false,  // Fall damage (blockable=false)
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER, false,   // Fire damage (blockable=true)
            true, 1.0f, false,  // Cactus damage (blockable=true)
            DEFAULT_DAMAGE_REPLACEMENT, DEFAULT_KNOCKBACK_ON_REPLACEMENT, true,  // Damage replacement
            false, 0.0f, 0  // Regeneration (disabled)
    );
    
    /**
     * Hypixel server configuration
     */
    public static final HealthConfig HYPIXEL = new HealthConfig(
            15,  // Invulnerability ticks
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER, false,  // Fall damage (blockable=false)
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER, false,   // Fire damage (blockable=true)
            true, 1.0f, false,  // Cactus damage (blockable=true)
            true, false, true,  // Damage replacement (no knockback, with logging)
            false, 0.0f, 0  // Regeneration (disabled)
    );
}


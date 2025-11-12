package com.minestom.mechanics.systems.health;

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
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,  // Fall damage
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,  // Fire damage
            true, 1.0f,  // Cactus damage
            true, true, true,  // Damage replacement (with knockback, with logging)
            false, 0.0f, 0  // Regeneration (disabled)
    );
    
    /**
     * Vanilla Minecraft configuration
     */
    public static final HealthConfig VANILLA = new HealthConfig(
            DEFAULT_INVULNERABILITY_TICKS,  // Invulnerability ticks
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,  // Fall damage
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,  // Fire damage
            true, 1.0f,  // Cactus damage
            DEFAULT_DAMAGE_REPLACEMENT, DEFAULT_KNOCKBACK_ON_REPLACEMENT, true,  // Damage replacement
            false, 0.0f, 0  // Regeneration (disabled)
    );
    
    /**
     * Hypixel server configuration
     */
    public static final HealthConfig HYPIXEL = new HealthConfig(
            15,  // Invulnerability ticks
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,  // Fall damage
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,  // Fire damage
            true, 1.0f,  // Cactus damage
            true, false, true,  // Damage replacement (no knockback, with logging)
            false, 0.0f, 0  // Regeneration (disabled)
    );
    
    /**
     * Standard configuration (alias for VANILLA)
     */
    public static final HealthConfig STANDARD = VANILLA;
    
    /**
     * No environmental damage configuration
     */
    public static final HealthConfig NO_ENVIRONMENT = new HealthConfig(
            DEFAULT_INVULNERABILITY_TICKS,  // Invulnerability ticks
            false, 0.0f,  // Fall damage (disabled)
            false, 0.0f,  // Fire damage (disabled)
            false, 0.0f,  // Cactus damage (disabled)
            false, false, true,  // Damage replacement
            false, 0.0f, 0  // Regeneration (disabled)
    );
    
    /**
     * High invulnerability configuration
     */
    public static final HealthConfig HIGH_INVULN = new HealthConfig(
            25,  // Invulnerability ticks (higher)
            true, DEFAULT_FALL_DAMAGE_MULTIPLIER,  // Fall damage
            true, DEFAULT_FIRE_DAMAGE_MULTIPLIER,  // Fire damage
            true, 1.0f,  // Cactus damage
            false, false, true,  // Damage replacement
            false, 0.0f, 0  // Regeneration (disabled)
    );
}


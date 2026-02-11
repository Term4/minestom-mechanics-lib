package com.minestom.mechanics.config.health;

import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;

/**
 * Health system presets for common server configurations.
 * These provide the global {@link HealthConfig} settings.
 *
 * <p>Per-damage-type settings are configured separately on each
 * {@link com.minestom.mechanics.systems.health.DamageHandler} via
 * {@link DamageTypeProperties}.</p>
 *
 * <p>Example (customize after initialization):</p>
 * <pre>
 * var system = HealthSystem.initialize(HealthPresets.MINEMEN);
 * system.getHandler(FallDamageHandler.class).setDefaults(
 *     DamageTypeProperties.ENVIRONMENTAL_DEFAULT.withBlockable(true).withDamageReplacement(true)
 * );
 * </pre>
 */
public final class HealthPresets {

    private HealthPresets() {
        throw new AssertionError("Cannot instantiate presets class");
    }

    /**
     * MinemenClub competitive PvP configuration.
     * 10 tick invulnerability, damage logging enabled.
     */
    public static final HealthConfig MINEMEN = new HealthConfig(
            10,     // invulnerabilityTicks
            true,   // logDamage
            false,  // regenerationEnabled
            0f,     // regenerationAmount
            0       // regenerationIntervalTicks
    );

    /**
     * Vanilla Minecraft configuration.
     * 10 tick invulnerability, damage logging enabled.
     */
    public static final HealthConfig VANILLA = new HealthConfig(
            10,     // invulnerabilityTicks
            true,   // logDamage
            false,  // regenerationEnabled
            0f,     // regenerationAmount
            0       // regenerationIntervalTicks
    );

    /**
     * Hypixel server configuration.
     * 15 tick invulnerability, damage logging enabled.
     */
    public static final HealthConfig HYPIXEL = new HealthConfig(
            15,     // invulnerabilityTicks
            true,   // logDamage
            false,  // regenerationEnabled
            0f,     // regenerationAmount
            0       // regenerationIntervalTicks
    );
}

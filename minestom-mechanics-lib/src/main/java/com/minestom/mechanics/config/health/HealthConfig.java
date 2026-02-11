package com.minestom.mechanics.config.health;

import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;

/**
 * Simplified health system configuration.
 * Contains only global settings. Per-damage-type settings now live on
 * {@link DamageTypeProperties} attached to each
 * {@link com.minestom.mechanics.systems.health.DamageHandler}.
 *
 * <p>Usage:</p>
 * <pre>
 * HealthConfig config = HealthPresets.MINEMEN
 *     .withInvulnerabilityTicks(15)
 *     .withLogDamage(false);
 * </pre>
 */
public record HealthConfig(
        /** Invulnerability ticks after taking damage. 0 = invulnerability disabled. */
        int invulnerabilityTicks,

        /** Whether to log damage events for debugging. */
        boolean logDamage,

        // Future: Regeneration
        boolean regenerationEnabled,
        float regenerationAmount,
        int regenerationIntervalTicks
) {

    // ===========================
    // VALIDATION
    // ===========================

    public HealthConfig {
        if (invulnerabilityTicks < 0)
            throw new IllegalArgumentException("Invulnerability ticks must be >= 0");
        if (regenerationAmount < 0)
            throw new IllegalArgumentException("Regeneration amount must be >= 0");
        if (regenerationEnabled && regenerationIntervalTicks < 1)
            throw new IllegalArgumentException("Regeneration interval must be >= 1 when enabled");
        if (!regenerationEnabled && regenerationIntervalTicks < 0)
            throw new IllegalArgumentException("Regeneration interval must be >= 0");
    }

    // ===========================
    // CONVENIENCE
    // ===========================

    /** Whether invulnerability is enabled (ticks > 0). */
    public boolean isInvulnerabilityEnabled() {
        return invulnerabilityTicks > 0;
    }

    // ===========================
    // WITH METHODS
    // ===========================

    public HealthConfig withInvulnerabilityTicks(int ticks) {
        return new HealthConfig(ticks, logDamage, regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    public HealthConfig withLogDamage(boolean log) {
        return new HealthConfig(invulnerabilityTicks, log, regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    public HealthConfig withRegeneration(boolean enabled, float amount, int intervalTicks) {
        return new HealthConfig(invulnerabilityTicks, logDamage, enabled, amount, intervalTicks);
    }

    public HealthConfig withRegeneration(boolean enabled) {
        return new HealthConfig(invulnerabilityTicks, logDamage, enabled, regenerationAmount, regenerationIntervalTicks);
    }
}

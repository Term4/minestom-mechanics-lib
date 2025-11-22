package com.minestom.mechanics.config.health;

/**
 * Health system configuration record.
 * Expanded from DamageConfig to support health features including future regeneration.
 *
 * Usage:
 * <pre>
 * HealthConfig health = HealthPresets.MINEMEN
 *     .withInvulnerabilityTicks(15)
 *     .withFallDamage(false);
 * </pre>
 */
public record HealthConfig(
        // Invulnerability
        int invulnerabilityTicks,

        // Environmental damage
        boolean fallDamageEnabled,
        float fallDamageMultiplier,
        boolean fireDamageEnabled,
        float fireDamageMultiplier,
        boolean cactusDamageEnabled,
        float cactusDamageMultiplier,

        // Damage replacement system
        boolean damageReplacementEnabled,
        boolean knockbackOnReplacement,
        boolean logReplacementDamage,
        
        // Future: Regeneration support
        boolean regenerationEnabled,
        float regenerationAmount,
        int regenerationIntervalTicks
) {

    // Validation
    public HealthConfig {
        if (invulnerabilityTicks < 0)
            throw new IllegalArgumentException("Invulnerability ticks must be >= 0");
        if (fallDamageMultiplier < 0)
            throw new IllegalArgumentException("Fall damage multiplier must be >= 0");
        if (fireDamageMultiplier < 0)
            throw new IllegalArgumentException("Fire damage multiplier must be >= 0");
        if (cactusDamageMultiplier < 0)
            throw new IllegalArgumentException("Cactus damage multiplier must be >= 0");
        if (regenerationAmount < 0)
            throw new IllegalArgumentException("Regeneration amount must be >= 0");
        // Only validate regeneration interval if regeneration is enabled
        if (regenerationEnabled && regenerationIntervalTicks < 1)
            throw new IllegalArgumentException("Regeneration interval must be >= 1 when regeneration is enabled");
        // If regeneration is disabled, allow 0 for the interval (it won't be used)
        if (!regenerationEnabled && regenerationIntervalTicks < 0)
            throw new IllegalArgumentException("Regeneration interval must be >= 0");
    }

    // ===== COMPATIBILITY GETTERS (match old DamageConfig) =====
    // These allow existing code to work without changes

    public boolean isFallDamageEnabled() { return fallDamageEnabled; }
    public float getFallDamageMultiplier() { return fallDamageMultiplier; }
    public boolean isFireDamageEnabled() { return fireDamageEnabled; }
    public float getFireDamageMultiplier() { return fireDamageMultiplier; }
    public int getInvulnerabilityTicks() { return invulnerabilityTicks; }
    public boolean isDamageReplacementEnabled() { return damageReplacementEnabled; }
    public boolean isKnockbackOnReplacement() { return knockbackOnReplacement; }
    public boolean isLogReplacementDamage() { return logReplacementDamage; }

    // ===== INVULNERABILITY =====

    public HealthConfig withInvulnerabilityTicks(int ticks) {
        return new HealthConfig(ticks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, cactusDamageEnabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    // ===== FALL DAMAGE =====

    public HealthConfig withFallDamage(boolean enabled) {
        return new HealthConfig(invulnerabilityTicks, enabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, cactusDamageEnabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    public HealthConfig withFallDamage(boolean enabled, float multiplier) {
        return new HealthConfig(invulnerabilityTicks, enabled, multiplier,
                fireDamageEnabled, fireDamageMultiplier, cactusDamageEnabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    public HealthConfig withFallDamageMultiplier(float multiplier) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, multiplier,
                fireDamageEnabled, fireDamageMultiplier, cactusDamageEnabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    // ===== FIRE DAMAGE =====

    public HealthConfig withFireDamage(boolean enabled) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                enabled, fireDamageMultiplier, cactusDamageEnabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    public HealthConfig withFireDamage(boolean enabled, float multiplier) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                enabled, multiplier, cactusDamageEnabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    public HealthConfig withFireDamageMultiplier(float multiplier) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, multiplier, cactusDamageEnabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    // ===== CACTUS DAMAGE =====

    public HealthConfig withCactusDamage(boolean enabled) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, enabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    public HealthConfig withCactusDamage(boolean enabled, float multiplier) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, enabled, multiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    public HealthConfig withCactusDamageMultiplier(float multiplier) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, cactusDamageEnabled, multiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    // ===== DAMAGE REPLACEMENT =====

    public HealthConfig withDamageReplacement(boolean enabled, boolean knockback) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, cactusDamageEnabled, cactusDamageMultiplier,
                enabled, knockback, logReplacementDamage,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    public HealthConfig withLogReplacementDamage(boolean log) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, cactusDamageEnabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, log,
                regenerationEnabled, regenerationAmount, regenerationIntervalTicks);
    }

    // ===== REGENERATION =====

    public HealthConfig withRegeneration(boolean enabled) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, cactusDamageEnabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                enabled, regenerationAmount, regenerationIntervalTicks);
    }

    public HealthConfig withRegeneration(boolean enabled, float amount, int intervalTicks) {
        return new HealthConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, cactusDamageEnabled, cactusDamageMultiplier,
                damageReplacementEnabled, knockbackOnReplacement, logReplacementDamage,
                enabled, amount, intervalTicks);
    }
}


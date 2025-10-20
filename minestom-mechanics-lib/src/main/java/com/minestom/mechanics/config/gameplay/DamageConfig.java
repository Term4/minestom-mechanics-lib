package com.minestom.mechanics.config.gameplay;

/**
 * Damage system configuration record.
 * Replaces the old builder-based DamageConfig class.
 *
 * Usage:
 * <pre>
 * DamageConfig damage = DamagePresets.MINEMEN
 *     .withInvulnerabilityTicks(15)
 *     .withFallDamage(false);
 * </pre>
 */
public record DamageConfig(
        // Invulnerability
        int invulnerabilityTicks,

        // Environmental damage
        boolean fallDamageEnabled,
        float fallDamageMultiplier,
        boolean fireDamageEnabled,
        float fireDamageMultiplier,

        // Damage replacement system
        boolean damageReplacementEnabled,
        boolean knockbackOnReplacement,
        boolean logReplacementDamage
) {

    // Validation
    public DamageConfig {
        if (invulnerabilityTicks < 0)
            throw new IllegalArgumentException("Invulnerability ticks must be >= 0");
        if (fallDamageMultiplier < 0)
            throw new IllegalArgumentException("Fall damage multiplier must be >= 0");
        if (fireDamageMultiplier < 0)
            throw new IllegalArgumentException("Fire damage multiplier must be >= 0");
    }

    // ===== COMPATIBILITY GETTERS (match old class) =====
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

    public DamageConfig withInvulnerabilityTicks(int ticks) {
        return new DamageConfig(ticks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage);
    }

    // ===== FALL DAMAGE =====

    public DamageConfig withFallDamage(boolean enabled) {
        return new DamageConfig(invulnerabilityTicks, enabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage);
    }

    public DamageConfig withFallDamage(boolean enabled, float multiplier) {
        return new DamageConfig(invulnerabilityTicks, enabled, multiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage);
    }

    public DamageConfig withFallDamageMultiplier(float multiplier) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, multiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage);
    }

    // ===== FIRE DAMAGE =====

    public DamageConfig withFireDamage(boolean enabled) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                enabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage);
    }

    public DamageConfig withFireDamage(boolean enabled, float multiplier) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                enabled, multiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage);
    }

    public DamageConfig withFireDamageMultiplier(float multiplier) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, multiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage);
    }

    // ===== DAMAGE REPLACEMENT =====

    public DamageConfig withDamageReplacement(boolean enabled, boolean knockback) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, enabled, knockback,
                logReplacementDamage);
    }

    public DamageConfig withLogReplacementDamage(boolean log) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, log);
    }
}
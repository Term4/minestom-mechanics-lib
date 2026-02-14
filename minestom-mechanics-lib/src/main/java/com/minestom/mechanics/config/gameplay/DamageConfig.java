package com.minestom.mechanics.config.gameplay;

import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;
import com.minestom.mechanics.systems.health.damage.util.DamageOverride;
import net.minestom.server.instance.Instance;

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
        boolean logReplacementDamage,

        // Invulnerability buffer for attacker-based damage (melee, projectiles). Hits in last N ticks scheduled for when invuln ends.
        int attackerInvulnerabilityBufferTicks
) {

    // Validation
    public DamageConfig {
        if (invulnerabilityTicks < 0)
            throw new IllegalArgumentException("Invulnerability ticks must be >= 0");
        if (fallDamageMultiplier < 0)
            throw new IllegalArgumentException("Fall damage multiplier must be >= 0");
        if (fireDamageMultiplier < 0)
            throw new IllegalArgumentException("Fire damage multiplier must be >= 0");
        if (attackerInvulnerabilityBufferTicks < 0)
            throw new IllegalArgumentException("Attacker invulnerability buffer ticks must be >= 0");
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
                knockbackOnReplacement, logReplacementDamage, attackerInvulnerabilityBufferTicks);
    }

    public DamageConfig withAttackerInvulnerabilityBufferTicks(int ticks) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage, ticks);
    }

    // ===== FALL DAMAGE =====

    public DamageConfig withFallDamage(boolean enabled) {
        return new DamageConfig(invulnerabilityTicks, enabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage, attackerInvulnerabilityBufferTicks);
    }

    public DamageConfig withFallDamage(boolean enabled, float multiplier) {
        return new DamageConfig(invulnerabilityTicks, enabled, multiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage, attackerInvulnerabilityBufferTicks);
    }

    public DamageConfig withFallDamageMultiplier(float multiplier) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, multiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage, attackerInvulnerabilityBufferTicks);
    }

    // ===== FIRE DAMAGE =====

    public DamageConfig withFireDamage(boolean enabled) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                enabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage, attackerInvulnerabilityBufferTicks);
    }

    public DamageConfig withFireDamage(boolean enabled, float multiplier) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                enabled, multiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage, attackerInvulnerabilityBufferTicks);
    }

    public DamageConfig withFireDamageMultiplier(float multiplier) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, multiplier, damageReplacementEnabled,
                knockbackOnReplacement, logReplacementDamage, attackerInvulnerabilityBufferTicks);
    }

    // ===== DAMAGE REPLACEMENT =====

    public DamageConfig withDamageReplacement(boolean enabled, boolean knockback) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, enabled, knockback,
                logReplacementDamage, attackerInvulnerabilityBufferTicks);
    }

    public DamageConfig withLogReplacementDamage(boolean log) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, log, attackerInvulnerabilityBufferTicks);
    }

    // ===== INVULNERABILITY BUFFER APPLICATION =====

    /**
     * Applies invulnerability buffer settings to a world via HealthSystem tags.
     * Applies to all attacker-based damage (melee, arrows, thrown projectiles).
     * Call this after HealthSystem is initialized and before players spawn.
     */
    public void applyInvulnerabilityBuffersTo(Instance world) {
        if (attackerInvulnerabilityBufferTicks <= 0) return;
        var props = DamageTypeProperties.ATTACK_DEFAULT.withInvulnerabilityBufferTicks(attackerInvulnerabilityBufferTicks);
        var override = DamageOverride.override(props);
        var meleeTag = HealthSystem.tag("melee");
        var arrowTag = HealthSystem.tag("arrow");
        var thrownTag = HealthSystem.tag("thrown");
        if (meleeTag != null) world.setTag(meleeTag, override);
        if (arrowTag != null) world.setTag(arrowTag, override);
        if (thrownTag != null) world.setTag(thrownTag, override);
    }
}
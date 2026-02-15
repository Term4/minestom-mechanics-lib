package com.minestom.mechanics.config.gameplay;

import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.config.health.DamageTypeProperties;
import com.minestom.mechanics.systems.health.damage.util.DamageOverride;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;

/**
 * Damage system configuration record.
 * Replaces the old builder-based DamageConfig class.
 *
 * <p>To detect replacement hits in custom logic, use
 * {@link HealthSystem#wasLastDamageReplacement(LivingEntity)} or
 * {@link com.minestom.mechanics.systems.health.damage.DamageResult#wasReplacement()}.</p>
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
        float replacementCutoff,
        boolean hurtEffect
) {

    // Validation
    public DamageConfig {
        if (invulnerabilityTicks < 0)
            throw new IllegalArgumentException("Invulnerability ticks must be >= 0");
        if (fallDamageMultiplier < 0)
            throw new IllegalArgumentException("Fall damage multiplier must be >= 0");
        if (fireDamageMultiplier < 0)
            throw new IllegalArgumentException("Fire damage multiplier must be >= 0");
        if (replacementCutoff < 0)
            throw new IllegalArgumentException("Replacement cutoff must be >= 0");
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

    // ===== INVULNERABILITY =====

    public DamageConfig withInvulnerabilityTicks(int ticks) {
        return new DamageConfig(ticks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, replacementCutoff, hurtEffect);
    }

    public DamageConfig withReplacementCutoff(float cutoff) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, cutoff, hurtEffect);
    }

    public DamageConfig withHurtEffect(boolean hurtEffect) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, replacementCutoff, hurtEffect);
    }

    public DamageConfig withNoReplacementSameItem(boolean noReplacementSameItem) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, replacementCutoff, hurtEffect);
    }

    // ===== FALL DAMAGE =====

    public DamageConfig withFallDamage(boolean enabled) {
        return new DamageConfig(invulnerabilityTicks, enabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, replacementCutoff, hurtEffect);
    }

    public DamageConfig withFallDamage(boolean enabled, float multiplier) {
        return new DamageConfig(invulnerabilityTicks, enabled, multiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, replacementCutoff, hurtEffect);
    }

    public DamageConfig withFallDamageMultiplier(float multiplier) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, multiplier,
                fireDamageEnabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, replacementCutoff, hurtEffect);
    }

    // ===== FIRE DAMAGE =====

    public DamageConfig withFireDamage(boolean enabled) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                enabled, fireDamageMultiplier, damageReplacementEnabled,
                knockbackOnReplacement, replacementCutoff, hurtEffect);
    }

    public DamageConfig withFireDamage(boolean enabled, float multiplier) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                enabled, multiplier, damageReplacementEnabled,
                knockbackOnReplacement, replacementCutoff, hurtEffect);
    }

    public DamageConfig withFireDamageMultiplier(float multiplier) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, multiplier, damageReplacementEnabled,
                knockbackOnReplacement, replacementCutoff, hurtEffect);
    }

    // ===== DAMAGE REPLACEMENT =====

    public DamageConfig withDamageReplacement(boolean enabled, boolean knockback) {
        return new DamageConfig(invulnerabilityTicks, fallDamageEnabled, fallDamageMultiplier,
                fireDamageEnabled, fireDamageMultiplier, enabled, knockback,
                replacementCutoff, hurtEffect);
    }

    // ===== INVULNERABILITY BUFFER APPLICATION =====

    /**
     * Applies invulnerability buffer and damage replacement settings to a world via HealthSystem tags.
     * Applies to all attacker-based damage (melee, arrows, thrown projectiles).
     * Uses replacementCutoff and hurtEffect from this config; noReplacementSameItem and
     * attackerInvulnerabilityBufferTicks from combat config.
     * Call this after HealthSystem is initialized and before players spawn.
     * <p><b>Override resolution:</b> Item &gt; Attacker &gt; Player &gt; World &gt; Server Default</p>
     *
     * @param world the instance to apply tags to
     * @param combat combat config for hit queue settings; if null, uses bufferTicks=0 and noReplacementSameItem=false
     */
    public void applyInvulnerabilityBuffersTo(Instance world, @Nullable CombatConfig combat) {
        int bufferTicks = combat != null ? combat.attackerInvulnerabilityBufferTicks() : 0;
        boolean sameItem = combat != null && combat.noReplacementSameItem();
        var props = DamageTypeProperties.ATTACK_DEFAULT
                .withInvulnerabilityBufferTicks(bufferTicks)
                .withReplacementCutoff(replacementCutoff)
                .withHurtEffect(hurtEffect)
                .withNoReplacementSameItem(sameItem);
        var override = DamageOverride.override(props);
        var meleeTag = HealthSystem.tag("melee");
        var arrowTag = HealthSystem.tag("arrow");
        var thrownTag = HealthSystem.tag("thrown");
        if (meleeTag != null) world.setTag(meleeTag, override);
        if (arrowTag != null) world.setTag(arrowTag, override);
        if (thrownTag != null) world.setTag(thrownTag, override);
    }
}
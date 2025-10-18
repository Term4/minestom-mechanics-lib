package com.minestom.mechanics.config.gameplay;

import static com.minestom.mechanics.config.combat.CombatConstants.*;

// TODO: Linked with CombatModeBundle's todo

/**
 * Configuration for damage system.
 * ✅ REFACTORED: All default values and validation use constants
 */
public class DamageConfig {
    private boolean fallDamageEnabled = true;
    private float fallDamageMultiplier = DEFAULT_FALL_DAMAGE_MULTIPLIER;
    private boolean fireDamageEnabled = true;
    private float fireDamageMultiplier = DEFAULT_FIRE_DAMAGE_MULTIPLIER;

    // ✅ REFACTORED: Using constant for default value
    private int invulnerabilityTicks = DEFAULT_INVULNERABILITY_TICKS;

    private boolean damageReplacementEnabled = DEFAULT_DAMAGE_REPLACEMENT;
    private boolean knockbackOnReplacement = DEFAULT_KNOCKBACK_ON_REPLACEMENT;
    private boolean logReplacementDamage = true; // Log replacement damage by default

    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Default Minecraft-like damage configuration
     */
    public static DamageConfig defaultConfig() {
        return builder()
            .fallDamage(true, DEFAULT_FALL_DAMAGE_MULTIPLIER)
            .fireDamage(true, DEFAULT_FIRE_DAMAGE_MULTIPLIER)
            .invulnerabilityTicks(DEFAULT_INVULNERABILITY_TICKS)
            .damageReplacement(DEFAULT_DAMAGE_REPLACEMENT, DEFAULT_KNOCKBACK_ON_REPLACEMENT)
            .build();
    }
    
    /**
     * Minecraft 1.8 style configuration
     */
    public static DamageConfig minecraft18() {
        return builder()
            .fallDamage(true, DEFAULT_FALL_DAMAGE_MULTIPLIER)
            .fireDamage(true, DEFAULT_FIRE_DAMAGE_MULTIPLIER)
            .invulnerabilityTicks(DEFAULT_INVULNERABILITY_TICKS)
            .damageReplacement(true, false)
            .build();
    }

    public static class Builder {
        private final DamageConfig config = new DamageConfig();

        // TODO: Introduce an abstract damage type class, then simplify this builder

        public Builder fallDamage(boolean enabled, float multiplier) {
            config.fallDamageEnabled = enabled;
            config.fallDamageMultiplier = multiplier;
            return this;
        }

        public Builder fireDamage(boolean enabled, float multiplier) {
            config.fireDamageEnabled = enabled;
            config.fireDamageMultiplier = multiplier;
            return this;
        }

        /**
         * Set victim invulnerability duration after taking damage.
         * @param ticks Number of ticks victim is invulnerable (20 ticks = 1 second)
         *              Use NO_INVULNERABILITY_TICKS for 1.8 combo style
         *              Use DEFAULT_INVULNERABILITY_TICKS for vanilla
         *              Use BALANCED_INVULNERABILITY_TICKS for balanced
         */
        public Builder invulnerabilityTicks(int ticks) {
            config.invulnerabilityTicks = ticks;
            return this;
        }

        /**
         * Configure damage replacement behavior.
         *
         * @param enableReplacement Allow stronger hits to override weaker damage during i-frames
         * @param applyKnockback Apply knockback on replacement hits (false = vanilla, true = MMC)
         */
        public Builder damageReplacement(boolean enableReplacement, boolean applyKnockback) {
            config.damageReplacementEnabled = enableReplacement;
            config.knockbackOnReplacement = applyKnockback;
            return this;
        }
        
        /**
         * Configure whether to log damage replacement events.
         *
         * @param log true to log replacement damage (default), false to suppress logs
         */
        public Builder logReplacementDamage(boolean log) {
            config.logReplacementDamage = log;
            return this;
        }

        public DamageConfig build() {
            return config;
        }
    }

    // Getters
    public boolean isFallDamageEnabled() { return fallDamageEnabled; }
    public float getFallDamageMultiplier() { return fallDamageMultiplier; }
    public boolean isFireDamageEnabled() { return fireDamageEnabled; }
    public float getFireDamageMultiplier() { return fireDamageMultiplier; }
    public int getInvulnerabilityTicks() { return invulnerabilityTicks; }
    public boolean isDamageReplacementEnabled() { return damageReplacementEnabled; }
    public boolean isKnockbackOnReplacement() { return knockbackOnReplacement; }
    public boolean isLogReplacementDamage() { return logReplacementDamage; }
}

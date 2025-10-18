package com.minestom.mechanics.config.blocking;

import static com.minestom.mechanics.config.combat.CombatConstants.*;

/**
 * BlockingConfig - Configuration for the blocking system.
 * ✅ REFACTORED: All defaults and validation use constants
 */
public class BlockingConfig {
    private boolean enabled = true;

    // ✅ REFACTORED: Using constants for defaults
    private double damageReduction = DEFAULT_BLOCKING_DAMAGE_REDUCTION;
    private double knockbackHorizontalMultiplier = DEFAULT_BLOCKING_KB_HORIZONTAL;
    private double knockbackVerticalMultiplier = DEFAULT_BLOCKING_KB_VERTICAL;

    private boolean showDamageMessages = true;
    private boolean showBlockEffects = true;

    private BlockingConfig() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BlockingConfig config = new BlockingConfig();

        public Builder enabled(boolean enabled) {
            config.enabled = enabled;
            return this;
        }

        /**
         * Set damage reduction with validation.
         * ✅ REFACTORED: Using constants for validation
         */
        public Builder damageReduction(double reduction) {
            if (reduction < MIN_BLOCKING_REDUCTION || reduction > MAX_BLOCKING_REDUCTION) {
                throw new IllegalArgumentException(
                        "Damage reduction must be between " + MIN_BLOCKING_REDUCTION +
                                " and " + MAX_BLOCKING_REDUCTION + ": " + reduction
                );
            }
            config.damageReduction = reduction;
            return this;
        }

        /**
         * Set knockback multipliers with validation.
         * ✅ REFACTORED: Using constants for validation
         */
        public Builder knockbackHorizontalMultiplier(double multiplier) {
            if (multiplier < MIN_BLOCKING_REDUCTION || multiplier > MAX_BLOCKING_REDUCTION) {
                throw new IllegalArgumentException(
                        "Knockback horizontal multiplier must be between " +
                                MIN_BLOCKING_REDUCTION + " and " + MAX_BLOCKING_REDUCTION +
                                ": " + multiplier
                );
            }
            config.knockbackHorizontalMultiplier = multiplier;
            return this;
        }

        public Builder knockbackVerticalMultiplier(double multiplier) {
            if (multiplier < MIN_BLOCKING_REDUCTION || multiplier > MAX_BLOCKING_REDUCTION) {
                throw new IllegalArgumentException(
                        "Knockback vertical multiplier must be between " +
                                MIN_BLOCKING_REDUCTION + " and " + MAX_BLOCKING_REDUCTION +
                                ": " + multiplier
                );
            }
            config.knockbackVerticalMultiplier = multiplier;
            return this;
        }

        public Builder showDamageMessages(boolean show) {
            config.showDamageMessages = show;
            return this;
        }

        public Builder showBlockEffects(boolean show) {
            config.showBlockEffects = show;
            return this;
        }

        public BlockingConfig build() {
            return config;
        }
    }

    // ===========================
    // GETTERS
    // ===========================

    public boolean isEnabled() {
        return enabled;
    }

    public double getDamageReduction() {
        return damageReduction;
    }

    public double getKnockbackHorizontalMultiplier() {
        return knockbackHorizontalMultiplier;
    }

    public double getKnockbackVerticalMultiplier() {
        return knockbackVerticalMultiplier;
    }

    public boolean isShowDamageMessages() {
        return showDamageMessages;
    }

    public boolean isShowBlockEffects() {
        return showBlockEffects;
    }

    // ===========================
    // SETTERS (for runtime configuration)
    // ===========================

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Set damage reduction with validation at runtime.
     * ✅ REFACTORED: Using constants for validation
     */
    public void setDamageReduction(double damageReduction) {
        if (damageReduction < MIN_BLOCKING_REDUCTION || damageReduction > MAX_BLOCKING_REDUCTION) {
            throw new IllegalArgumentException(
                    "Damage reduction must be between " + MIN_BLOCKING_REDUCTION +
                            " and " + MAX_BLOCKING_REDUCTION + ": " + damageReduction
            );
        }
        this.damageReduction = damageReduction;
    }

    public void setKnockbackHorizontalMultiplier(double multiplier) {
        if (multiplier < 0 || multiplier > 1) {
            throw new IllegalArgumentException(
                    "Knockback horizontal multiplier must be between 0.0 and 1.0: " + multiplier
            );
        }
        this.knockbackHorizontalMultiplier = multiplier;
    }

    public void setKnockbackVerticalMultiplier(double multiplier) {
        if (multiplier < 0 || multiplier > 1) {
            throw new IllegalArgumentException(
                    "Knockback vertical multiplier must be between 0.0 and 1.0: " + multiplier
            );
        }
        this.knockbackVerticalMultiplier = multiplier;
    }

    public void setShowDamageMessages(boolean show) {
        this.showDamageMessages = show;
    }

    public void setShowBlockEffects(boolean show) {
        this.showBlockEffects = show;
    }

    // ===========================
    // UTILITY
    // ===========================

    @Override
    public String toString() {
        return "BlockingConfig{" +
                "enabled=" + enabled +
                ", damageReduction=" + (damageReduction * 100) + "%" +
                ", knockbackHorizontalMultiplier=" + knockbackHorizontalMultiplier +
                ", knockbackVerticalMultiplier=" + knockbackVerticalMultiplier +
                ", showDamageMessages=" + showDamageMessages +
                ", showBlockEffects=" + showBlockEffects +
                '}';
    }
}

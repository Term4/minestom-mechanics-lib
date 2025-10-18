package com.minestom.mechanics.config.combat;

import com.minestom.mechanics.features.knockback.KnockbackProfile;

import static com.minestom.mechanics.config.combat.CombatConstants.*;

/**
 * Configuration for pure combat gameplay rules.
 * Contains only combat mechanics, no hit detection or world interaction settings.
 * 
 * This class focuses on the core combat experience:
 * - Attack timing and cooldowns
 * - Knockback behavior
 * - Critical hit mechanics
 * - Sprint bonus systems
 */
public class CombatRulesConfig {
    
    // Combat timing
    private final boolean removeAttackCooldown; // For 1.8 style instant attacks
    
    // Knockback and critical settings
    private final KnockbackProfile knockbackProfile;
    private final float criticalMultiplier;
    private final boolean allowSprintCrits;
    
    // Sprint bonus window configuration
    private final boolean dynamicSprintWindow;
    private final boolean sprintWindowDouble;
    private final int sprintWindowMaxTicks;
    
    private CombatRulesConfig(Builder builder) {
        this.removeAttackCooldown = builder.removeAttackCooldown;
        this.knockbackProfile = builder.knockbackProfile;
        this.criticalMultiplier = builder.criticalMultiplier;
        this.allowSprintCrits = builder.allowSprintCrits;
        this.dynamicSprintWindow = builder.dynamicSprintWindow;
        this.sprintWindowDouble = builder.sprintWindowDouble;
        this.sprintWindowMaxTicks = builder.sprintWindowMaxTicks;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public boolean shouldRemoveAttackCooldown() {
        return removeAttackCooldown;
    }
    
    public KnockbackProfile getKnockbackProfile() {
        return knockbackProfile;
    }
    
    public float getCriticalMultiplier() {
        return criticalMultiplier;
    }
    
    public boolean isAllowSprintCrits() {
        return allowSprintCrits;
    }
    
    public boolean isDynamicSprintWindow() {
        return dynamicSprintWindow;
    }
    
    public boolean isSprintWindowDouble() {
        return sprintWindowDouble;
    }
    
    public int getSprintWindowMaxTicks() {
        return sprintWindowMaxTicks;
    }
    
    public static class Builder {
        private boolean removeAttackCooldown = true; // Default to 1.8 style
        private KnockbackProfile knockbackProfile = KnockbackProfile.VANILLA;
        private float criticalMultiplier = CRITICAL_HIT_MULTIPLIER;
        private boolean allowSprintCrits = true;
        private boolean dynamicSprintWindow = true;
        private boolean sprintWindowDouble = false;
        private int sprintWindowMaxTicks = DEFAULT_SPRINT_WINDOW_MAX_TICKS;
        
        /**
         * Set whether to remove attack cooldown (1.8 style instant attacks).
         * When true, players can attack as fast as they can click.
         * When false, vanilla attack speed mechanics apply.
         */
        public Builder removeAttackCooldown(boolean remove) {
            this.removeAttackCooldown = remove;
            return this;
        }
        
        /**
         * Set knockback profile.
         */
        public Builder knockback(KnockbackProfile profile) {
            if (profile == null) {
                throw new IllegalArgumentException(
                        "Knockback profile cannot be null - use KnockbackProfile.VANILLA for default");
            }
            
            this.knockbackProfile = profile;
            return this;
        }
        
        /**
         * Set critical multiplier with validation.
         */
        public Builder criticalMultiplier(float multiplier) {
            if (multiplier < MIN_CRITICAL_MULTIPLIER) {
                throw new IllegalArgumentException(
                        "Critical multiplier must be at least " + MIN_CRITICAL_MULTIPLIER + ": " + multiplier);
            }
            if (multiplier > MAX_CRITICAL_MULTIPLIER) {
                throw new IllegalArgumentException(
                        "Critical multiplier too high (max " + MAX_CRITICAL_MULTIPLIER + "): " + multiplier);
            }
            
            this.criticalMultiplier = multiplier;
            return this;
        }
        
        /**
         * Configure whether critical hits can happen while sprinting.
         *
         * @param allow true = allow sprint crits (non-vanilla, common in PvP servers)
         *              false = vanilla 1.8 behavior (sprint prevents crits)
         */
        public Builder allowSprintCrits(boolean allow) {
            this.allowSprintCrits = allow;
            return this;
        }
        
        /**
         * Configure sprint bonus window settings.
         * Compensates for packet ordering issues with high ping players.
         *
         * @param dynamic true = window scales with player ping, false = fixed window for all
         * @param doublePing Whether to double the ping-based window (ignored if dynamic=false)
         * @param maxTicks Fixed window size (if dynamic=false) or maximum size (if dynamic=true)
         */
        public Builder sprintWindow(boolean dynamic, boolean doublePing, int maxTicks) {
            if (maxTicks < 1) {
                throw new IllegalArgumentException("Sprint window max ticks must be at least 1: " + maxTicks);
            }
            if (maxTicks > 20) {
                throw new IllegalArgumentException(
                        "Sprint window max ticks too high (max 20 = 1 second): " + maxTicks);
            }
            
            this.dynamicSprintWindow = dynamic;
            this.sprintWindowDouble = doublePing;
            this.sprintWindowMaxTicks = maxTicks;
            return this;
        }
        
        public CombatRulesConfig build() {
            return new CombatRulesConfig(this);
        }
    }
}

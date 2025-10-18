package com.minestom.mechanics.config.combat;

import com.minestom.mechanics.config.blocking.BlockingConfig;

// TODO: I like this general idea of being able to set one mechanics package instantly, but this method of implementing such a feature is hacky at best
//  and could cause conflicts. Most likely is NOT thread safe either (bad for reliability in production)
//  Alternative way to do this would be to overhaul the entire mechanics system to be more independent, i.e. if a feature is set individually it takes priority
//  or something. But I need to look more into how to accomplish this without hacky solutions.

// TODO: ACTUALLY thinking back to it, I think MinestomPvP's combat featureset did this fairly well. Could take some inspiration from that

/**
 * Java record to bundle all combat-related configurations together.
 * This replaces the old CombatMode enum's bundling role.
 * 
 * Contains all the configuration needed for a complete combat mode:
 * - Combat rules (timing, knockback, critical hits)
 * - Blocking mechanics
 * - Projectile behavior
 * 
 * NOTE: Damage and hit detection configuration are now managed by GameplayConfig and HitboxManager to avoid duplication
 * 
 * @param name the display name of this combat mode
 * @param description a description of this combat mode
 * @param combatRules the combat rules configuration
 * @param blocking the blocking configuration
 * @param projectiles the projectile configuration
 */
public record CombatModeBundle(
        String name,
        String description,
        CombatRulesConfig combatRules,
        BlockingConfig blocking,
        ProjectileConfig projectiles
) {
    
    /**
     * Create a new CombatModeBundle with the given configurations.
     * 
     * @param name Display name for this combat mode
     * @param description Description of this combat mode
     * @param combatRules Combat rules configuration
     * @param blocking Blocking configuration
     * @param projectiles Projectile configuration
     * @throws IllegalArgumentException if any required config is null
     */
    public CombatModeBundle {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (combatRules == null) {
            throw new IllegalArgumentException("Combat rules config cannot be null");
        }
        if (blocking == null) {
            throw new IllegalArgumentException("Blocking config cannot be null");
        }
        if (projectiles == null) {
            throw new IllegalArgumentException("Projectile config cannot be null");
        }
    }
    
    /**
     * Create a builder for CombatModeBundle.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for CombatModeBundle.
     */
    public static class Builder {
        private String name;
        private String description;
        private CombatRulesConfig combatRules;
        private BlockingConfig blocking;
        private ProjectileConfig projectiles;
        
        /**
         * Set the name of this combat mode.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Set the description of this combat mode.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Set the combat rules configuration.
         */
        public Builder combatRules(CombatRulesConfig combatRules) {
            this.combatRules = combatRules;
            return this;
        }
        
        /**
         * Set the blocking configuration.
         */
        public Builder blocking(BlockingConfig blocking) {
            this.blocking = blocking;
            return this;
        }
        
        /**
         * Set the projectile configuration.
         */
        public Builder projectiles(ProjectileConfig projectiles) {
            this.projectiles = projectiles;
            return this;
        }
        
        /**
         * Build the CombatModeBundle.
         * 
         * @return a new CombatModeBundle with the configured settings
         * @throws IllegalArgumentException if any required field is null
         */
        public CombatModeBundle build() {
            return new CombatModeBundle(
                    name, description, combatRules, 
                    blocking, projectiles
            );
        }
    }
}

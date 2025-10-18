package com.minestom.mechanics.config.combat;

import com.minestom.mechanics.projectile.config.FishingRodConfig;
import com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig;
import com.minestom.mechanics.projectile.config.FishingRodKnockbackConfig;

/**
 * Configuration for all projectile-related settings.
 * Bundles together knockback configs for different projectile types.
 */
public class ProjectileConfig {
    
    // Individual projectile knockback configs
    private final ProjectileKnockbackConfig arrowKnockbackConfig;
    private final ProjectileKnockbackConfig snowballKnockbackConfig;
    private final ProjectileKnockbackConfig eggKnockbackConfig;
    private final ProjectileKnockbackConfig enderPearlKnockbackConfig;
    private final FishingRodKnockbackConfig fishingRodKnockbackConfig;
    
    // Fishing rod specific config
    private final FishingRodConfig fishingRodConfig;
    
    // Player momentum inheritance (modern feature, disabled for legacy 1.8)
    private final boolean inheritPlayerMomentum;
    
    private ProjectileConfig(Builder builder) {
        this.arrowKnockbackConfig = builder.arrowKnockbackConfig;
        this.snowballKnockbackConfig = builder.snowballKnockbackConfig;
        this.eggKnockbackConfig = builder.eggKnockbackConfig;
        this.enderPearlKnockbackConfig = builder.enderPearlKnockbackConfig;
        this.fishingRodKnockbackConfig = builder.fishingRodKnockbackConfig;
        this.fishingRodConfig = builder.fishingRodConfig;
        this.inheritPlayerMomentum = builder.inheritPlayerMomentum;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public ProjectileKnockbackConfig getArrowKnockbackConfig() {
        return arrowKnockbackConfig;
    }
    
    public ProjectileKnockbackConfig getSnowballKnockbackConfig() {
        return snowballKnockbackConfig;
    }
    
    public ProjectileKnockbackConfig getEggKnockbackConfig() {
        return eggKnockbackConfig;
    }
    
    public ProjectileKnockbackConfig getEnderPearlKnockbackConfig() {
        return enderPearlKnockbackConfig;
    }
    
    public FishingRodKnockbackConfig getFishingRodKnockbackConfig() {
        return fishingRodKnockbackConfig;
    }
    
    public FishingRodConfig getFishingRodConfig() {
        return fishingRodConfig;
    }
    
    public boolean shouldInheritPlayerMomentum() {
        return inheritPlayerMomentum;
    }
    
    public static class Builder {
        private ProjectileKnockbackConfig arrowKnockbackConfig = ProjectileKnockbackConfig.defaultArrowKnockback();
        private ProjectileKnockbackConfig snowballKnockbackConfig = ProjectileKnockbackConfig.defaultSnowballKnockback();
        private ProjectileKnockbackConfig eggKnockbackConfig = ProjectileKnockbackConfig.defaultEggKnockback();
        private ProjectileKnockbackConfig enderPearlKnockbackConfig = ProjectileKnockbackConfig.defaultEnderPearlKnockback();
        private FishingRodKnockbackConfig fishingRodKnockbackConfig = FishingRodKnockbackConfig.defaultConfig();
        private FishingRodConfig fishingRodConfig = FishingRodConfig.defaultConfig();
        private boolean inheritPlayerMomentum = false; // False by default for legacy 1.8 compatibility
        
        public Builder arrowKnockback(ProjectileKnockbackConfig config) {
            if (config == null) {
                throw new IllegalArgumentException("Arrow knockback config cannot be null");
            }
            this.arrowKnockbackConfig = config;
            return this;
        }
        
        public Builder snowballKnockback(ProjectileKnockbackConfig config) {
            if (config == null) {
                throw new IllegalArgumentException("Snowball knockback config cannot be null");
            }
            this.snowballKnockbackConfig = config;
            return this;
        }
        
        public Builder eggKnockback(ProjectileKnockbackConfig config) {
            if (config == null) {
                throw new IllegalArgumentException("Egg knockback config cannot be null");
            }
            this.eggKnockbackConfig = config;
            return this;
        }
        
        public Builder enderPearlKnockback(ProjectileKnockbackConfig config) {
            if (config == null) {
                throw new IllegalArgumentException("Ender pearl knockback config cannot be null");
            }
            this.enderPearlKnockbackConfig = config;
            return this;
        }
        
        public Builder fishingRodKnockback(FishingRodKnockbackConfig config) {
            if (config == null) {
                throw new IllegalArgumentException("Fishing rod knockback config cannot be null");
            }
            this.fishingRodKnockbackConfig = config;
            return this;
        }
        
        public Builder fishingRod(FishingRodConfig config) {
            if (config == null) {
                throw new IllegalArgumentException("Fishing rod config cannot be null");
            }
            this.fishingRodConfig = config;
            return this;
        }
        
        /**
         * Enable or disable player momentum inheritance for projectiles.
         * When enabled, projectiles inherit the player's velocity (modern behavior).
         * When disabled, projectiles use only the aim direction (legacy 1.8 behavior).
         * 
         * @param inheritPlayerMomentum true to enable (modern), false for legacy 1.8
         * @return this builder
         */
        public Builder inheritPlayerMomentum(boolean inheritPlayerMomentum) {
            this.inheritPlayerMomentum = inheritPlayerMomentum;
            return this;
        }
        
        public ProjectileConfig build() {
            return new ProjectileConfig(this);
        }
    }
}

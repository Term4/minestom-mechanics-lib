package com.minestom.mechanics.projectile.config;

// TODO: General projectile knockback config?? Why do we have one specific to
//  the fishing rod? Seems repetitive and unnecessary.

/**
 * Configuration for fishing rod bobber knockback settings.
 * Extends ProjectileKnockbackConfig with fishing rod specific options.
 */
public record FishingRodKnockbackConfig(
    ProjectileKnockbackConfig baseConfig,
    KnockbackMode mode
) {
    
    public enum KnockbackMode {
        BOBBER_RELATIVE,  // Current behavior - knockback relative to bobber position
        ORIGIN_RELATIVE   // New behavior - knockback relative to player/shooter position
    }
    
    // Delegate to base config
    public boolean isEnabled() { return baseConfig.enabled(); }
    public double getHorizontalKnockback() { return baseConfig.horizontalKnockback(); }
    public double getVerticalKnockback() { return baseConfig.verticalKnockback(); }
    public double getVerticalLimit() { return baseConfig.verticalLimit(); }
    
    // Default configurations
    public static FishingRodKnockbackConfig defaultConfig() {
        return new FishingRodKnockbackConfig(
            new ProjectileKnockbackConfig(true, 0.525, 0.365, 0.45),
            KnockbackMode.BOBBER_RELATIVE
        );
    }
    
    public static FishingRodKnockbackConfig originRelative() {
        return new FishingRodKnockbackConfig(
            new ProjectileKnockbackConfig(true, 0.3, 0.2, 0.3),
            KnockbackMode.ORIGIN_RELATIVE
        );
    }
    
    public static FishingRodKnockbackConfig disabled() {
        return new FishingRodKnockbackConfig(
            ProjectileKnockbackConfig.disabled(),
            KnockbackMode.BOBBER_RELATIVE
        );
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ProjectileKnockbackConfig baseConfig = new ProjectileKnockbackConfig(true, 0.3, 0.2, 0.3);
        private KnockbackMode mode = KnockbackMode.BOBBER_RELATIVE;
        
        public Builder baseConfig(ProjectileKnockbackConfig config) {
            this.baseConfig = config;
            return this;
        }
        
        public Builder mode(KnockbackMode mode) {
            this.mode = mode;
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            this.baseConfig = new ProjectileKnockbackConfig(
                enabled, 
                baseConfig.horizontalKnockback(),
                baseConfig.verticalKnockback(),
                baseConfig.verticalLimit()
            );
            return this;
        }
        
        public Builder horizontalKnockback(double knockback) {
            this.baseConfig = new ProjectileKnockbackConfig(
                baseConfig.enabled(),
                knockback,
                baseConfig.verticalKnockback(),
                baseConfig.verticalLimit()
            );
            return this;
        }
        
        public Builder verticalKnockback(double knockback) {
            this.baseConfig = new ProjectileKnockbackConfig(
                baseConfig.enabled(),
                baseConfig.horizontalKnockback(),
                knockback,
                baseConfig.verticalLimit()
            );
            return this;
        }
        
        public Builder verticalLimit(double limit) {
            this.baseConfig = new ProjectileKnockbackConfig(
                baseConfig.enabled(),
                baseConfig.horizontalKnockback(),
                baseConfig.verticalKnockback(),
                limit
            );
            return this;
        }
        
        public FishingRodKnockbackConfig build() {
            return new FishingRodKnockbackConfig(baseConfig, mode);
        }
    }
}

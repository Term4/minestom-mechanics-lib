package com.minestom.mechanics.projectile.config;

// TODO: Remove unnecessary default configurations, also the
//  builder could change on update, but for now this mostly looks
//  fine.

/**
 * Configuration for fishing rod cast strength and behavior.
 * Provides configurable multipliers for velocity components and spread.
 */
public record FishingRodConfig(
    double horizontalMultiplier,
    double verticalMultiplier,
    double spreadMultiplier,
    double powerMultiplier
) {
    
    // Default configurations
    public static FishingRodConfig defaultConfig() {
        return new FishingRodConfig(1.5, 1.5, 1.0, 2);
    }
    
    public static FishingRodConfig weakCast() {
        return new FishingRodConfig(0.5, 0.5, 1.0, 0.8);
    }
    
    public static FishingRodConfig strongCast() {
        return new FishingRodConfig(2, 2, 1.0, 2.5);
    }
    
    public static FishingRodConfig preciseCast() {
        return new FishingRodConfig(1.0, 1.0, 0.5, 1.5);
    }
    
    public static FishingRodConfig wildCast() {
        return new FishingRodConfig(1.0, 1.0, 2.0, 1.5);
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private double horizontalMultiplier = 1.0;
        private double verticalMultiplier = 1.0;
        private double spreadMultiplier = 1.0;
        private double powerMultiplier = 1.5;
        
        public Builder horizontalMultiplier(double multiplier) {
            this.horizontalMultiplier = multiplier;
            return this;
        }
        
        public Builder verticalMultiplier(double multiplier) {
            this.verticalMultiplier = multiplier;
            return this;
        }
        
        public Builder spreadMultiplier(double multiplier) {
            this.spreadMultiplier = multiplier;
            return this;
        }
        
        public Builder powerMultiplier(double multiplier) {
            this.powerMultiplier = multiplier;
            return this;
        }
        
        public FishingRodConfig build() {
            return new FishingRodConfig(horizontalMultiplier, verticalMultiplier, 
                                     spreadMultiplier, powerMultiplier);
        }
    }
}

package com.minestom.mechanics.config.gameplay;

/**
 * Configuration for player eye height enforcement.
 * Useful for normalizing cross-version reach mechanics.
 */
public record EyeHeightConfig(
    boolean enabled,
    double standingEyeHeight,
    double sneakingEyeHeight,
    double survivalReach,
    double creativeReach,
    boolean enforceBlockPlaceReach
) {
    
    // Presets
    public static final EyeHeightConfig MINECRAFT_1_8 = new EyeHeightConfig(
        true, 1.62, 1.54, 4.5, 5.0, true
    );
        
    public static final EyeHeightConfig VANILLA = new EyeHeightConfig(
        false, 1.62, 1.54, 4.5, 5.0, true
    );
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean enabled = false;
        private double standingEyeHeight = 1.62;
        private double sneakingEyeHeight = 1.54;
        private double survivalReach = 4.5;
        private double creativeReach = 5.0;
        private boolean enforceBlockPlaceReach = true;
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder standingEyeHeight(double height) {
            this.standingEyeHeight = height;
            return this;
        }
        
        public Builder sneakingEyeHeight(double height) {
            this.sneakingEyeHeight = height;
            return this;
        }
        
        public Builder survivalReach(double reach) {
            this.survivalReach = reach;
            return this;
        }
        
        public Builder creativeReach(double reach) {
            this.creativeReach = reach;
            return this;
        }
        
        public Builder enforceBlockPlaceReach(boolean enforce) {
            this.enforceBlockPlaceReach = enforce;
            return this;
        }
        
        public EyeHeightConfig build() {
            return new EyeHeightConfig(enabled, standingEyeHeight, sneakingEyeHeight, 
                                     survivalReach, creativeReach, enforceBlockPlaceReach);
        }
    }
}

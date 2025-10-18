package com.minestom.mechanics.config.gameplay;

/**
 * Configuration for player hitbox enforcement.
 * Control collision box dimensions and validation.
 */
public record HitboxConfig(
    boolean enforceFixed,
    double width,
    double height,
    boolean heightChangesOnSneak,
    double sneakingHeight,
    int validationIntervalTicks,
    boolean strictCollision
) {
    
    public HitboxConfig(boolean enforceFixed, double width, double height) {
        this(enforceFixed, width, height, true, height - 0.3, 5, false);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static HitboxConfig defaultConfig() {
        return VANILLA;
    }
    
    public static final HitboxConfig MINECRAFT_1_8 = new HitboxConfig(
        true, 0.6, 1.8, false, 1.5, 5, true
    );
        
    public static final HitboxConfig VANILLA = new HitboxConfig(
        false, 0.6, 1.8, true, 1.5, 5, false
    );
    
    public static class Builder {
        private boolean enforceFixed = false;
        private double width = 0.6;
        private double height = 1.8;
        private boolean heightChangesOnSneak = true;
        private double sneakingHeight = 1.5;
        private int validationIntervalTicks = 5;
        private boolean strictCollision = false;
        
        public Builder enforceFixed(boolean enforce) {
            this.enforceFixed = enforce;
            return this;
        }
        
        public Builder width(double width) {
            this.width = width;
            return this;
        }
        
        public Builder height(double height) {
            this.height = height;
            return this;
        }
        
        public Builder heightChangesOnSneak(boolean changes) {
            this.heightChangesOnSneak = changes;
            return this;
        }
        
        public Builder sneakingHeight(double height) {
            this.sneakingHeight = height;
            return this;
        }
        
        public Builder validationIntervalTicks(int ticks) {
            this.validationIntervalTicks = ticks;
            return this;
        }
        
        public Builder strictCollision(boolean strict) {
            this.strictCollision = strict;
            return this;
        }
        
        public HitboxConfig build() {
            return new HitboxConfig(enforceFixed, width, height, heightChangesOnSneak,
                                  sneakingHeight, validationIntervalTicks, strictCollision);
        }
    }
}

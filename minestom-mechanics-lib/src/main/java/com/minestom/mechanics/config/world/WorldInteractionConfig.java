package com.minestom.mechanics.config.world;

import static com.minestom.mechanics.config.combat.CombatConstants.*;


// TODO: Is this necessary? I feel like we could move functionality
//  over to other classes and clean up the codebase significantly.

/**
 * Configuration for world/environment interaction settings.
 * Contains block interaction reach and raycasting settings.
 * 
 * This class focuses on server-wide world interaction settings:
 * - Block breaking/placing reach distances
 * - Raycasting precision settings
 * 
 * Note: These settings are server-wide, not per-combat-mode, since
 * world interaction is independent of combat mechanics.
 */
public record WorldInteractionConfig(
    double creativeBlockReach,
    double survivalBlockReach,
    double blockRaycastStep
) {
    
    public static Builder builder() {
        return new Builder();
    }

    // TODO: This is what I was talking about in the eye height config.
    //  Seems to be duplicate logic (block reach)

    /**
     * Get the appropriate block reach for the given game mode.
     * 
     * @param isCreative true for creative mode, false for survival
     * @return the appropriate reach distance
     */
    public double getBlockReachForMode(boolean isCreative) {
        return isCreative ? creativeBlockReach : survivalBlockReach;
    }
    
    public static class Builder {
        private double creativeBlockReach = CREATIVE_REACH;
        private double survivalBlockReach = SURVIVAL_REACH;
        private double blockRaycastStep = BLOCK_RAYCAST_STEP;
        
        /**
         * Configure block interaction reach settings.
         * 
         * @param creative Creative mode block reach
         * @param survival Survival mode block reach
         */
        public Builder blockReach(double creative, double survival) {
            if (creative <= 0) {
                throw new IllegalArgumentException("Creative block reach must be positive: " + creative);
            }
            if (survival <= 0) {
                throw new IllegalArgumentException("Survival block reach must be positive: " + survival);
            }
            if (creative > 20.0) {
                throw new IllegalArgumentException("Creative block reach too high (max 20): " + creative);
            }
            if (survival > 20.0) {
                throw new IllegalArgumentException("Survival block reach too high (max 20): " + survival);
            }
            
            this.creativeBlockReach = creative;
            this.survivalBlockReach = survival;
            return this;
        }
        
        /**
         * Set only the creative block reach.
         */
        public Builder creativeBlockReach(double reach) {
            if (reach <= 0) {
                throw new IllegalArgumentException("Creative block reach must be positive: " + reach);
            }
            if (reach > 20.0) {
                throw new IllegalArgumentException("Creative block reach too high (max 20): " + reach);
            }
            
            this.creativeBlockReach = reach;
            return this;
        }
        
        /**
         * Set only the survival block reach.
         */
        public Builder survivalBlockReach(double reach) {
            if (reach <= 0) {
                throw new IllegalArgumentException("Survival block reach must be positive: " + reach);
            }
            if (reach > 20.0) {
                throw new IllegalArgumentException("Survival block reach too high (max 20): " + reach);
            }
            
            this.survivalBlockReach = reach;
            return this;
        }

        // TODO: I like this, but I don't know if it should be in THIS class.
        //  Maybe have some sort of math, physics, or other sort of misc package for this kind of thing.
        //  We do similar things for combat reach and hit detection.

        /**
         * Set block raycast step size.
         * 
         * @param step Step size for block raycasting (smaller = more accurate but expensive)
         */
        public Builder blockRaycastStep(double step) {
            if (step <= 0) {
                throw new IllegalArgumentException("Block raycast step must be positive: " + step);
            }
            if (step > 1.0) {
                throw new IllegalArgumentException("Block raycast step too large (max 1.0): " + step);
            }
            
            this.blockRaycastStep = step;
            return this;
        }
        
        public WorldInteractionConfig build() {
            return new WorldInteractionConfig(creativeBlockReach, survivalBlockReach, blockRaycastStep);
        }
    }
}

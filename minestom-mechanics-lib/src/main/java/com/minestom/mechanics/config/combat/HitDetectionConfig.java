package com.minestom.mechanics.config.combat;

import static com.minestom.mechanics.config.combat.CombatConstants.*;

// TODO: Linked with CombatModeBundle's todo

/**
 * Configuration for hit detection and validation settings.
 * Contains reach distances and hitbox expansion for combat hit detection.
 * 
 * This class focuses on the technical aspects of hit detection:
 * - Server-side reach for raycasting
 * - Attack packet reach for anticheat validation
 * - Hitbox expansion for lag compensation
 */
public class HitDetectionConfig {
    
    // Reach settings
    private final double serverSideReach;
    private final double attackPacketReach;
    
    // Hitbox expansion settings
    private final double hitboxExpansionPrimary;
    private final double hitboxExpansionLimit;
    
    // Additional settings
    private final double angleThreshold;
    private final boolean enableAngleValidation;
    private final boolean trackHitSnapshots;
    
    private HitDetectionConfig(Builder builder) {
        this.serverSideReach = builder.serverSideReach;
        this.attackPacketReach = builder.attackPacketReach;
        this.hitboxExpansionPrimary = builder.hitboxExpansionPrimary;
        this.hitboxExpansionLimit = builder.hitboxExpansionLimit;
        this.angleThreshold = builder.angleThreshold;
        this.enableAngleValidation = builder.enableAngleValidation;
        this.trackHitSnapshots = builder.trackHitSnapshots;
    }
    
    public HitDetectionConfig(double serverSideReach, double attackPacketReach, double angleThreshold, boolean trackHitSnapshots) {
        this.serverSideReach = serverSideReach;
        this.attackPacketReach = attackPacketReach;
        this.hitboxExpansionPrimary = HITBOX_EXPANSION_PRIMARY;
        this.hitboxExpansionLimit = HITBOX_EXPANSION_LIMIT;
        this.angleThreshold = angleThreshold;
        this.enableAngleValidation = false; // Default to disabled
        this.trackHitSnapshots = trackHitSnapshots;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static HitDetectionConfig defaultConfig() {
        return builder().build();
    }
    
    // Getters
    /**
     * Get the reach used for server-side hit detection from swing packets.
     * This is the "fair" reach that gives all clients equal reach via raycasting.
     */
    public double getServerSideReach() {
        return serverSideReach;
    }
    
    /**
     * Get the maximum reach for validating client-sent attack packets.
     * This is an anticheat upper bound that's lenient to account for lag.
     */
    public double getAttackPacketReach() {
        return attackPacketReach;
    }
    
    /**
     * Get primary hitbox expansion (standard 1.8-style).
     */
    public double getHitboxExpansionPrimary() {
        return hitboxExpansionPrimary;
    }
    
    /**
     * Get maximum hitbox expansion for lag compensation.
     */
    public double getHitboxExpansionLimit() {
        return hitboxExpansionLimit;
    }
    
    /**
     * Get angle threshold for hit validation.
     */
    public double getAngleThreshold() {
        return angleThreshold;
    }
    
    /**
     * Check if angle validation is enabled.
     */
    public boolean isAngleValidationEnabled() {
        return enableAngleValidation;
    }
    
    /**
     * Check if hit snapshots should be tracked.
     */
    public boolean shouldTrackHitSnapshots() {
        return trackHitSnapshots;
    }
    
    public static class Builder {
        private double serverSideReach = SERVER_SIDE_REACH;
        private double attackPacketReach = ATTACK_PACKET_REACH;
        private double hitboxExpansionPrimary = HITBOX_EXPANSION_PRIMARY;
        private double hitboxExpansionLimit = HITBOX_EXPANSION_LIMIT;
        private double angleThreshold = 90.0; // Default angle threshold in degrees
        private boolean enableAngleValidation = false; // Default to disabled
        private boolean trackHitSnapshots = true; // Default to tracking snapshots
        
        /**
         * Configure reach settings with clear semantics.
         *
         * @param serverSideReach Reach for server-side raycasting (swings). This is the "fair"
         *                        reach given to all clients via server-side hit detection.
         *                        Typically 3.0 blocks for 1.8.9-style PvP.
         * @param attackPacketReach Maximum reach for client attack packets (anticheat upper bound).
         *                          More lenient to account for network lag and edge cases.
         *                          Typically 4.0-5.0 blocks.
         */
        public Builder reach(double serverSideReach, double attackPacketReach) {
            // Validate server-side reach
            if (serverSideReach <= 0) {
                throw new IllegalArgumentException("Server-side reach must be positive: " + serverSideReach);
            }
            if (serverSideReach < MIN_REACH_DISTANCE) {
                throw new IllegalArgumentException(
                        "Server-side reach too low (min " + MIN_REACH_DISTANCE + "): " + serverSideReach);
            }
            if (serverSideReach > MAX_REACH_VALIDATION) {
                throw new IllegalArgumentException(
                        "Server-side reach too high (max " + MAX_REACH_VALIDATION + "): " + serverSideReach);
            }
            
            // Validate attack packet reach
            if (attackPacketReach <= 0) {
                throw new IllegalArgumentException("Attack packet reach must be positive: " + attackPacketReach);
            }
            if (attackPacketReach > MAX_REACH_VALIDATION) {
                throw new IllegalArgumentException(
                        "Attack packet reach too high (max " + MAX_REACH_VALIDATION + "): " + attackPacketReach);
            }
            
            // Ensure attack packet reach >= server-side reach
            if (serverSideReach > attackPacketReach) {
                throw new IllegalArgumentException(
                        "Server-side reach cannot exceed attack packet reach: " +
                                serverSideReach + " > " + attackPacketReach);
            }
            
            this.serverSideReach = serverSideReach;
            this.attackPacketReach = attackPacketReach;
            return this;
        }
        
        /**
         * Set only the server-side reach (for swings/raycasting).
         * Attack packet reach remains at its current value.
         */
        public Builder serverSideReach(double reach) {
            if (reach <= 0) {
                throw new IllegalArgumentException("Server-side reach must be positive: " + reach);
            }
            if (reach < MIN_REACH_DISTANCE) {
                throw new IllegalArgumentException(
                        "Server-side reach too low (min " + MIN_REACH_DISTANCE + "): " + reach);
            }
            if (reach > MAX_REACH_VALIDATION) {
                throw new IllegalArgumentException(
                        "Server-side reach too high (max " + MAX_REACH_VALIDATION + "): " + reach);
            }
            
            this.serverSideReach = reach;
            return this;
        }
        
        /**
         * Set only the attack packet reach (anticheat upper bound).
         * Server-side reach remains at its current value.
         */
        public Builder attackPacketReach(double reach) {
            if (reach <= 0) {
                throw new IllegalArgumentException("Attack packet reach must be positive: " + reach);
            }
            if (reach > MAX_REACH_VALIDATION) {
                throw new IllegalArgumentException(
                        "Attack packet reach too high (max " + MAX_REACH_VALIDATION + "): " + reach);
            }
            
            this.attackPacketReach = reach;
            return this;
        }
        
        /**
         * Configure hitbox expansion settings.
         * 
         * @param primary Primary hitbox expansion (0.1 = 1.8 client standard)
         * @param limit Maximum hitbox expansion for lag compensation
         */
        public Builder hitboxExpansion(double primary, double limit) {
            if (primary < 0.0 || primary > 0.5) {
                throw new IllegalArgumentException("Primary hitbox expansion must be between 0.0 and 0.5: " + primary);
            }
            if (limit < 0.0 || limit > 0.5) {
                throw new IllegalArgumentException("Limit hitbox expansion must be between 0.0 and 0.5: " + limit);
            }
            if (primary > limit) {
                throw new IllegalArgumentException("Primary expansion cannot exceed limit: " + primary + " > " + limit);
            }
            
            this.hitboxExpansionPrimary = primary;
            this.hitboxExpansionLimit = limit;
            return this;
        }
        
        /**
         * Set only the primary hitbox expansion.
         */
        public Builder hitboxExpansionPrimary(double expansion) {
            if (expansion < 0.0 || expansion > 0.5) {
                throw new IllegalArgumentException("Primary hitbox expansion must be between 0.0 and 0.5: " + expansion);
            }
            if (expansion > this.hitboxExpansionLimit) {
                throw new IllegalArgumentException("Primary expansion cannot exceed limit: " + expansion + " > " + this.hitboxExpansionLimit);
            }
            
            this.hitboxExpansionPrimary = expansion;
            return this;
        }
        
        /**
         * Set only the limit hitbox expansion.
         */
        public Builder hitboxExpansionLimit(double expansion) {
            if (expansion < 0.0 || expansion > 0.5) {
                throw new IllegalArgumentException("Limit hitbox expansion must be between 0.0 and 0.5: " + expansion);
            }
            if (this.hitboxExpansionPrimary > expansion) {
                throw new IllegalArgumentException("Primary expansion cannot exceed limit: " + this.hitboxExpansionPrimary + " > " + expansion);
            }
            
            this.hitboxExpansionLimit = expansion;
            return this;
        }
        
        /**
         * Configure angle validation settings.
         * 
         * @param enabled Whether angle validation is enabled
         * @param threshold Angle threshold in degrees (0-180)
         */
        public Builder angleValidation(boolean enabled, double threshold) {
            if (threshold < 0.0 || threshold > 180.0) {
                throw new IllegalArgumentException("Angle threshold must be between 0.0 and 180.0: " + threshold);
            }
            
            this.enableAngleValidation = enabled;
            this.angleThreshold = threshold;
            return this;
        }
        
        /**
         * Enable angle validation with default threshold (90 degrees).
         */
        public Builder enableAngleValidation() {
            this.enableAngleValidation = true;
            return this;
        }
        
        /**
         * Disable angle validation.
         */
        public Builder disableAngleValidation() {
            this.enableAngleValidation = false;
            return this;
        }
        
        public HitDetectionConfig build() {
            // Validate that server-side reach doesn't exceed attack packet reach
            if (serverSideReach > attackPacketReach) {
                throw new IllegalStateException(
                        "Server-side reach (" + serverSideReach + ") cannot exceed " +
                                "attack packet reach (" + attackPacketReach + ")");
            }
            
            return new HitDetectionConfig(this);
        }
    }
}

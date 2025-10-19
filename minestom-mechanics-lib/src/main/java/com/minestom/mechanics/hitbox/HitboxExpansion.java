package com.minestom.mechanics.hitbox;

import com.minestom.mechanics.config.combat.HitDetectionConfig;
import net.minestom.server.coordinate.Vec;

// TODO: Ensure no duplicate methods / logic, move to future player package

/**
 * Manages hitbox expansion values for hit detection and validation.
 * Provides primary and limit expansion values for different hit detection scenarios.
 */
public class HitboxExpansion {
    
    private final double primaryExpansion;
    private final double limitExpansion;
    
    /**
     * Constructor for HitDetectionConfig.
     */
    public HitboxExpansion(HitDetectionConfig config) {
        this.primaryExpansion = config.hitboxExpansionPrimary();
        this.limitExpansion = config.hitboxExpansionLimit();
    }
    
    /**
     * Get the primary hitbox expansion (standard 1.8-style).
     * Used for normal hit detection with precise raycasting.
     * 
     * @return Vec representing 3D hitbox expansion (x, y, z all equal to primary value)
     */
    public Vec getPrimary() {
        return new Vec(primaryExpansion, primaryExpansion, primaryExpansion);
    }
    
    /**
     * Get the limit hitbox expansion for lag compensation.
     * Used as fallback when primary expansion doesn't hit.
     * 
     * @return Vec representing 3D hitbox expansion (x, y, z all equal to limit value)
     */
    public Vec getLimit() {
        return new Vec(limitExpansion, limitExpansion, limitExpansion);
    }
    
    /**
     * Get the raw primary expansion value.
     * 
     * @return Primary expansion as double
     */
    public double getPrimaryValue() {
        return primaryExpansion;
    }
    
    /**
     * Get the raw limit expansion value.
     * 
     * @return Limit expansion as double
     */
    public double getLimitValue() {
        return limitExpansion;
    }
}

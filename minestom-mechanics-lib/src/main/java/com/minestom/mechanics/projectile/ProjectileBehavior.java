package com.minestom.mechanics.projectile;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;

/**
 * Interface for defining projectile behavior patterns.
 * ✅ ABSTRACTION: Provides consistent behavior interface for all projectile types.
 */
public interface ProjectileBehavior {
    
    /**
     * Called when the projectile hits an entity.
     * 
     * @param projectile The projectile entity
     * @param hit The entity that was hit
     * @return true if the hit should be processed, false to ignore
     */
    boolean onHit(Entity projectile, Entity hit);
    
    /**
     * Called when the projectile hits a block.
     * 
     * @param projectile The projectile entity
     * @param position The block position that was hit
     * @return true if the hit should be processed, false to ignore
     */
    boolean onBlockHit(Entity projectile, Point position);
    
    /**
     * Called when the projectile expires (timeout, distance, etc.).
     * 
     * @param projectile The projectile entity
     */
    void onExpire(Entity projectile);
}


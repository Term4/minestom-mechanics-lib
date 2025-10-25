package com.minestom.mechanics.systems.projectile.components;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;

// TODO: This is what I was talking about earlier with a general
//  projectile class, that handles onhit, onstuck, etc. OR ignores them.
//  this interface isn't used really though, sooo...
//  Still unsure if I should use an interface here or an abstract class.

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


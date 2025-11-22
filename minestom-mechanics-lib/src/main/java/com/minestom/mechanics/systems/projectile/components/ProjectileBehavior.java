package com.minestom.mechanics.systems.projectile.components;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;

// TODO: Add 'Look direction' knockback (where the direction a player is looking upon
//  being hit with a projectile determines the direction they take the knockback in)
//  Should have attacker based, victim based, and an onlyself (only works for attacker
//  hitting themselves, not on other players) boolean

/**
 * Interface for defining projectile behavior patterns.
 * 
 * NOTE: This interface is currently unused but reserved for future use.
 * It could be useful for implementing a strategy pattern to separate behavior
 * logic from entity implementation, allowing for swappable/configurable behaviors.
 * 
 * Currently, projectile behavior is implemented directly in entity classes
 * (e.g., AbstractArrow.onHit(), Snowball.onHit(), etc.). This interface could
 * be used in the future if behavior patterns need to be made more flexible or
 * configurable without subclassing.
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


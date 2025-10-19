package com.minestom.mechanics.sounds;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;

// TODO: Unused. Mentioned earlier moving all sounds / sound handling to this sounds package

/**
 * Interface for consistent sound playing across different systems.
 * ✅ ABSTRACTION: Standardizes sound playing patterns for projectiles, combat, and other mechanics.
 */
public interface SoundPlayer {
    
    /**
     * Play a sound when a projectile is shot/launched.
     * 
     * @param player The player who shot the projectile
     */
    void playShootSound(Player player);
    
    /**
     * Play a sound when a projectile hits an entity.
     * 
     * @param projectile The projectile entity
     * @param hit The entity that was hit
     */
    void playHitSound(Entity projectile, Entity hit);
    
    /**
     * Play a sound when a projectile hits a block or expires.
     * 
     * @param projectile The projectile entity
     */
    void playImpactSound(Entity projectile);
}

package com.minestom.mechanics.systems.projectile.features;

import com.minestom.mechanics.manager.Lifecycle;
import net.minestom.server.entity.Player;

// TODO: Add the ability to enable / disable individual projectiles
//  (i.e handle snowballs and eggs, but disable ender pearls)

/**
 * Interface for projectile features that can be managed by ProjectileManager.
 * Provides common methods for cleanup and lifecycle management.
 */
public interface ProjectileFeature extends Lifecycle {
    // Here for future use (with methods olike onhit, etc)
}

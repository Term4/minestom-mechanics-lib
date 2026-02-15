package com.minestom.mechanics.config.constants;

// TODO: Move this to a general constants package that contains all constants classes

import net.minestom.server.ServerFlag;

/**
 * Constants used throughout the projectile system.
 * Extracted magic numbers for better maintainability and easier tuning.
 */
public class ProjectileConstants {
    
    // ===========================
    // PHYSICS CONSTANTS
    // ===========================
    
    /**
     * Spread multiplier for projectile inaccuracy.
     * Applied to Gaussian random spread for projectile trajectory.
     * Value from vanilla Minecraft.
     */
    public static final double SPREAD_MULTIPLIER = 0.007499999832361937D;
    
    /**
     * Rotation lerp factor for smooth projectile rotation.
     * Lower values = smoother but slower rotation tracking.
     * 0.2 matches MinestomPvP and prevents stuttering.
     */
    public static final float ROTATION_LERP_FACTOR = 0.2f;
    
    /**
     * Ticks to wait before projectile can collide with shooter.
     * Prevents projectiles from immediately hitting the player who shot them.
     * 6 ticks = 0.3 seconds
     */
    public static final int SHOOTER_COLLISION_DELAY_TICKS = 5;
    
    // ===========================
    // HITBOX CONSTANTS
    // ===========================
    
    /**
     * Default projectile hitbox size (width, height, depth in blocks).
     * Small hitbox for accurate collision detection.
     */
    public static final double PROJECTILE_HITBOX_SIZE = 0.25;
    
    // ===========================
    // SPAWN POSITION CONSTANTS
    // ===========================
    
    /**
     * Offset below eye height for arrow spawn position.
     * Matches MinestomPvP: eyeHeight - 0.1
     */
    public static final double ARROW_SPAWN_HEIGHT_OFFSET = 0.1;
    
    // ===========================
    // PROJECTILE POWER CONSTANTS
    // ===========================
    
    /**
     * Power multiplier for miscellaneous projectiles (snowballs, eggs, ender pearls).
     * Controls initial velocity magnitude.
     */
    public static final double MISC_PROJECTILE_POWER = 1.5;
    
    // ===========================
    // ARROW CONSTANTS
    // ===========================
    
    /**
     * Base damage for arrows before velocity and enchantment modifiers.
     * Used in damage calculation: damage = velocity * baseDamage
     */
    public static final double ARROW_BASE_DAMAGE = 2.0;
    
    /**
     * Ticks to wait before an arrow can be picked up after being shot.
     * Prevents immediate pickup (vanilla 1.8 behavior).
     * 20 ticks = 1 second
     */
    public static final int ARROW_PICKUP_DELAY_TICKS = 20;

    /**
     * Ticks a stuck arrow survives before despawning.
     * 1200 ticks = 60 seconds at 20 TPS
     */
    public static final int ARROW_DESPAWN_TICKS = 1200;

    /**
     * Ticks to wait before an arrow stuck in a block can be picked up.
     * 7 ticks = 0.35 seconds at 20 TPS
     */
    public static final int ARROW_STUCK_PICKUP_DELAY_TICKS = 7;

    // ===========================
    // FISHING BOBBER CONSTANTS
    // ===========================
    
    /**
     * Gravity for legacy 1.8 fishing bobber.
     * Applied as additional gravity before normal physics.
     */
    public static final double FISHING_BOBBER_LEGACY_GRAVITY = 0.04;
    
    /**
     * Gravity for modern fishing bobber.
     * Applied as additional gravity before normal physics.
     */
    public static final double FISHING_BOBBER_MODERN_GRAVITY = 0.03;

    /**
     * Ticks to wait before applying fall velocity when bobber lands near player.
     * 3 ticks = 0.15 seconds at 20 TPS
     */
    public static final int FISHING_BOBBER_FALL_DELAY_TICKS = 3;

    /**
     * Ticks a stuck fishing bobber survives before despawning.
     * 1200 ticks = 60 seconds at 20 TPS
     */
    public static final int FISHING_BOBBER_STUCK_DESPAWN_TICKS = 1200;

    // ===========================
    // SYNCHRONIZATION CONSTANTS
    // ===========================
    
    /**
     * Position update interval in ticks.
     * How often position synchronization packets are sent to clients.
     * 20 ticks = 1 second (matches MinestomPvP)
     */
    public static final int POSITION_UPDATE_INTERVAL = ServerFlag.SERVER_TICKS_PER_SECOND;
    
    private ProjectileConstants() {
        // Prevent instantiation
    }
}


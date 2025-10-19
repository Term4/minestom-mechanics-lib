package com.minestom.mechanics.util;

// TODO: Move to constants package

/**
 * Centralized constants for the Minestom Mechanics library.
 * Contains commonly used magic numbers, timing values, and distances.
 */
public final class MechanicsConstants {

    private MechanicsConstants() {
        // Private constructor to prevent instantiation
    }

    // ===========================
    // TIMING CONSTANTS
    // ===========================

    /** Attack deduplication timeout in milliseconds */
    public static final long ATTACK_DEDUPE_MS = 50;

    /** Server ticks per second */
    public static final int TICKS_PER_SECOND = 20;

    /** One hour in ticks (72000 ticks) */
    public static final int ONE_HOUR_TICKS = 72000;

    // ===========================
    // DISTANCE CONSTANTS
    // ===========================

    /** Maximum fishing rod distance before auto-retract */
    public static final double MAX_FISHING_ROD_DISTANCE = 33.0;

    /** Auto-retract distance for fishing rods */
    public static final double AUTO_RETRACT_DISTANCE = 30.0;

    // ===========================
    // HITBOX CONSTANTS
    // ===========================

    /** Primary hitbox expansion for precise hit detection */
    public static final double HITBOX_EXPANSION_PRIMARY = 0.1;

    /** Limit hitbox expansion for fallback hit detection */
    public static final double HITBOX_EXPANSION_LIMIT = 0.3;

    // ===========================
    // PHYSICS CONSTANTS
    // ===========================

    /** Default friction value for knockback calculations */
    public static final double DEFAULT_FRICTION = 0.6;

    /** Default air resistance for projectiles */
    public static final double DEFAULT_AIR_RESISTANCE = 0.92;

    /** Default velocity multiplier for fishing rods */
    public static final double FISHING_ROD_MAX_VELOCITY = 0.4;

    // ===========================
    // KNOCKBACK CONSTANTS
    // ===========================

    /** Default horizontal knockback multiplier */
    public static final double DEFAULT_HORIZONTAL_KNOCKBACK = 0.4;

    /** Default vertical knockback multiplier */
    public static final double DEFAULT_VERTICAL_KNOCKBACK = 0.4;

    /** Default vertical knockback limit */
    public static final double DEFAULT_VERTICAL_LIMIT = 0.5;

    /** Default sprint knockback multiplier */
    public static final double DEFAULT_SPRINT_KNOCKBACK = 0.5;

    /** Default sprint vertical knockback */
    public static final double DEFAULT_SPRINT_VERTICAL = 0.1;

    // ===========================
    // PLAYER CONSTANTS
    // ===========================

    /** Standard player height */
    public static final double PLAYER_HEIGHT = 1.8;

    /** Standard player width */
    public static final double PLAYER_WIDTH = 0.6;

    /** Standard player eye height when standing */
    public static final double STANDING_EYE_HEIGHT = 1.62;

    /** Standard player eye height when sneaking */
    public static final double SNEAKING_EYE_HEIGHT = 1.27;

    // ===========================
    // PROJECTILE CONSTANTS
    // ===========================

    /** Default projectile spread multiplier */
    public static final double DEFAULT_PROJECTILE_SPREAD = 0.0075;

    /** Default projectile power multiplier */
    public static final double DEFAULT_PROJECTILE_POWER = 0.75;

    /** Default projectile velocity multiplier */
    public static final double DEFAULT_PROJECTILE_VELOCITY = 0.4;
}

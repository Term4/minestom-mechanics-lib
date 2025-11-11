package com.minestom.mechanics.config.constants;

import net.minestom.server.ServerFlag;
import net.minestom.server.network.socket.Server;

/**
 * Constants for the legacy gravity system.
 * Used for 1.8 players with custom gravity mechanics.
 */
public final class LegacyGravityConstants {

    private LegacyGravityConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // ===========================
    // TUNING CONSTANTS
    // ===========================

    /**
     * Scale factor for explosion velocity.
     * Controls how strongly gravity affects the player.
     * 0.05 = gentle/moon-like, 0.1 = normal-ish, 0.2 = heavy
     */
    public static final double EXPLOSION_VELOCITY_SCALE = 0.22;

    /**
     * Base terminal velocity (NEGATIVE value).
     * This is the terminal velocity for GRAVITY_NORMAL.
     * For other gravity values, terminal velocity is scaled proportionally.
     * Prevents unlimited acceleration.
     */
    public static final double TERMINAL_VELOCITY = 0.6 * ServerFlag.SERVER_TICKS_PER_SECOND;

    /**
     * Maximum upward velocity (POSITIVE value).
     * Prevents exponential upward acceleration from knockback.
     * Increased slightly to make knockback feel more natural.
     */
    public static final double MAX_UPWARD_VELOCITY = (double) 7 / ServerFlag.SERVER_TICKS_PER_SECOND;

    /**
     * Whether to apply terminal velocity cap.
     */
    public static final boolean USE_TERMINAL_VELOCITY = true;

    /**
     * Smoothing factor for velocity changes (0.0 to 1.0).
     * Higher = more responsive, Lower = smoother (helps with ping)
     * 1.0 = no smoothing, 0.7 = moderate smoothing
     */
    public static final double VELOCITY_SMOOTHING = 0.85;

    // ===========================
    // COMMON GRAVITY VALUES
    // ===========================

    /**
     * Normal gravity value (standard fall speed).
     */
    public static final double GRAVITY_NORMAL = 0.08;

    /**
     * Projectile gravity value (slower fall for projectiles).
     */
    public static final double GRAVITY_PROJECTILE = 0.05;

    /**
     * No gravity (zero fall speed).
     */
    public static final double GRAVITY_NONE = 0.0;

    /**
     * Low gravity value (gentle fall).
     */
    public static final double GRAVITY_LOW = 0.02;

    /**
     * High gravity value (fast fall).
     */
    public static final double GRAVITY_HIGH = 0.16;
}


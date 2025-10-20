package com.minestom.mechanics.config.combat;

import static com.minestom.mechanics.constants.CombatConstants.*;

/**
 * Configuration for hit detection and validation.
 *
 * <p>Controls how the server validates and processes combat hits, including reach
 * distances for raycasting and packet validation, hitbox expansion for lag
 * compensation, and optional angle validation for anticheat.</p>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Use a preset
 * HitDetectionConfig config = HitDetectionConfig.standard();
 *
 * // Customize from preset
 * HitDetectionConfig config = HitDetectionConfig.strict()
 *     .withServerSideReach(3.2)
 *     .withHitboxExpansion(0.15, 0.3);
 *
 * // Build from scratch
 * HitDetectionConfig config = new HitDetectionConfig(
 *     3.5, 6.5, 0.15, 0.3, 45.0, true, true
 * );
 * }</pre>
 *
 * @param serverSideReach Server-side reach for raycasting in blocks (typical: 3.0).
 *                        This is the "fair" reach given to all clients via server-side hit detection.
 * @param attackPacketReach Maximum reach for validating client attack packets (typical: 6.0).
 *                          Anticheat upper bound that's lenient to account for lag.
 * @param hitboxExpansionPrimary Primary hitbox expansion per side (0.1 = 1.8 standard).
 *                               Applied to all hit detection checks.
 * @param hitboxExpansionLimit Maximum hitbox expansion for lag compensation.
 *                             Used for benefit-of-the-doubt scenarios.
 * @param angleThreshold Maximum angle in degrees for hit validation (0-180).
 *                       Only used when angle validation is enabled.
 * @param enableAngleValidation Whether to validate attack angles.
 *                              Can help detect some types of killaura.
 * @param trackHitSnapshots Whether to track hit snapshots for debugging.
 *                          Useful for development and troubleshooting.
 */
public record HitDetectionConfig(
        double serverSideReach,
        double attackPacketReach,
        double hitboxExpansionPrimary,
        double hitboxExpansionLimit,
        double angleThreshold,
        boolean enableAngleValidation,
        boolean trackHitSnapshots
) {

    // Validation
    public HitDetectionConfig {
        if (serverSideReach <= 0 || serverSideReach < MIN_REACH_DISTANCE || serverSideReach > MAX_REACH_VALIDATION) {
            throw new IllegalArgumentException("Invalid server-side reach: " + serverSideReach);
        }
        if (attackPacketReach <= 0 || attackPacketReach > MAX_REACH_VALIDATION) {
            throw new IllegalArgumentException("Invalid attack packet reach: " + attackPacketReach);
        }
        if (serverSideReach > attackPacketReach) {
            throw new IllegalStateException("Server-side reach cannot exceed attack packet reach");
        }
        if (hitboxExpansionPrimary < 0.0 || hitboxExpansionPrimary > 0.5) {
            throw new IllegalArgumentException("Invalid primary hitbox expansion: " + hitboxExpansionPrimary);
        }
        if (hitboxExpansionLimit < 0.0 || hitboxExpansionLimit > 0.5) {
            throw new IllegalArgumentException("Invalid hitbox expansion limit: " + hitboxExpansionLimit);
        }
        if (hitboxExpansionPrimary > hitboxExpansionLimit) {
            throw new IllegalArgumentException("Primary expansion cannot exceed limit");
        }
        if (angleThreshold < 0.0 || angleThreshold > 180.0) {
            throw new IllegalArgumentException("Invalid angle threshold: " + angleThreshold);
        }
    }

    // ===========================
    // PRESETS
    // ===========================

    /**
     * Standard hit detection configuration.
     *
     * <p>Balanced settings for general 1.8-style PvP:</p>
     * <ul>
     *   <li>Server-side reach: 3.0 blocks</li>
     *   <li>Attack packet reach: 6.0 blocks</li>
     *   <li>Primary hitbox expansion: 0.1 (1.8 standard)</li>
     *   <li>Expansion limit: 0.3</li>
     *   <li>Angle validation: disabled</li>
     * </ul>
     *
     * @return standard hit detection configuration
     */
    public static HitDetectionConfig standard() {
        return new HitDetectionConfig(SERVER_SIDE_REACH, ATTACK_PACKET_REACH,
                HITBOX_EXPANSION_PRIMARY, HITBOX_EXPANSION_LIMIT, 90.0, false, true);
    }

    /**
     * Strict hit detection for anticheat compatibility.
     *
     * <p>Tighter validation with angle checking enabled:</p>
     * <ul>
     *   <li>Server-side reach: 3.0 blocks</li>
     *   <li>Attack packet reach: 4.5 blocks (strict)</li>
     *   <li>Primary hitbox expansion: 0.1</li>
     *   <li>Expansion limit: 0.105 (very strict)</li>
     *   <li>Angle validation: enabled (90°)</li>
     * </ul>
     *
     * @return strict hit detection configuration
     */
    public static HitDetectionConfig strict() {
        return new HitDetectionConfig(3.0, 4.5, 0.1, 0.105, 90.0, true, true);
    }

    /**
     * Lenient hit detection for high ping players.
     *
     * <p>More generous validation to reduce false positives:</p>
     * <ul>
     *   <li>Server-side reach: 3.5 blocks (lenient)</li>
     *   <li>Attack packet reach: 7.0 blocks (very lenient)</li>
     *   <li>Primary hitbox expansion: 0.15</li>
     *   <li>Expansion limit: 0.4 (generous)</li>
     *   <li>Angle validation: disabled</li>
     * </ul>
     *
     * @return lenient hit detection configuration
     */
    public static HitDetectionConfig lenient() {
        return new HitDetectionConfig(3.5, 7.0, 0.15, 0.4, 45.0, false, true);
    }

    // ===========================
    // "WITH" METHODS
    // ===========================

    /**
     * Create a copy with a different server-side reach.
     *
     * @param reach the new server-side reach in blocks
     * @return a new config with the updated value
     */
    public HitDetectionConfig withServerSideReach(double reach) {
        return new HitDetectionConfig(reach, attackPacketReach, hitboxExpansionPrimary,
                hitboxExpansionLimit, angleThreshold, enableAngleValidation, trackHitSnapshots);
    }

    /**
     * Create a copy with a different attack packet reach.
     *
     * @param reach the new attack packet reach in blocks
     * @return a new config with the updated value
     */
    public HitDetectionConfig withAttackPacketReach(double reach) {
        return new HitDetectionConfig(serverSideReach, reach, hitboxExpansionPrimary,
                hitboxExpansionLimit, angleThreshold, enableAngleValidation, trackHitSnapshots);
    }

    /**
     * Create a copy with different reach values.
     *
     * <p>Convenience method to set both reach values at once.</p>
     *
     * @param serverSide the server-side reach in blocks
     * @param attackPacket the attack packet reach in blocks
     * @return a new config with the updated values
     */
    public HitDetectionConfig withReach(double serverSide, double attackPacket) {
        return new HitDetectionConfig(serverSide, attackPacket, hitboxExpansionPrimary,
                hitboxExpansionLimit, angleThreshold, enableAngleValidation, trackHitSnapshots);
    }

    /**
     * Create a copy with different hitbox expansion values.
     *
     * <p>Convenience method to set both expansion values at once.</p>
     *
     * @param primary the primary expansion value
     * @param limit the expansion limit value
     * @return a new config with the updated values
     */
    public HitDetectionConfig withHitboxExpansion(double primary, double limit) {
        return new HitDetectionConfig(serverSideReach, attackPacketReach, primary,
                limit, angleThreshold, enableAngleValidation, trackHitSnapshots);
    }

    /**
     * Create a copy with angle validation enabled or disabled.
     *
     * @param enabled whether angle validation should be enabled
     * @return a new config with the updated value
     */
    public HitDetectionConfig withAngleValidation(boolean enabled) {
        return new HitDetectionConfig(serverSideReach, attackPacketReach, hitboxExpansionPrimary,
                hitboxExpansionLimit, angleThreshold, enabled, trackHitSnapshots);
    }

    /**
     * Create a copy with angle validation settings.
     *
     * <p>Convenience method to set both angle validation and threshold.</p>
     *
     * @param enabled whether angle validation should be enabled
     * @param threshold the angle threshold in degrees (0-180)
     * @return a new config with the updated values
     */
    public HitDetectionConfig withAngleValidation(boolean enabled, double threshold) {
        return new HitDetectionConfig(serverSideReach, attackPacketReach, hitboxExpansionPrimary,
                hitboxExpansionLimit, threshold, enabled, trackHitSnapshots);
    }
}
package com.minestom.mechanics.config.combat;

/**
 * Centralized constants for combat system.
 * All magic numbers and configuration values should be defined here.
 */
public final class CombatConstants {

    private CombatConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // ===========================
    // DAMAGE SYSTEM
    // ===========================

    /** Default invulnerability duration in ticks (0.5 seconds at 20 TPS) */
    public static final int DEFAULT_INVULNERABILITY_TICKS = 10;

    /** No invulnerability - for 1.8 combo style combat */
    public static final int NO_INVULNERABILITY_TICKS = 0;

    /** Default: Enable damage replacement */
    public static final boolean DEFAULT_DAMAGE_REPLACEMENT = false;

    /** Default: Apply knockback on replacement hits */
    public static final boolean DEFAULT_KNOCKBACK_ON_REPLACEMENT = false;

    /** Balanced invulnerability duration in ticks */
    public static final int BALANCED_INVULNERABILITY_TICKS = 5;

    /** Safe fall distance before damage is applied (blocks) */
    public static final double SAFE_FALL_DISTANCE = 3.0;

    /** Default fall damage multiplier */
    public static final float DEFAULT_FALL_DAMAGE_MULTIPLIER = 1.0f;

    /** Default fire damage multiplier */
    public static final float DEFAULT_FIRE_DAMAGE_MULTIPLIER = 1.0f;

    // ---------------------------------------------------------------------------
    // PLAYER DIMENSIONS
    // ---------------------------------------------------------------------------

    /** Standard player width (blocks) */
    public static final double PLAYER_WIDTH = 0.6;

    /** Standard player height when standing (blocks) */
    public static final double PLAYER_HEIGHT = 1.8;

    /** Player height when sneaking (blocks) */
    public static final double PLAYER_HEIGHT_SNEAKING = 1.5;

    /** Eye height offset from feet when standing (blocks) */
    public static final double PLAYER_EYE_HEIGHT = 1.62;

    /** Eye height offset from feet when sneaking (blocks) */
    public static final double PLAYER_EYE_HEIGHT_SNEAKING = 1.52;

    // ---------------------------------------------------------------------------
    // HITBOX EXPANSION
    // ---------------------------------------------------------------------------

    /**
     * Primary hitbox expansion matching 1.8 client expectations (blocks per side).
     * 1.8 clients hardcode 0.1 block expansion during client-side raycasting.
     */
    public static final double HITBOX_EXPANSION_PRIMARY = 0.1;

    /**
     * Maximum hitbox expansion limit for validation (blocks per side).
     * Used as fallback when primary validation fails.
     * Compensates for: network latency, position desync, precision errors.
     *
     * Common values:
     * - 0.15: Lenient (50-100ms latency)
     * - 0.2: Very lenient (100-150ms latency)
     * - 0.25: Maximum leniency (>150ms latency)
     */
    public static final double HITBOX_EXPANSION_LIMIT = 0.2;

    // ---------------------------------------------------------------------------
    // BLOCK INTERACTION REACH
    // ---------------------------------------------------------------------------

    /**
     * Creative mode block breaking/placing reach (blocks).
     * Used to prevent swing-based attacks from triggering during block interaction.
     */
    public static final double CREATIVE_REACH = 6.0;

    /**
     * Survival mode block breaking/placing reach (blocks).
     * Standard 1.8 survival reach distance.
     */
    public static final double SURVIVAL_REACH = 4.5;

    /**
     * Block raycasting step size (blocks).
     * Smaller = more accurate but more expensive.
     * 0.2 blocks = check every ~1/5th of a block.
     */
    public static final double BLOCK_RAYCAST_STEP = 0.2;

    // ===========================
    // COMBAT SYSTEM
    // ===========================

    /** Minimum reach distance in blocks */
    public static final double MIN_REACH_DISTANCE = 1.0;

    /** Standard reach distance in blocks (vanilla survival) */
    public static final double SERVER_SIDE_REACH = 3.0;

    /** Maximum reach distance in blocks (vanilla creative) */
    public static final double ATTACK_PACKET_REACH = 5.0;

    /** Critical hit damage multiplier */
    public static final float CRITICAL_HIT_MULTIPLIER = 1.5f;

    /** Attack range margin for hit detection */
    public static final double ATTACK_RANGE_MARGIN = 0.0;

    // ===========================
    // KNOCKBACK SYSTEM
    // ===========================

    /** Minimum distance for knockback direction calculation */
    public static final double MIN_KNOCKBACK_DISTANCE = 0.0001;

    /** Falling velocity threshold for "no falling KB" feature */
    public static final double FALLING_VELOCITY_THRESHOLD = -0.08;

    /** Minimum vertical knockback when falling (1.8 no-falling KB) */
    public static final double MIN_FALLING_KNOCKBACK = 0.32;

    /** Ground state detection delay in milliseconds */
    public static final long GROUND_STATE_DELAY_MS = 100;

    /** Combat state timeout in milliseconds */
    public static final long COMBAT_TIMEOUT_MS = 5000;

    /** Maximum angle difference for angle interpolation */
    public static final float MAX_ANGLE_DIFF = 180.0f;

    /** Full circle in degrees */
    public static final float FULL_CIRCLE_DEGREES = 360.0f;

    // ===========================
    // KNOCKBACK SYNC
    // ===========================

    /** Maximum rewind time for lag compensation in milliseconds */
    public static final long MAX_REWIND_TIME_MS = 300;

    /** Position history size (number of snapshots to keep) */
    public static final int POSITION_HISTORY_SIZE = 30;

    /** Velocity history size (number of snapshots to keep) */
    public static final int VELOCITY_HISTORY_SIZE = 30;

    /** Default interpolation factor for lag compensation */
    public static final double DEFAULT_INTERPOLATION_FACTOR = 0.75;

    /** Ping tracker history size (number of ping samples) */
    public static final int PING_HISTORY_SIZE = 10;

    /** Ping update interval in seconds */
    public static final int PING_UPDATE_INTERVAL_SECONDS = 1;

    // ===========================
    // BLOCKING SYSTEM
    // ===========================

    /** Default damage reduction when blocking (50%) */
    public static final double DEFAULT_BLOCKING_DAMAGE_REDUCTION = 0.5;

    /** Default horizontal knockback multiplier when blocking (40% = 60% reduction) */
    public static final double DEFAULT_BLOCKING_KB_HORIZONTAL = 0.4;

    /** Default vertical knockback multiplier when blocking (40% = 60% reduction) */
    public static final double DEFAULT_BLOCKING_KB_VERTICAL = 0.4;

    /** Minimum blocking reduction value */
    public static final double MIN_BLOCKING_REDUCTION = 0.0;

    /** Maximum blocking reduction value */
    public static final double MAX_BLOCKING_REDUCTION = 1.0;

    /** Particle circle radius for blocking effects */
    public static final double BLOCKING_PARTICLE_RADIUS = 0.5;

    /** Default particle count for blocking effects */
    public static final int DEFAULT_BLOCKING_PARTICLE_COUNT = 8;

    /** Minimum particle count */
    public static final int MIN_PARTICLE_COUNT = 1;

    /** Maximum particle count */
    public static final int MAX_PARTICLE_COUNT = 20;

    /** Particle effect update interval in milliseconds */
    public static final long PARTICLE_UPDATE_INTERVAL_MS = 100;

    /** Default particle Y offset (at player eye level) */
    public static final double PARTICLE_Y_OFFSET = 1.0;

    // ===========================
    // ARMOR SYSTEM
    // ===========================

    /** Maximum armor reduction percentage (at 20 armor points) */
    public static final double MAX_ARMOR_REDUCTION = 0.8; // 80%

    /** Armor points divisor for effectiveness calculation */
    public static final double ARMOR_EFFECTIVENESS_DIVISOR = 5.0;

    /** Toughness divisor for damage reduction calculation */
    public static final double TOUGHNESS_DIVISOR = 4.0;

    /** Base divisor for armor calculation */
    public static final double ARMOR_BASE_DIVISOR = 2.0;

    /** Maximum effective armor points */
    public static final double MAX_EFFECTIVE_ARMOR = 20.0;

    /** Armor reduction divisor (25 = full protection at 20 armor) */
    public static final double ARMOR_REDUCTION_DIVISOR = 25.0;

    // ===========================
    // VELOCITY CONVERSION
    // ===========================

    /** Velocity packet multiplier for client (Minestom uses 8000) */
    public static final double VELOCITY_PACKET_MULTIPLIER = 8000.0;

    /** Maximum velocity value for packet (Short.MAX_VALUE) */
    public static final short MAX_VELOCITY_VALUE = Short.MAX_VALUE;

    /** Minimum velocity value for packet (Short.MIN_VALUE) */
    public static final short MIN_VELOCITY_VALUE = Short.MIN_VALUE;

    // ===========================
    // VALIDATION LIMITS
    // ===========================

    /** Maximum reach distance for validation */
    public static final double MAX_REACH_VALIDATION = 25.0;

    /** Maximum attack cooldown in ticks */
    public static final int MAX_ATTACK_COOLDOWN_TICKS = 100;

    /** Standard attacker cooldown */
    public static final int STANDARD_ATTACK_COOLDOWN = 0;

    /** Maximum critical multiplier */
    public static final float MAX_CRITICAL_MULTIPLIER = 10.0f;

    /** Minimum critical multiplier (1.0 = no bonus) */
    public static final float MIN_CRITICAL_MULTIPLIER = 1.0f;

    /** Sprint window tracking size (ticks) */
    public static final int SPRINTWINDOW_TRACKING_TICKS = 10;

    /** Minimum time (ms) a player must be sprinting to recieve knockback bonus */
    public static final int SPRINTWINDOW_MIN_SPRINT_DURATION_MS = 50;

    /** Sprint window jitter buffer (ms) */
    public static final int SPRINTWINDOW_JITTER_TOLERANCE_MS = 25;

    /** Default sprint window tick buffer */
    public static final int DEFAULT_SPRINTWINDOW_TICKS = 2;

    /** Default maximum sprint window tick buffer */
    public static final int DEFAULT_SPRINT_WINDOW_MAX_TICKS = 5;

    /** Maximum knockback multiplier for validation */
    public static final double MAX_KNOCKBACK_MULTIPLIER = 2.0;

    /** Minimum knockback multiplier for validation */
    public static final double MIN_KNOCKBACK_MULTIPLIER = 0.5;

    // ===========================
    // WEAPON DAMAGE VALUES
    // ===========================

    public static final float DAMAGE_FIST = 1.0f;

    // Swords
    public static final float DAMAGE_WOODEN_SWORD = 4.0f;
    public static final float DAMAGE_STONE_SWORD = 5.0f;
    public static final float DAMAGE_IRON_SWORD = 6.0f;
    public static final float DAMAGE_GOLDEN_SWORD = 4.0f;
    public static final float DAMAGE_DIAMOND_SWORD = 7.0f;
    public static final float DAMAGE_NETHERITE_SWORD = 8.0f;

    // Axes
    public static final float DAMAGE_WOODEN_AXE = 7.0f;
    public static final float DAMAGE_STONE_AXE = 9.0f;
    public static final float DAMAGE_IRON_AXE = 9.0f;
    public static final float DAMAGE_GOLDEN_AXE = 7.0f;
    public static final float DAMAGE_DIAMOND_AXE = 9.0f;
    public static final float DAMAGE_NETHERITE_AXE = 10.0f;

    // Pickaxes
    public static final float DAMAGE_WOODEN_PICKAXE = 2.0f;
    public static final float DAMAGE_STONE_PICKAXE = 3.0f;
    public static final float DAMAGE_IRON_PICKAXE = 4.0f;
    public static final float DAMAGE_GOLDEN_PICKAXE = 2.0f;
    public static final float DAMAGE_DIAMOND_PICKAXE = 5.0f;
    public static final float DAMAGE_NETHERITE_PICKAXE = 6.0f;

    // Shovels
    public static final float DAMAGE_WOODEN_SHOVEL = 2.5f;
    public static final float DAMAGE_STONE_SHOVEL = 3.5f;
    public static final float DAMAGE_IRON_SHOVEL = 4.5f;
    public static final float DAMAGE_GOLDEN_SHOVEL = 2.5f;
    public static final float DAMAGE_DIAMOND_SHOVEL = 5.5f;
    public static final float DAMAGE_NETHERITE_SHOVEL = 6.5f;

    // ===========================
    // ARMOR VALUES
    // ===========================

    // Leather
    public static final int ARMOR_LEATHER_HELMET = 1;
    public static final int ARMOR_LEATHER_CHESTPLATE = 3;
    public static final int ARMOR_LEATHER_LEGGINGS = 2;
    public static final int ARMOR_LEATHER_BOOTS = 1;

    // Chainmail
    public static final int ARMOR_CHAINMAIL_HELMET = 2;
    public static final int ARMOR_CHAINMAIL_CHESTPLATE = 5;
    public static final int ARMOR_CHAINMAIL_LEGGINGS = 4;
    public static final int ARMOR_CHAINMAIL_BOOTS = 1;

    // Iron
    public static final int ARMOR_IRON_HELMET = 2;
    public static final int ARMOR_IRON_CHESTPLATE = 6;
    public static final int ARMOR_IRON_LEGGINGS = 5;
    public static final int ARMOR_IRON_BOOTS = 2;

    // Gold
    public static final int ARMOR_GOLDEN_HELMET = 2;
    public static final int ARMOR_GOLDEN_CHESTPLATE = 5;
    public static final int ARMOR_GOLDEN_LEGGINGS = 3;
    public static final int ARMOR_GOLDEN_BOOTS = 1;

    // Diamond
    public static final int ARMOR_DIAMOND_HELMET = 3;
    public static final int ARMOR_DIAMOND_CHESTPLATE = 8;
    public static final int ARMOR_DIAMOND_LEGGINGS = 6;
    public static final int ARMOR_DIAMOND_BOOTS = 3;

    // Netherite
    public static final int ARMOR_NETHERITE_HELMET = 3;
    public static final int ARMOR_NETHERITE_CHESTPLATE = 8;
    public static final int ARMOR_NETHERITE_LEGGINGS = 6;
    public static final int ARMOR_NETHERITE_BOOTS = 3;

    // Turtle
    public static final int ARMOR_TURTLE_HELMET = 2;
}

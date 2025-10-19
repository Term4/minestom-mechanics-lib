package com.minestom.mechanics.config.gameplay;

/**
 * Preset gameplay configurations for common server modes.
 */
public final class GameplayPresets {

    private GameplayPresets() {} // Utility class

    /**
     * MinemanClub gameplay - 1.8 mechanics enforcement.
     */
    public static final GameplayConfig MINEMEN = new GameplayConfig(
            EyeHeightConfig.minecraft18(),
            MovementConfig.minecraft18(),
            HitboxConfig.minecraft18(),
            false  // No player collision
    );

    // TODO: Add a modern preset and a legacy preset (1.8 vs 1.20)
    /**
     * Vanilla gameplay - standard Minecraft mechanics.
     */
    public static final GameplayConfig VANILLA = new GameplayConfig(
            EyeHeightConfig.vanilla(),
            MovementConfig.vanilla(),
            HitboxConfig.vanilla(),
            true  // Player collision enabled
    );

    /**
     * Hypixel-style gameplay.
     */
    public static final GameplayConfig HYPIXEL = new GameplayConfig(
            EyeHeightConfig.vanilla(),
            MovementConfig.vanilla(),
            HitboxConfig.vanilla(),
            false  // No collision
    );

    /**
     * Standard/balanced gameplay.
     */
    public static final GameplayConfig STANDARD = VANILLA;

    /**
     * Legacy 1.8 mode - strict mechanics.
     */
    public static final GameplayConfig LEGACY_1_8 = new GameplayConfig(
            EyeHeightConfig.minecraft18(),
            MovementConfig.minecraft18(),
            HitboxConfig.minecraft18(),
            false
    );

    /**
     * Modern mode - all features enabled.
     */
    public static final GameplayConfig MODERN = new GameplayConfig(
            EyeHeightConfig.vanilla(),
            MovementConfig.vanilla(),
            HitboxConfig.vanilla(),
            true
    );
}
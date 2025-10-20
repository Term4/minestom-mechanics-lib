package com.minestom.mechanics.config.projectiles;

// TODO: Add an option for modern vs legacy projectiles (already exists, kinda, player momentum, just less explicit)
//  ALSO could add general configurable option in projectileconfig for
//  allowing / disallowing players from hitting themselves with a specified
//  projectile (would be useful for minigames / rules).
//  ALSO could add same option for being able to hit team members.

// TODO: Add fireballs as a projectile

import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackPresets;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityPresets;

/**
 * Immutable configuration for all projectile-related settings.
 * Follows the same pattern as CombatConfig.
 *
 * Usage:
 * <pre>
 * // Use preset
 * ProjectileConfig config = ProjectilePresets.VANILLA;
 *
 * // Swap full configs
 * ProjectileConfig config = ProjectilePresets.VANILLA
 *     .withArrowVelocity(ProjectileVelocityPresets.LASER_ARROW);
 *
 * // Quick tweaks using static helpers
 * ProjectileConfig config = ProjectilePresets.withArrowSpeed(ProjectilePresets.VANILLA, 5.0);
 * </pre>
 */
public record ProjectileConfig(
        // Player momentum inheritance (modern feature, disabled for legacy 1.8)
        boolean inheritPlayerMomentum,

        // Knockback configurations (what happens on hit)
        ProjectileKnockbackConfig arrowKnockback,
        ProjectileKnockbackConfig snowballKnockback,
        ProjectileKnockbackConfig eggKnockback,
        ProjectileKnockbackConfig enderPearlKnockback,
        ProjectileKnockbackConfig fishingRodKnockback,

        // Velocity configurations (how projectiles fly)
        ProjectileVelocityConfig arrowVelocity,
        ProjectileVelocityConfig snowballVelocity,
        ProjectileVelocityConfig eggVelocity,
        ProjectileVelocityConfig enderPearlVelocity,
        ProjectileVelocityConfig fishingRodVelocity,

        // Fishing rod specific
        FishingRodKnockbackMode fishingRodKnockbackMode,
        boolean fishingRodPullPlayers
) {

    /**
     * Knockback mode for fishing rods - determines direction calculation
     */
    public enum FishingRodKnockbackMode {
        /** Knockback away from bobber position (vanilla behavior) */
        BOBBER_RELATIVE,
        /** Knockback from shooter/player position (like normal projectiles) */
        ORIGIN_RELATIVE
    }
    public boolean isFishingRodPullPlayers() {
        return fishingRodPullPlayers;
    }

    // Validation
    public ProjectileConfig {
        if (arrowKnockback == null) throw new IllegalArgumentException("Arrow knockback cannot be null");
        if (snowballKnockback == null) throw new IllegalArgumentException("Snowball knockback cannot be null");
        if (eggKnockback == null) throw new IllegalArgumentException("Egg knockback cannot be null");
        if (enderPearlKnockback == null) throw new IllegalArgumentException("Ender pearl knockback cannot be null");
        if (fishingRodKnockback == null) throw new IllegalArgumentException("Fishing rod knockback cannot be null");
        if (arrowVelocity == null) throw new IllegalArgumentException("Arrow velocity cannot be null");
        if (snowballVelocity == null) throw new IllegalArgumentException("Snowball velocity cannot be null");
        if (eggVelocity == null) throw new IllegalArgumentException("Egg velocity cannot be null");
        if (enderPearlVelocity == null) throw new IllegalArgumentException("Ender pearl velocity cannot be null");
        if (fishingRodVelocity == null) throw new IllegalArgumentException("Fishing rod velocity cannot be null");
        if (fishingRodKnockbackMode == null) throw new IllegalArgumentException("Fishing rod knockback mode cannot be null");
    }

    // ===== DEFAULT CONFIGURATION =====

    public static ProjectileConfig defaultConfig() {
        return new ProjectileConfig(
                // Inherit Player momentum
                true,   // Modern behavior. Inherits player momentum
                // Knockback
                ProjectileKnockbackPresets.ARROW,
                ProjectileKnockbackPresets.SNOWBALL,
                ProjectileKnockbackPresets.EGG,
                ProjectileKnockbackPresets.ENDER_PEARL,
                ProjectileKnockbackPresets.FISHING_ROD,
                // Velocity
                ProjectileVelocityPresets.ARROW,
                ProjectileVelocityPresets.SNOWBALL,
                ProjectileVelocityPresets.EGG,
                ProjectileVelocityPresets.ENDER_PEARL,
                ProjectileVelocityPresets.FISHING_ROD,
                // Fishing rod mode
                FishingRodKnockbackMode.BOBBER_RELATIVE,
                true  // Pull hooked players with fishing rod
        );
    }

    // ===== COMPATIBILITY GETTERS =====

    public ProjectileKnockbackConfig getArrowKnockbackConfig() { return arrowKnockback; }
    public ProjectileKnockbackConfig getSnowballKnockbackConfig() { return snowballKnockback; }
    public ProjectileKnockbackConfig getEggKnockbackConfig() { return eggKnockback; }
    public ProjectileKnockbackConfig getEnderPearlKnockbackConfig() { return enderPearlKnockback; }
    public ProjectileKnockbackConfig getFishingRodKnockbackConfig() { return fishingRodKnockback; }

    public ProjectileVelocityConfig getArrowVelocityConfig() { return arrowVelocity; }
    public ProjectileVelocityConfig getSnowballVelocityConfig() { return snowballVelocity; }
    public ProjectileVelocityConfig getEggVelocityConfig() { return eggVelocity; }
    public ProjectileVelocityConfig getEnderPearlVelocityConfig() { return enderPearlVelocity; }
    public ProjectileVelocityConfig getFishingRodVelocityConfig() { return fishingRodVelocity; }

    public FishingRodKnockbackMode getFishingRodKnockbackMode() { return fishingRodKnockbackMode; }
    public boolean shouldInheritPlayerMomentum() { return inheritPlayerMomentum; }

    // ===== ARROW =====

    public ProjectileConfig withArrowKnockback(ProjectileKnockbackConfig config) {
        return new ProjectileConfig(inheritPlayerMomentum, config, snowballKnockback, eggKnockback, enderPearlKnockback,
                fishingRodKnockback, arrowVelocity, snowballVelocity, eggVelocity, enderPearlVelocity,
                fishingRodVelocity, fishingRodKnockbackMode, fishingRodPullPlayers);
    }

    public ProjectileConfig withArrowVelocity(ProjectileVelocityConfig config) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, snowballKnockback, eggKnockback, enderPearlKnockback,
                fishingRodKnockback, config, snowballVelocity, eggVelocity, enderPearlVelocity,
                fishingRodVelocity, fishingRodKnockbackMode, fishingRodPullPlayers);
    }

    // ===== SNOWBALL =====

    public ProjectileConfig withSnowballKnockback(ProjectileKnockbackConfig config) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, config, eggKnockback, enderPearlKnockback,
                fishingRodKnockback, arrowVelocity, snowballVelocity, eggVelocity, enderPearlVelocity,
                fishingRodVelocity, fishingRodKnockbackMode, fishingRodPullPlayers);
    }

    public ProjectileConfig withSnowballVelocity(ProjectileVelocityConfig config) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, snowballKnockback, eggKnockback, enderPearlKnockback,
                fishingRodKnockback, arrowVelocity, config, eggVelocity, enderPearlVelocity,
                fishingRodVelocity, fishingRodKnockbackMode, fishingRodPullPlayers);
    }

    // ===== EGG =====

    public ProjectileConfig withEggKnockback(ProjectileKnockbackConfig config) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, snowballKnockback, config, enderPearlKnockback,
                fishingRodKnockback, arrowVelocity, snowballVelocity, eggVelocity, enderPearlVelocity,
                fishingRodVelocity, fishingRodKnockbackMode, fishingRodPullPlayers);
    }

    public ProjectileConfig withEggVelocity(ProjectileVelocityConfig config) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, snowballKnockback, eggKnockback, enderPearlKnockback,
                fishingRodKnockback, arrowVelocity, snowballVelocity, config, enderPearlVelocity,
                fishingRodVelocity, fishingRodKnockbackMode, fishingRodPullPlayers);
    }

    // ===== ENDER PEARL =====

    public ProjectileConfig withEnderPearlKnockback(ProjectileKnockbackConfig config) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, snowballKnockback, eggKnockback, config,
                fishingRodKnockback, arrowVelocity, snowballVelocity, eggVelocity, enderPearlVelocity,
                fishingRodVelocity, fishingRodKnockbackMode, fishingRodPullPlayers);
    }

    public ProjectileConfig withEnderPearlVelocity(ProjectileVelocityConfig config) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, snowballKnockback, eggKnockback, enderPearlKnockback,
                fishingRodKnockback, arrowVelocity, snowballVelocity, eggVelocity, config,
                fishingRodVelocity, fishingRodKnockbackMode, fishingRodPullPlayers);
    }

    // ===== FISHING ROD =====

    public ProjectileConfig withFishingRodKnockback(ProjectileKnockbackConfig config) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, snowballKnockback, eggKnockback, enderPearlKnockback,
                config, arrowVelocity, snowballVelocity, eggVelocity, enderPearlVelocity,
                fishingRodVelocity, fishingRodKnockbackMode, fishingRodPullPlayers);
    }

    public ProjectileConfig withFishingRodVelocity(ProjectileVelocityConfig config) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, snowballKnockback, eggKnockback, enderPearlKnockback,
                fishingRodKnockback, arrowVelocity, snowballVelocity, eggVelocity, enderPearlVelocity,
                config, fishingRodKnockbackMode, fishingRodPullPlayers);
    }

    public ProjectileConfig withFishingRodKnockbackMode(FishingRodKnockbackMode mode) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, snowballKnockback, eggKnockback, enderPearlKnockback,
                fishingRodKnockback, arrowVelocity, snowballVelocity, eggVelocity, enderPearlVelocity,
                fishingRodVelocity, mode, fishingRodPullPlayers);
    }

    // Add this:
    public ProjectileConfig withFishingRodPullPlayers(boolean pullPlayers) {
        return new ProjectileConfig(inheritPlayerMomentum, arrowKnockback, snowballKnockback, eggKnockback, enderPearlKnockback,
                fishingRodKnockback, arrowVelocity, snowballVelocity, eggVelocity, enderPearlVelocity,
                fishingRodVelocity, fishingRodKnockbackMode, pullPlayers);
    }

    // ===== PLAYER MOMENTUM =====

    public ProjectileConfig withInheritPlayerMomentum(boolean inherit) {
        return new ProjectileConfig(inherit, arrowKnockback, snowballKnockback, eggKnockback, enderPearlKnockback,
                fishingRodKnockback, arrowVelocity, snowballVelocity, eggVelocity, enderPearlVelocity,
                fishingRodVelocity, fishingRodKnockbackMode, fishingRodPullPlayers);
    }
}
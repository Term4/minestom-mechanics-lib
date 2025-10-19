package com.minestom.mechanics.config.gameplay;

/**
 * Configuration for player eye height enforcement.
 * Useful for normalizing cross-version reach mechanics.
 */
public record EyeHeightConfig(
        boolean enabled,
        double standingEyeHeight,
        double sneakingEyeHeight,
        double survivalReach,
        double creativeReach,
        boolean enforceBlockPlaceReach
) {
    // Validation
    public EyeHeightConfig {
        if (standingEyeHeight <= 0 || sneakingEyeHeight <= 0)
            throw new IllegalArgumentException("Eye heights must be positive");
        if (survivalReach <= 0 || creativeReach <= 0)
            throw new IllegalArgumentException("Reach must be positive");
    }

    // TODO: Get preset values from a constants class (constants package to create from combatconstants)
    // Presets
    public static EyeHeightConfig minecraft18() {
        return new EyeHeightConfig(true, 1.62, 1.54, 4.5, 5.0, true);
    }

    public static EyeHeightConfig vanilla() {
        return new EyeHeightConfig(false, 1.62, 1.54, 4.5, 5.0, true);
    }

    // "With" methods
    public EyeHeightConfig withEnabled(boolean enabled) {
        return new EyeHeightConfig(enabled, standingEyeHeight, sneakingEyeHeight,
                survivalReach, creativeReach, enforceBlockPlaceReach);
    }

    public EyeHeightConfig withStandingHeight(double height) {
        return new EyeHeightConfig(enabled, height, sneakingEyeHeight,
                survivalReach, creativeReach, enforceBlockPlaceReach);
    }

    public EyeHeightConfig withSneakingHeight(double height) {
        return new EyeHeightConfig(enabled, standingEyeHeight, height,
                survivalReach, creativeReach, enforceBlockPlaceReach);
    }

    public EyeHeightConfig withReach(double survival, double creative) {
        return new EyeHeightConfig(enabled, standingEyeHeight, sneakingEyeHeight,
                survival, creative, enforceBlockPlaceReach);
    }

    public EyeHeightConfig withEnforceBlockPlaceReach(boolean enforce) {
        return new EyeHeightConfig(enabled, standingEyeHeight, sneakingEyeHeight,
                survivalReach, creativeReach, enforce);
    }
}
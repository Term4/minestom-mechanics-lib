package com.minestom.mechanics.config.knockback;

public final class KnockbackPresets {
    private KnockbackPresets() {} // Prevent instantiation

    public static KnockbackConfig minemen() {
        return new KnockbackConfig(
                0.375,  // horizontal
                0.365,  // vertical
                0.45,   // verticalLimit
                0.34,   // sprintBonusHorizontal
                0.0,    // sprintBonusVertical
                1.0,    // airMultiplierHorizontal
                1.0,    // airMultiplierVertical
                0.5,    // lookWeight
                false,   // modern
                true    // knockbackSyncSupported
        );
    }

    public static KnockbackConfig vanilla18() {
        return new KnockbackConfig(
                0.4,    // horizontal
                0.4,    // vertical
                0.4,    // verticalLimit
                0.0,    // sprintBonusHorizontal
                0.0,    // sprintBonusVertical
                1.0,    // airMultiplierHorizontal
                1.0,    // airMultiplierVertical
                0.0,    // lookWeight
                false,  // modern (vanilla is legacy)
                true   // knockbackSyncSupported
        );
    }

    public static KnockbackConfig hypixel() {
        return new KnockbackConfig(
                0.42,   // horizontal
                0.36,   // vertical
                0.38,   // verticalLimit
                0.08,   // sprintBonusHorizontal
                0.04,   // sprintBonusVertical
                1.0,    // airMultiplierHorizontal
                1.0,    // airMultiplierVertical
                0.0,    // lookWeight
                false,   // modern
                true    // knockbackSyncSupported
        );
    }
}
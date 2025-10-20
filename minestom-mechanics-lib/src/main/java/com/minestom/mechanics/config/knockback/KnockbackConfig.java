package com.minestom.mechanics.config.knockback;

public record KnockbackConfig(
        double horizontal,
        double vertical,
        double verticalLimit,
        double sprintBonusHorizontal,
        double sprintBonusVertical,
        double airMultiplierHorizontal,
        double airMultiplierVertical,
        double lookWeight,
        boolean modern,  // true = modern, false = legacy
        boolean knockbackSyncSupported,
        boolean skipValidation
) {
    // Validation
    public KnockbackConfig {
        if (!skipValidation) {
            if (horizontal < 0) throw new IllegalArgumentException("Horizontal knockback cannot be negative");
            if (vertical < 0) throw new IllegalArgumentException("Vertical knockback cannot be negative");
            if (verticalLimit < 0) throw new IllegalArgumentException("Vertical limit cannot be negative");
            if (sprintBonusHorizontal < 0) throw new IllegalArgumentException("Sprint bonus horizontal cannot be negative");
            if (sprintBonusVertical < 0) throw new IllegalArgumentException("Sprint bonus vertical cannot be negative");
            if (airMultiplierHorizontal < 0) throw new IllegalArgumentException("Air multiplier horizontal cannot be negative");
            if (airMultiplierVertical < 0) throw new IllegalArgumentException("Air multiplier vertical cannot be negative");
            if (lookWeight < 0 || lookWeight > 1) throw new IllegalArgumentException("Look weight must be between 0 and 1");
        }
    }

    // Base knockback
    public KnockbackConfig withKnockback(double horizontal, double vertical) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    public KnockbackConfig withKnockback(double horizontal, double vertical, double verticalLimit) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    public KnockbackConfig withHorizontal(double horizontal) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    public KnockbackConfig withVertical(double vertical) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    public KnockbackConfig withVerticalLimit(double verticalLimit) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    // Sprint bonuses
    public KnockbackConfig withSprintBonus(double horizontal, double vertical) {
        return new KnockbackConfig(this.horizontal, this.vertical, verticalLimit, horizontal, vertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    public KnockbackConfig withSprintBonusHorizontal(double sprintBonusHorizontal) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    public KnockbackConfig withSprintBonusVertical(double sprintBonusVertical) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    // Air multipliers
    public KnockbackConfig withAirMultipliers(double horizontal, double vertical) {
        return new KnockbackConfig(this.horizontal, this.vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, horizontal, vertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    public KnockbackConfig withAirMultiplierHorizontal(double airMultiplierHorizontal) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    public KnockbackConfig withAirMultiplierVertical(double airMultiplierVertical) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    // Look weight
    public KnockbackConfig withLookWeight(double lookWeight) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    // Modern/Legacy
    public KnockbackConfig withModern(boolean modern) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }

    // Sync support
    public KnockbackConfig withKnockbackSyncSupported(boolean knockbackSyncSupported) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported, false);
    }
}
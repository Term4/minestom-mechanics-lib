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
        boolean knockbackSyncSupported
) {
    // Constructor with validation moved to factory methods
    public KnockbackConfig {
        // No validation in constructor - validation happens in factory methods
    }

    // Factory methods
    public static KnockbackConfig validated(
            double horizontal,
            double vertical,
            double verticalLimit,
            double sprintBonusHorizontal,
            double sprintBonusVertical,
            double airMultiplierHorizontal,
            double airMultiplierVertical,
            double lookWeight,
            boolean modern,
            boolean knockbackSyncSupported
    ) {
        validate(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical,
                airMultiplierHorizontal, airMultiplierVertical, lookWeight);
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal,
                sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight,
                modern, knockbackSyncSupported);
    }

    public static KnockbackConfig unvalidated(
            double horizontal,
            double vertical,
            double verticalLimit,
            double sprintBonusHorizontal,
            double sprintBonusVertical,
            double airMultiplierHorizontal,
            double airMultiplierVertical,
            double lookWeight,
            boolean modern,
            boolean knockbackSyncSupported
    ) {
        return new KnockbackConfig(horizontal, vertical, verticalLimit, sprintBonusHorizontal,
                sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight,
                modern, knockbackSyncSupported);
    }

    // Validation method
    private static void validate(
            double horizontal,
            double vertical,
            double verticalLimit,
            double sprintBonusHorizontal,
            double sprintBonusVertical,
            double airMultiplierHorizontal,
            double airMultiplierVertical,
            double lookWeight
    ) {
        if (horizontal < 0) throw new IllegalArgumentException("Horizontal knockback cannot be negative");
        if (vertical < 0) throw new IllegalArgumentException("Vertical knockback cannot be negative");
        if (verticalLimit < 0) throw new IllegalArgumentException("Vertical limit cannot be negative");
        if (sprintBonusHorizontal < 0) throw new IllegalArgumentException("Sprint bonus horizontal cannot be negative");
        if (sprintBonusVertical < 0) throw new IllegalArgumentException("Sprint bonus vertical cannot be negative");
        if (airMultiplierHorizontal < 0) throw new IllegalArgumentException("Air multiplier horizontal cannot be negative");
        if (airMultiplierVertical < 0) throw new IllegalArgumentException("Air multiplier vertical cannot be negative");
        if (lookWeight < 0 || lookWeight > 1) throw new IllegalArgumentException("Look weight must be between 0 and 1");
    }

    // Base knockback
    public KnockbackConfig withKnockback(double horizontal, double vertical) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    public KnockbackConfig withKnockback(double horizontal, double vertical, double verticalLimit) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    public KnockbackConfig withHorizontal(double horizontal) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    public KnockbackConfig withVertical(double vertical) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    public KnockbackConfig withVerticalLimit(double verticalLimit) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    // Sprint bonuses
    public KnockbackConfig withSprintBonus(double horizontal, double vertical) {
        return validated(this.horizontal, this.vertical, verticalLimit, horizontal, vertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    public KnockbackConfig withSprintBonusHorizontal(double sprintBonusHorizontal) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    public KnockbackConfig withSprintBonusVertical(double sprintBonusVertical) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    // Air multipliers
    public KnockbackConfig withAirMultipliers(double horizontal, double vertical) {
        return validated(this.horizontal, this.vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, horizontal, vertical, lookWeight, modern, knockbackSyncSupported);
    }

    public KnockbackConfig withAirMultiplierHorizontal(double airMultiplierHorizontal) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    public KnockbackConfig withAirMultiplierVertical(double airMultiplierVertical) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    // Look weight
    public KnockbackConfig withLookWeight(double lookWeight) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    // Modern/Legacy
    public KnockbackConfig withModern(boolean modern) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }

    // Sync support
    public KnockbackConfig withKnockbackSyncSupported(boolean knockbackSyncSupported) {
        return validated(horizontal, vertical, verticalLimit, sprintBonusHorizontal, sprintBonusVertical, airMultiplierHorizontal, airMultiplierVertical, lookWeight, modern, knockbackSyncSupported);
    }
}
package com.minestom.mechanics.features.knockback;

import com.minestom.mechanics.features.knockback.KnockbackHandler.KnockbackSettings;

public enum KnockbackProfile {
    VANILLA("Vanilla 1.8", "Standard 1.8.9 knockback",
            KnockbackSettings.builder()
                    .horizontal(0.4)
                    .vertical(0.4)
                    .verticalLimit(0.5)
                    .extraHorizontal(0.5)
                    .extraVertical(0.1)
                    .build(),
            1.0, 1.0, 0.25, false),

    MINEMEN("MinemanClub", "MinemanClub knockback settings",
            KnockbackSettings.builder()
                    .horizontal(0.375)
                    .vertical(0.365)
                    .verticalLimit(0.45)
                    .extraHorizontal(0.34)
                    .extraVertical(0)
                    .build(),
            1.0, 1.0, 0.5, true),


    HYPIXEL("Hypixel", "Hypixel knockback settings",
            KnockbackSettings.builder()
                    .horizontal(0.4)
                    .vertical(0.4)
                    .verticalLimit(0.5)
                    .extraHorizontal(0.4)
                    .extraVertical(0.1)
                    .build(),
            1.0, 1.0, 0.0, false),

    INTAVE("Intave/Grim", "Modern anticheat knockback",
            KnockbackSettings.builder()
                    .horizontal(1)
                    .vertical(1)
                    .verticalLimit(1)
                    .extraHorizontal(2)
                    .extraVertical(2)
                    .build(),
            2, 2, 0.1, true);

    private final String displayName;
    private final String description;
    private final KnockbackSettings settings;
    private final double airHorizontalMultiplier;
    private final double airVerticalMultiplier;
    private final double lookWeight;
    private final boolean knockbackSyncSupported;

    KnockbackProfile(String displayName, String description, KnockbackSettings settings,
                     double airHorizontalMultiplier, double airVerticalMultiplier,
                     double lookWeight, boolean knockbackSyncSupported) {
        this.displayName = displayName;
        this.description = description;
        this.settings = settings;
        this.airHorizontalMultiplier = airHorizontalMultiplier;
        this.airVerticalMultiplier = airVerticalMultiplier;
        this.lookWeight = lookWeight;
        this.knockbackSyncSupported = knockbackSyncSupported;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public KnockbackSettings getSettings() {
        return settings;
    }

    public double getAirHorizontalMultiplier() {
        return airHorizontalMultiplier;
    }

    public double getAirVerticalMultiplier() {
        return airVerticalMultiplier;
    }

    public double getLookWeight() {
        return lookWeight;
    }

    public boolean isKnockbackSyncSupported() {
        return knockbackSyncSupported;
    }
    
    // Convenience methods for KnockbackManager
    public double getHorizontal() {
        return settings.horizontal();
    }
    
    public double getVertical() {
        return settings.vertical();
    }
    
    public double getSprintMultiplier() {
        return settings.extraHorizontal();
    }
    
    public double getyLimit() {
        return settings.verticalLimit();
    }
    
    public double getFriction() {
        return 0.6; // Default friction value, can be made configurable later
    }
}

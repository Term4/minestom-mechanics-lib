package com.minestom.mechanics.config;

import com.minestom.mechanics.config.combat.CombatModeBundle;
import com.minestom.mechanics.config.combat.CombatPresets;
import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.config.gameplay.GameplayConfig;
import com.minestom.mechanics.features.knockback.KnockbackProfile;

/**
 * Preset configurations for MechanicsManager.
 * 
 * Provides common configurations that combine settings from all managers:
 * - Combat settings
 * - Gameplay settings
 * - Hitbox detection
 * - Armor settings
 * - Knockback profiles
 */
public enum MechanicsPresets {
    /**
     * Vanilla Minecraft-like mechanics
     */
    VANILLA(
        "Vanilla",
        "Standard Minecraft mechanics",
        CombatPresets.Modes.VANILLA,
        GameplayConfig.VANILLA,
        CombatPresets.STANDARD_HIT_DETECTION,
        true,
        KnockbackProfile.VANILLA
    ),
    
    /**
     * MinemanClub competitive PvP settings
     */
    MINEMEN(
        "Minemen",
        "MinemanClub competitive PvP configuration",
        CombatPresets.Modes.MINEMEN,
        GameplayConfig.MINEMEN,
        CombatPresets.STANDARD_HIT_DETECTION,
        true,
        KnockbackProfile.MINEMEN
    ),
    
    /**
     * Hypixel-style mechanics
     */
    HYPIXEL(
        "Hypixel",
        "Hypixel server mechanics",
        CombatPresets.Modes.VANILLA, // Hypixel uses mostly vanilla combat
        GameplayConfig.VANILLA,
        HitDetectionConfig.builder()
            .reach(3.0, 4.5)
            .build(),
        true,
        KnockbackProfile.HYPIXEL
    );
    
    private final String name;
    private final String description;
    private final CombatModeBundle combatBundle;
    private final GameplayConfig gameplayConfig;
    private final HitDetectionConfig hitDetectionConfig;
    private final boolean armorEnabled;
    private final KnockbackProfile knockbackProfile;
    
    MechanicsPresets(String name, String description, 
                     CombatModeBundle combatBundle,
                     GameplayConfig gameplayConfig,
                     HitDetectionConfig hitDetectionConfig,
                     boolean armorEnabled,
                     KnockbackProfile knockbackProfile) {
        this.name = name;
        this.description = description;
        this.combatBundle = combatBundle;
        this.gameplayConfig = gameplayConfig;
        this.hitDetectionConfig = hitDetectionConfig;
        this.armorEnabled = armorEnabled;
        this.knockbackProfile = knockbackProfile;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public CombatModeBundle getCombatBundle() {
        return combatBundle;
    }
    
    public GameplayConfig getGameplayConfig() {
        return gameplayConfig;
    }
    
    public HitDetectionConfig getHitDetectionConfig() {
        return hitDetectionConfig;
    }
    
    public boolean isArmorEnabled() {
        return armorEnabled;
    }
    
    public KnockbackProfile getKnockbackProfile() {
        return knockbackProfile;
    }
}

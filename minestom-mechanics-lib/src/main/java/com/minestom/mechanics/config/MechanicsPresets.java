package com.minestom.mechanics.config;

import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.config.combat.CombatPresets;
import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.config.gameplay.GameplayConfig;
import com.minestom.mechanics.config.gameplay.DamageConfig;
import com.minestom.mechanics.config.gameplay.DamagePresets;
import com.minestom.mechanics.features.knockback.KnockbackProfile;

public enum MechanicsPresets {
    VANILLA(
            "Vanilla",
            "Standard Minecraft mechanics",
            CombatPresets.VANILLA,
            GameplayConfig.VANILLA,              // ✅ GameplayConfig for eye height, movement, etc.
            DamagePresets.VANILLA,                // ✅ DamageConfig for damage/invuln
            HitDetectionConfig.standard(),
            true,
            KnockbackProfile.VANILLA
    ),

    MINEMEN(
            "Minemen",
            "MinemanClub competitive PvP configuration",
            CombatPresets.MINEMEN,
            GameplayConfig.MINEMEN,               // ✅ GameplayConfig
            DamagePresets.MINEMEN,                // ✅ DamageConfig
            HitDetectionConfig.standard(),
            true,
            KnockbackProfile.MINEMEN
    ),

    HYPIXEL(
            "Hypixel",
            "Hypixel server mechanics",
            CombatPresets.HYPIXEL,
            GameplayConfig.VANILLA,               // ✅ GameplayConfig
            DamagePresets.HYPIXEL,                // ✅ DamageConfig
            HitDetectionConfig.standard(),
            true,
            KnockbackProfile.HYPIXEL
    );

    private final String name;
    private final String description;
    private final CombatConfig combatConfig;
    private final GameplayConfig gameplayConfig;      // ✅ BOTH configs
    private final DamageConfig damageConfig;
    private final HitDetectionConfig hitDetectionConfig;
    private final boolean armorEnabled;
    private final KnockbackProfile knockbackProfile;

    MechanicsPresets(String name, String description,
                     CombatConfig combatConfig,
                     GameplayConfig gameplayConfig,       // ✅ BOTH parameters
                     DamageConfig damageConfig,
                     HitDetectionConfig hitDetectionConfig,
                     boolean armorEnabled,
                     KnockbackProfile knockbackProfile) {
        this.name = name;
        this.description = description;
        this.combatConfig = combatConfig;
        this.gameplayConfig = gameplayConfig;          // ✅ Store both
        this.damageConfig = damageConfig;
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

    public CombatConfig getCombatConfig() {
        return combatConfig;
    }

    public GameplayConfig getGameplayConfig() {         // ✅ Getter for GameplayConfig
        return gameplayConfig;
    }

    public DamageConfig getDamageConfig() {             // ✅ Getter for DamageConfig
        return damageConfig;
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
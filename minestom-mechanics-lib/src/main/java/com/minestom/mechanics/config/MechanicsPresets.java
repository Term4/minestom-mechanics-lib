package com.minestom.mechanics.config;

import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.config.combat.CombatPresets;
import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.config.gameplay.GameplayConfig;
import com.minestom.mechanics.config.gameplay.DamageConfig;
import com.minestom.mechanics.config.gameplay.DamagePresets;
import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.config.knockback.KnockbackPresets;

public enum MechanicsPresets {
    VANILLA(
            "Vanilla",
            "Standard Minecraft mechanics",
            CombatPresets.VANILLA,
            GameplayConfig.VANILLA,              // ✅ GameplayConfig for eye height, movement, etc.
            DamagePresets.VANILLA,                // ✅ DamageConfig for damage/invuln
            HitDetectionConfig.standard(),
            true,
            KnockbackPresets.vanilla18()
    ),

    MINEMEN(
            "Minemen",
            "MinemanClub competitive PvP configuration",
            CombatPresets.MINEMEN,
            GameplayConfig.MINEMEN,               // ✅ GameplayConfig
            DamagePresets.MINEMEN,                // ✅ DamageConfig
            HitDetectionConfig.standard(),
            true,
            KnockbackPresets.minemen()
    ),

    HYPIXEL(
            "Hypixel",
            "Hypixel server mechanics",
            CombatPresets.HYPIXEL,
            GameplayConfig.VANILLA,               // ✅ GameplayConfig
            DamagePresets.HYPIXEL,                // ✅ DamageConfig
            HitDetectionConfig.standard(),
            true,
            KnockbackPresets.hypixel()
    );

    private final String name;
    private final String description;
    private final CombatConfig combatConfig;
    private final GameplayConfig gameplayConfig;      // ✅ BOTH configs
    private final DamageConfig damageConfig;
    private final HitDetectionConfig hitDetectionConfig;
    private final boolean armorEnabled;
    private final KnockbackConfig knockbackConfig;

    MechanicsPresets(String name, String description,
                     CombatConfig combatConfig,
                     GameplayConfig gameplayConfig,
                     DamageConfig damageConfig,
                     HitDetectionConfig hitDetectionConfig,
                     boolean armorEnabled,
                     KnockbackConfig knockbackConfig) {
        this.name = name;
        this.description = description;
        this.combatConfig = combatConfig;
        this.gameplayConfig = gameplayConfig;
        this.damageConfig = damageConfig;
        this.hitDetectionConfig = hitDetectionConfig;
        this.armorEnabled = armorEnabled;
        this.knockbackConfig = knockbackConfig;
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

    public KnockbackConfig getKnockbackConfig() {
        return knockbackConfig;
    }
}
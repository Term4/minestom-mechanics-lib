package com.minestom.mechanics.config.combat;

import com.minestom.mechanics.features.knockback.KnockbackProfile;
import com.minestom.mechanics.config.blocking.BlockingConfig;
import com.minestom.mechanics.config.world.WorldInteractionConfig;
import com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig;
import com.minestom.mechanics.projectile.config.FishingRodConfig;
import com.minestom.mechanics.projectile.config.FishingRodKnockbackConfig;


/**
 * Static constants for common combat configurations.
 * Provides the "preset support" users want - easy access to common setups.
 * 
 * This class contains:
 * - Individual config presets (combat rules, hit detection, etc.)
 * - Full mode bundles (complete combat modes)
 * - Mix-and-match presets for customization
 */
public class CombatPresets {
    
    private CombatPresets() {
        // Static class - prevent instantiation
    }
    
    // ===========================
    // INDIVIDUAL CONFIG PRESETS
    // ===========================
    
    /**
     * Standard combat rules for 1.8-style PvP.
     */
    public static final CombatRulesConfig STANDARD_COMBAT_RULES = CombatRulesConfig.builder()
            .removeAttackCooldown(true) // 1.8 style instant attacks
            .knockback(KnockbackProfile.VANILLA)
            .criticalMultiplier(1.5f)
            .allowSprintCrits(false)
            .sprintWindow(false, false, 4)
            .build();
    
    /**
     * MinemanClub-style combat rules (fast combo combat).
     */
    public static final CombatRulesConfig MINEMEN_COMBAT_RULES = CombatRulesConfig.builder()
            .removeAttackCooldown(true) // 1.8 style instant attacks
            .knockback(KnockbackProfile.MINEMEN)
            .criticalMultiplier(1.5f)
            .allowSprintCrits(true)
            .sprintWindow(true, false, 5)
            .build();
    
    /**
     * Standard hit detection (3.0 server-side, 5.0 attack packet).
     */
    public static final HitDetectionConfig STANDARD_HIT_DETECTION = HitDetectionConfig.builder()
            .reach(3.0, 5.0)
            .hitboxExpansion(0.1, 0.2)
            .build();
    
    /**
     * Strict hit detection for anticheat compatibility.
     */
    public static final HitDetectionConfig STRICT_HIT_DETECTION = HitDetectionConfig.builder()
            .reach(3.0, 4.5)
            .hitboxExpansion(0.1, 0.105)
            .build();
    
    // Damage configuration has been moved to GameplayConfig to avoid duplication
    
    /**
     * Standard blocking configuration.
     */
    public static final BlockingConfig STANDARD_BLOCKING = BlockingConfig.builder()
            .enabled(true)
            .damageReduction(0.5)
            .knockbackHorizontalMultiplier(0.95)
            .knockbackVerticalMultiplier(0.95)
            .showDamageMessages(true)
            .showBlockEffects(true)
            .build();
    
    /**
     * Default projectile configuration with 1.8 vanilla values.
     */
    public static final ProjectileConfig DEFAULT_PROJECTILES = ProjectileConfig.builder()
            .arrowKnockback(ProjectileKnockbackConfig.builder()
                    .enabled(true)
                    .horizontalKnockback(0.4)  // 1.8 vanilla arrow
                    .verticalKnockback(0.1)
                    .verticalLimit(0.5)
                    .build())
            .snowballKnockback(ProjectileKnockbackConfig.builder()
                    .enabled(true)
                    .horizontalKnockback(0.3)  // 1.8 vanilla snowball
                    .verticalKnockback(0.05)
                    .verticalLimit(0.45)
                    .build())
            .eggKnockback(ProjectileKnockbackConfig.builder()
                    .enabled(true)
                    .horizontalKnockback(0.2)  // 1.8 vanilla egg
                    .verticalKnockback(0.05)
                    .verticalLimit(0.45)
                    .build())
            .enderPearlKnockback(ProjectileKnockbackConfig.disabled())  // No knockback for ender pearls
            .fishingRodKnockback(FishingRodKnockbackConfig.defaultConfig()) // Standard rod knockback
            .fishingRod(FishingRodConfig.defaultConfig()) // Standard rod cast velocity
            .build();
    
    /**
     * Minemen-style projectile configuration with higher knockback.
     */
    public static final ProjectileConfig MINEMEN_PROJECTILES = ProjectileConfig.builder()
            .arrowKnockback(ProjectileKnockbackConfig.builder()
                    .enabled(true)
                    .horizontalKnockback(0.525)  // Higher than vanilla
                    .verticalKnockback(0.365)
                    .verticalLimit(0.45)
                    .build())
            .snowballKnockback(ProjectileKnockbackConfig.builder()
                    .enabled(true)
                    .horizontalKnockback(0.525)
                    .verticalKnockback(0.365)
                    .verticalLimit(0.45)
                    .build())
            .eggKnockback(ProjectileKnockbackConfig.builder()
                    .enabled(true)
                    .horizontalKnockback(0.525)
                    .verticalKnockback(0.365)
                    .verticalLimit(0.45)
                    .build())
            .enderPearlKnockback(ProjectileKnockbackConfig.disabled())
            .fishingRodKnockback(FishingRodKnockbackConfig.builder()
                    .baseConfig(new ProjectileKnockbackConfig(true, 0.4, 0.3, 0.4))
                    .mode(FishingRodKnockbackConfig.KnockbackMode.BOBBER_RELATIVE)
                    .build()) // Stronger rod knockback for Minemen
            .fishingRod(FishingRodConfig.strongCast()) // Stronger cast for competitive play
            .build();
    
    /**
     * Standard world interaction configuration.
     */
    public static final WorldInteractionConfig STANDARD_WORLD_INTERACTION = WorldInteractionConfig.builder()
            .blockReach(6.0, 4.5)
            .blockRaycastStep(0.2)
            .build();
    
    // ===========================
    // FULL MODE BUNDLES
    // ===========================
    
    /**
     * Complete combat mode presets.
     */
    public static class Modes {
        
        /**
         * MinemanClub/MMC style - fast-paced combo combat.
         */
        public static final CombatModeBundle MINEMEN = CombatModeBundle.builder()
                .name("MinemanClub")
                .description("Fast combo combat with 50ms invulnerability")
                .combatRules(MINEMEN_COMBAT_RULES)
                .blocking(STANDARD_BLOCKING)
                .projectiles(MINEMEN_PROJECTILES)
                .build();
        
        /**
         * Vanilla 1.8 - classic Minecraft PvP.
         */
        public static final CombatModeBundle VANILLA = CombatModeBundle.builder()
                .name("Vanilla 1.8")
                .description("Classic 1.8 PvP mechanics")
                .combatRules(STANDARD_COMBAT_RULES)
                .blocking(STANDARD_BLOCKING)
                .projectiles(DEFAULT_PROJECTILES)
                .build();
        
        /**
         * Hypixel style - similar to vanilla but tweaked.
         */
        public static final CombatModeBundle HYPIXEL = CombatModeBundle.builder()
                .name("Hypixel")
                .description("Hypixel network combat style")
                .combatRules(CombatRulesConfig.builder()
                        .removeAttackCooldown(true)
                        .knockback(KnockbackProfile.HYPIXEL)
                        .criticalMultiplier(1.5f)
                        .allowSprintCrits(true)
                        .sprintWindow(true, false, 5)
                        .build())
                .blocking(STANDARD_BLOCKING)
                .projectiles(DEFAULT_PROJECTILES)
                .build();
        
    }
}

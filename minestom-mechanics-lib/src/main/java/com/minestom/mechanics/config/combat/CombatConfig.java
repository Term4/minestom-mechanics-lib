package com.minestom.mechanics.config.combat;

import com.minestom.mechanics.config.knockback.KnockbackConfig;

import static com.minestom.mechanics.config.constants.CombatConstants.MAX_BLOCKING_REDUCTION;
import static com.minestom.mechanics.config.constants.CombatConstants.MIN_BLOCKING_REDUCTION;

/**
 * Pure combat configuration - attack mechanics, knockback, and blocking.
 * Does NOT include general damage/gameplay mechanics (see GameplayConfig).
 *
 * Usage:
 * <pre>
 * CombatConfig combat = CombatPresets.MINEMEN
 *     .withKnockback(0.45, 0.38)
 *     .withBlockDamageReduction(0.6);
 * </pre>
 */
public record CombatConfig(
        // Attack mechanics
        // TODO: could be good to have some way to force 1.9+ attack cooldowns on 1.8?
        //  I mean they would be applied server side already (I think???) but we'd need a way
        //  for them to see it.
        boolean removeAttackCooldown,
        float criticalMultiplier,
        boolean allowSprintCrits,

        // Knockback
        KnockbackConfig knockbackConfig,

        // Sprint window
        boolean dynamicSprintWindow,
        boolean sprintWindowDouble,
        int sprintWindowMaxTicks,

        // Blocking (combat-specific)
        boolean blockingEnabled,
        double blockDamageReduction,
        double blockKnockbackHReduction,
        double blockKnockbackVReduction,
        boolean showBlockDamageMessages,
        boolean showBlockEffects
) {

    // Validation
    public CombatConfig {
        if (criticalMultiplier < 1.0f)
            throw new IllegalArgumentException("Critical multiplier must be >= 1.0");
        if (sprintWindowMaxTicks < 0)
            throw new IllegalArgumentException("Sprint window max ticks must be >= 0");
        if (blockDamageReduction < MIN_BLOCKING_REDUCTION || blockDamageReduction > MAX_BLOCKING_REDUCTION)
            throw new IllegalArgumentException("Block damage reduction must be between " +
                    MIN_BLOCKING_REDUCTION + " and " + MAX_BLOCKING_REDUCTION);
        if (blockKnockbackHReduction < 0 || blockKnockbackHReduction > 1)
            throw new IllegalArgumentException("Block knockback H multiplier must be between 0 and 1");
        if (blockKnockbackVReduction < 0 || blockKnockbackVReduction > 1)
            throw new IllegalArgumentException("Block knockback V multiplier must be between 0 and 1");
        if (knockbackConfig == null)
            throw new IllegalArgumentException("Knockback config cannot be null");
    }

    // ===== KNOCKBACK =====

    public CombatConfig withKnockbackConfig(KnockbackConfig knockbackConfig) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withKnockback(double horizontal, double vertical) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withKnockback(horizontal, vertical), dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withKnockback(double horizontal, double vertical, double verticalLimit) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withKnockback(horizontal, vertical, verticalLimit), dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withSprintBonus(double horizontal, double vertical) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withSprintBonus(horizontal, vertical), dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withAirMultipliers(double horizontal, double vertical) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withAirMultipliers(horizontal, vertical), dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withLookWeight(double lookWeight) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withLookWeight(lookWeight), dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withModern(boolean modern) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withModern(modern), dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withKnockbackSyncSupported(boolean syncSupported) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withKnockbackSyncSupported(syncSupported), dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    // ===== ATTACK =====

    public CombatConfig withAttackCooldown(boolean remove) {
        return new CombatConfig(remove, criticalMultiplier, allowSprintCrits,
                knockbackConfig, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockingEnabled, blockDamageReduction, blockKnockbackHReduction,
                blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withCriticalMultiplier(float multiplier) {
        return new CombatConfig(removeAttackCooldown, multiplier, allowSprintCrits,
                knockbackConfig, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockingEnabled, blockDamageReduction, blockKnockbackHReduction,
                blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withSprintCrits(boolean allow) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allow,
                knockbackConfig, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockingEnabled, blockDamageReduction, blockKnockbackHReduction,
                blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects);
    }

    // ===== SPRINT WINDOW =====

    public CombatConfig withSprintWindow(boolean dynamic, boolean doubleHit, int maxTicks) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, dynamic, doubleHit, maxTicks, blockingEnabled,
                blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction,
                showBlockDamageMessages, showBlockEffects);
    }

    // ===== BLOCKING =====

    public CombatConfig withBlockDamageReduction(double reduction) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockingEnabled, reduction, blockKnockbackHReduction, blockKnockbackVReduction,
                showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withBlockKnockback(double horizontal, double vertical) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockingEnabled, blockDamageReduction, horizontal, vertical,
                showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withBlockEffects(boolean showMessages, boolean showEffects) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockingEnabled, blockDamageReduction, blockKnockbackHReduction,
                blockKnockbackVReduction, showMessages, showEffects);
    }
}
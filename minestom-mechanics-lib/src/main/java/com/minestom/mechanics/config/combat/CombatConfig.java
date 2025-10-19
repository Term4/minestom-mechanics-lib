package com.minestom.mechanics.config.combat;

import com.minestom.mechanics.features.knockback.KnockbackProfile;
import static com.minestom.mechanics.config.combat.CombatConstants.*;

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
        boolean removeAttackCooldown,
        float criticalMultiplier,
        boolean allowSprintCrits,

        // Knockback
        KnockbackProfile knockbackProfile,

        // Sprint window
        boolean dynamicSprintWindow,
        boolean sprintWindowDouble,
        int sprintWindowMaxTicks,

        // Blocking (combat-specific)
        double blockDamageReduction,
        double blockKnockbackHorizontal,
        double blockKnockbackVertical,
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
        if (blockKnockbackHorizontal < 0 || blockKnockbackHorizontal > 1)
            throw new IllegalArgumentException("Block knockback horizontal must be between 0 and 1");
        if (blockKnockbackVertical < 0 || blockKnockbackVertical > 1)
            throw new IllegalArgumentException("Block knockback vertical must be between 0 and 1");
        if (knockbackProfile == null)
            throw new IllegalArgumentException("Knockback profile cannot be null");
    }

    // ===== KNOCKBACK =====

    public CombatConfig withKnockbackProfile(KnockbackProfile profile) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                profile, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockDamageReduction, blockKnockbackHorizontal, blockKnockbackVertical,
                showBlockDamageMessages, showBlockEffects);
    }


    // TODO: Update knockbackprofile (and other knockback systems)
    //  Don't worry about adding specific horizontal / vertical methods, as if someone
    //  wants to change actual knockback values, they can change all of them. Fuck that.
    public CombatConfig withKnockback(double horizontal, double vertical) {
        // Note: Cannot create custom KnockbackProfile (it's an enum)
        // This method is for future when KnockbackProfile becomes a record
        // For now, use withKnockbackProfile() with existing profiles
        throw new UnsupportedOperationException(
                "Custom knockback values not yet supported. Use withKnockbackProfile() with predefined profiles.");
    }

    // Note: Individual horizontal/vertical not supported until KnockbackProfile becomes a record
    // Use withKnockbackProfile() with predefined profiles for now

    // ===== ATTACK =====

    public CombatConfig withAttackCooldown(boolean remove) {
        return new CombatConfig(remove, criticalMultiplier, allowSprintCrits,
                knockbackProfile, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockDamageReduction, blockKnockbackHorizontal, blockKnockbackVertical,
                showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withCriticalMultiplier(float multiplier) {
        return new CombatConfig(removeAttackCooldown, multiplier, allowSprintCrits,
                knockbackProfile, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockDamageReduction, blockKnockbackHorizontal, blockKnockbackVertical,
                showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withSprintCrits(boolean allow) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allow,
                knockbackProfile, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockDamageReduction, blockKnockbackHorizontal, blockKnockbackVertical,
                showBlockDamageMessages, showBlockEffects);
    }

    // ===== SPRINT WINDOW =====

    public CombatConfig withSprintWindow(boolean dynamic, boolean doubleHit, int maxTicks) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackProfile, dynamic, doubleHit, maxTicks,
                blockDamageReduction, blockKnockbackHorizontal, blockKnockbackVertical,
                showBlockDamageMessages, showBlockEffects);
    }

    // ===== BLOCKING =====

    public CombatConfig withBlockDamageReduction(double reduction) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackProfile, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                reduction, blockKnockbackHorizontal, blockKnockbackVertical,
                showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withBlockKnockback(double horizontal, double vertical) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackProfile, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockDamageReduction, horizontal, vertical,
                showBlockDamageMessages, showBlockEffects);
    }

    public CombatConfig withBlockEffects(boolean showMessages, boolean showEffects) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackProfile, dynamicSprintWindow, sprintWindowDouble, sprintWindowMaxTicks,
                blockDamageReduction, blockKnockbackHorizontal, blockKnockbackVertical,
                showMessages, showEffects);
    }
}
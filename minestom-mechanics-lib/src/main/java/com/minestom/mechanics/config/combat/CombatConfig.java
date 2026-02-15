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
        //  --> attack cooldown charging with tags ???

        // TODO: Move out from combat (needs to be separate)
        boolean removeAttackCooldown,
        float criticalMultiplier,
        boolean allowSprintCrits,

        // Knockback
        KnockbackConfig knockbackConfig,

        // Sprint window: ticks after stopping sprint we still apply sprint bonus. 0 = disabled (only current state).
        int sprintWindowTicks,

        // Swing hit window: ticks after attacker hits victim when swings can register as attacks (Minemen-style). 0 = disabled.
        int swingHitWindowTicks,
        // Ticks after each swing to poll look direction. When > 0, the hit can land when crosshair passes over victim during this window (not just at swing moment). 0 = check only at swing packet.
        int swingLookCheckTicks,
        // Victim-to-attacker: ticks after victim takes damage when victim can swing-hit their attacker. 0 = disabled.
        int victimSwingHitWindowTicks,
        // Ticks after victim's swing to poll look for victim→attacker hit. 0 = check only at swing moment.
        int victimSwingLookCheckTicks,

        // Hit queue / damage replacement (combat-style)
        // When true, don't apply damage replacement when attacker uses same item type (or fist) as previous hit (Minemen).
        boolean noReplacementSameItem,
        // Hits in last N ticks of invulnerability get queued for when invuln ends. 0 = disabled.
        int attackerInvulnerabilityBufferTicks,

        // Blocking (combat-specific)
        boolean blockingEnabled,
        double blockDamageReduction,
        // Knockback reduction: 0 = none, 1 = full (e.g. 0.6 = 60% reduction)
        double blockKnockbackHReduction,
        double blockKnockbackVReduction,
        boolean showBlockDamageMessages,
        boolean showBlockEffects,

        // Legacy 1.8: when true, suppress sprint metadata/attribute packets for 1.8 clients (hit slowdown fix).
        boolean fix18HitSlowdown
) {

    // Validation
    public CombatConfig {
        if (criticalMultiplier < 1.0f)
            throw new IllegalArgumentException("Critical multiplier must be >= 1.0");
        if (sprintWindowTicks < 0)
            throw new IllegalArgumentException("Sprint window ticks must be >= 0");
        if (swingHitWindowTicks < 0)
            throw new IllegalArgumentException("Swing hit window ticks must be >= 0");
        if (swingLookCheckTicks < 0)
            throw new IllegalArgumentException("Swing look check ticks must be >= 0");
        if (victimSwingHitWindowTicks < 0)
            throw new IllegalArgumentException("Victim swing hit window ticks must be >= 0");
        if (victimSwingLookCheckTicks < 0)
            throw new IllegalArgumentException("Victim swing look check ticks must be >= 0");
        if (attackerInvulnerabilityBufferTicks < 0)
            throw new IllegalArgumentException("Attacker invulnerability buffer ticks must be >= 0");
        if (blockDamageReduction < MIN_BLOCKING_REDUCTION || blockDamageReduction > MAX_BLOCKING_REDUCTION)
            throw new IllegalArgumentException("Block damage reduction must be between " +
                    MIN_BLOCKING_REDUCTION + " and " + MAX_BLOCKING_REDUCTION);
        if (blockKnockbackHReduction < 0 || blockKnockbackHReduction > 1)
            throw new IllegalArgumentException("Block knockback H reduction must be between 0 and 1");
        if (blockKnockbackVReduction < 0 || blockKnockbackVReduction > 1)
            throw new IllegalArgumentException("Block knockback V reduction must be between 0 and 1");
        if (knockbackConfig == null)
            throw new IllegalArgumentException("Knockback config cannot be null");
    }

    // ===== KNOCKBACK =====

    public CombatConfig withKnockbackConfig(KnockbackConfig knockbackConfig) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withKnockback(double horizontal, double vertical) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withKnockback(horizontal, vertical), sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withKnockback(double horizontal, double vertical, double verticalLimit) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withKnockback(horizontal, vertical, verticalLimit), sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withSprintBonus(double horizontal, double vertical) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withSprintBonus(horizontal, vertical), sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withAirMultipliers(double horizontal, double vertical) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withAirMultipliers(horizontal, vertical), sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withLookWeight(double lookWeight) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withLookWeight(lookWeight), sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withModern(boolean modern) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withModern(modern), sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withKnockbackSyncSupported(boolean syncSupported) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits, knockbackConfig.withKnockbackSyncSupported(syncSupported), sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    // ===== ATTACK =====

    public CombatConfig withAttackCooldown(boolean remove) {
        return new CombatConfig(remove, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks,
                noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction,
                blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withCriticalMultiplier(float multiplier) {
        return new CombatConfig(removeAttackCooldown, multiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks,
                noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction,
                blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withSprintCrits(boolean allow) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allow,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks,
                noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction,
                blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    // ===== SPRINT WINDOW =====

    public CombatConfig withSprintWindowTicks(int ticks) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, ticks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled,
                blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction,
                showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withSwingHitWindowTicks(int ticks) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, ticks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks,
                noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction,
                showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withSwingLookCheckTicks(int ticks) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, ticks,
                victimSwingHitWindowTicks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction,
                showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withVictimSwingHitWindowTicks(int ticks) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks,
                ticks, victimSwingLookCheckTicks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction,
                showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withVictimSwingLookCheckTicks(int ticks) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks,
                victimSwingHitWindowTicks, ticks, noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction,
                showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    // ===== HIT QUEUE / DAMAGE REPLACEMENT =====

    public CombatConfig withNoReplacementSameItem(boolean noReplacementSameItem) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks,
                noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction,
                showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withAttackerInvulnerabilityBufferTicks(int ticks) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks,
                noReplacementSameItem, ticks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction, blockKnockbackVReduction,
                showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    // ===== BLOCKING =====

    public CombatConfig withBlockDamageReduction(double reduction) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks,
                noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, reduction, blockKnockbackHReduction, blockKnockbackVReduction,
                showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withBlockKnockback(double horizontal, double vertical) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks,
                noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, horizontal, vertical,
                showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }

    public CombatConfig withBlockEffects(boolean showMessages, boolean showEffects) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks,
                noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction,
                blockKnockbackVReduction, showMessages, showEffects, fix18HitSlowdown);
    }

    // ===== LEGACY 1.8 =====

    public CombatConfig withFix18HitSlowdown(boolean fix18HitSlowdown) {
        return new CombatConfig(removeAttackCooldown, criticalMultiplier, allowSprintCrits,
                knockbackConfig, sprintWindowTicks, swingHitWindowTicks, swingLookCheckTicks, victimSwingHitWindowTicks, victimSwingLookCheckTicks,
                noReplacementSameItem, attackerInvulnerabilityBufferTicks, blockingEnabled, blockDamageReduction, blockKnockbackHReduction,
                blockKnockbackVReduction, showBlockDamageMessages, showBlockEffects, fix18HitSlowdown);
    }
}
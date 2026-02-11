package com.minestom.mechanics.systems.health.damage;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Result of the damage pipeline, returned by {@link DamageType#processDamage}.
 * Contains all context needed for post-damage logic (knockback, effects, etc.).
 */
public record DamageResult(
        boolean applied,
        boolean wasReplacement,
        float finalDamage,
        DamageTypeProperties props,
        @Nullable Entity attacker,
        LivingEntity victim
) {
    /** Damage was blocked (creative, i-frames, disabled, etc.) */
    public static DamageResult blocked(DamageTypeProperties props, @Nullable Entity attacker, LivingEntity victim) {
        return new DamageResult(false, false, 0, props, attacker, victim);
    }
}

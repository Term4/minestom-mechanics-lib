package com.minestom.mechanics.systems.health.damage;

import net.minestom.server.coordinate.Pos;
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
        @Nullable Entity source,          // direct source: player (melee) or projectile (ranged)
        @Nullable Entity attacker,        // the player who caused the damage
        LivingEntity victim,
        @Nullable Pos shooterOriginPos    // where the shooter was when projectile launched (for KB direction)
) {
    /** Damage was blocked. */
    public static DamageResult blocked(DamageTypeProperties props, @Nullable Entity source, @Nullable Entity attacker, LivingEntity victim) {
        return new DamageResult(false, false, 0, props, source, attacker, victim, null);
    }
}

package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.systems.misc.VelocityEstimator;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.minestom.mechanics.config.constants.CombatConstants.MIN_KNOCKBACK_DISTANCE;

// TODO: Potentially add another knockback calculator for modern knockback
//  (the formula is entirely different) would lead to this being called "LegacyKnockbackCalculator"
//  Actually I think the only change would need to be in

/**
 * Component responsible for calculating knockback strength and direction.
 * Extracted from monolithic KnockbackHandler for better maintainability.
 */
public class KnockbackCalculator {

    private static final Logger log = LoggerFactory.getLogger(KnockbackCalculator.class);
    private final KnockbackConfig config;

    public KnockbackCalculator(KnockbackConfig config) {
        this.config = config;
    }
    
    /**
     * Calculate knockback direction based on the configured mode.
     * Position-based modes blend with attacker/shooter look via lookWeight; VICTIM_FACING blends
     * position (away-from-attacker) with victim's facing via lookWeight.
     *
     * @param lookWeight 0–1; for position modes blends with attacker/shooter look; for VICTIM_FACING blends with victim look
     * @param degenerateFallback behavior when victim and origin are very close
     */
    public Vec calculateDirection(KnockbackSystem.KnockbackDirectionMode mode, LivingEntity victim,
                                  @org.jetbrains.annotations.Nullable Entity attacker,
                                  @org.jetbrains.annotations.Nullable Entity source,
                                  @org.jetbrains.annotations.Nullable Pos shooterOriginPos,
                                  double lookWeight,
                                  KnockbackSystem.DegenerateFallback degenerateFallback) {
        Pos lookSource = shooterOriginPos != null ? shooterOriginPos
                : (attacker != null ? attacker.getPosition() : null);
        Vec lookDir = lookSource != null ? directionFromLook(lookSource) : null;

        Pos origin = switch (mode) {
            case ATTACKER_POSITION -> attacker != null ? attacker.getPosition() : null;
            case SHOOTER_ORIGIN -> shooterOriginPos;
            case PROJECTILE_POSITION -> source != null ? source.getPosition() : null;
            case VICTIM_FACING -> (attacker != null || source != null)
                    ? (attacker != null ? attacker : source).getPosition() : null;
        };

        boolean degenerate = origin != null && horizontalDistance(victim.getPosition(), origin) < MIN_KNOCKBACK_DISTANCE;

        if (mode == KnockbackSystem.KnockbackDirectionMode.VICTIM_FACING) {
            Vec victimLook = directionFromLook(victim.getPosition());
            Vec posDir = origin != null
                    ? directionFromPosRaw(victim, origin, victimLook)
                    : fallback(victimLook, degenerateFallback, victimLook);
            return blendDirection(posDir, victimLook, lookWeight);
        }

        if (degenerate) {
            return fallback(lookDir, degenerateFallback, lookDir);
        }

        Vec base = switch (mode) {
            case ATTACKER_POSITION -> attacker != null
                    ? directionFromPosRaw(victim, attacker.getPosition(), lookDir)
                    : fallback(lookDir, degenerateFallback, lookDir);
            case SHOOTER_ORIGIN -> shooterOriginPos != null
                    ? directionFromPosRaw(victim, shooterOriginPos, lookDir)
                    : fallback(lookDir, degenerateFallback, lookDir);
            case PROJECTILE_POSITION -> source != null
                    ? directionFromPosRaw(victim, source.getPosition(), lookDir)
                    : fallback(lookDir, degenerateFallback, lookDir);
            case VICTIM_FACING -> throw new IllegalStateException("handled above");
        };

        if (lookWeight <= 0) return base;
        Vec lookDirForBlend = lookDir != null ? lookDir : base;
        return blendDirection(base, lookDirForBlend, lookWeight);
    }

    private double horizontalDistance(Pos a, Pos b) {
        double dx = a.x() - b.x();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private Vec fallback(Vec preferred, KnockbackSystem.DegenerateFallback mode, Vec lookDir) {
        return switch (mode) {
            case LOOK -> preferred != null ? preferred : randomDirection();
            case RANDOM -> randomDirection();
        };
    }

    private Vec fallback(Vec preferred) {
        return preferred != null ? preferred : randomDirection();
    }

    private Vec blendDirection(Vec positionDir, Vec lookDir, double lookWeight) {
        double dx = positionDir.x() * (1 - lookWeight) + lookDir.x() * lookWeight;
        double dz = positionDir.z() * (1 - lookWeight) + lookDir.z() * lookWeight;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < MIN_KNOCKBACK_DISTANCE) return lookDir;
        return new Vec(dx / dist, 0, dz / dist);
    }

    /** Direction from origin toward victim (normalized). Uses fallback when victim and origin are degenerate. */
    private Vec directionFromPosRaw(LivingEntity victim, Pos origin, @org.jetbrains.annotations.Nullable Vec fallbackIfDegenerate) {
        double dx = victim.getPosition().x() - origin.x();
        double dz = victim.getPosition().z() - origin.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < MIN_KNOCKBACK_DISTANCE) return fallback(fallbackIfDegenerate);
        // TODO: Might can allow up direction
        return new Vec(dx / distance, 0, dz / distance);
    }

    /** Direction from an entity's yaw (look direction). */
    private Vec directionFromLook(Pos pos) {
        double yaw = Math.toRadians(pos.yaw());
        return new Vec(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    /** Random direction fallback. */
    private Vec randomDirection() {
        double dx = Math.random() * 0.02 - 0.01;
        double dz = Math.random() * 0.02 - 0.01;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return dist > 0 ? new Vec(dx / dist, 0, dz / dist) : new Vec(1, 0, 0);
    }

    /**
     * Calculate knockback direction for projectiles (from origin position, not attacker position).
     */
    public Vec calculateProjectileKnockbackDirection(LivingEntity victim, Pos projectileOrigin) {

        double dx = victim.getPosition().x() - projectileOrigin.x();
        double dz = victim.getPosition().z() - projectileOrigin.z();

        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < MIN_KNOCKBACK_DISTANCE) {
            dx = Math.random() * 0.02 - 0.01;
            dz = Math.random() * 0.02 - 0.01;
            distance = Math.sqrt(dx * dx + dz * dz);
        }

        dx /= distance;
        dz /= distance;

        return new Vec(dx, 0, dz);
    }

    /**
     * Calculate final velocity from knockback components. Uses {@link VelocityEstimator}
     * for victim velocity so knockback properly accounts for existing motion.
     *
     * @param horizontalFriction divisor for old x/z velocity (2 = retain 50%). 0 = ignore old velocity.
     * @param verticalFriction divisor for old y velocity. 0 = ignore old velocity.
     * @param verticalLimit cap for upward vertical component (blocks/tick). Applied after calculation.
     */
    public Vec calculateFinalVelocity(LivingEntity victim, Vec direction, KnockbackSystem.KnockbackStrength strength,
                                      KnockbackSystem.KnockbackType type,
                                      double horizontalFriction, double verticalFriction,
                                      double verticalLimit) {
        double tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        double horizontal = strength.horizontal() / tps;
        double vertical = strength.vertical() / tps;

        Vec oldVelocity = VelocityEstimator.getVelocity(victim);

        double oldX = horizontalFriction > 0 ? oldVelocity.x() / horizontalFriction : 0;
        double oldZ = horizontalFriction > 0 ? oldVelocity.z() / horizontalFriction : 0;
        double oldY = verticalFriction > 0 ? oldVelocity.y() / verticalFriction : 0;

        double finalVertical = oldY + vertical * tps;
        finalVertical = Math.min(finalVertical, verticalLimit);

        double vx = oldX + direction.x() * horizontal * tps;
        double vz = oldZ + direction.z() * horizontal * tps;

        return new Vec(vx, finalVertical, vz);
    }
}

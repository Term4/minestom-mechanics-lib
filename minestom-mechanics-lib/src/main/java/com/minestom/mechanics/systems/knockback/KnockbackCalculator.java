package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.systems.misc.GameplayUtils;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;

import static com.minestom.mechanics.config.constants.CombatConstants.MIN_KNOCKBACK_DISTANCE;
import static com.minestom.mechanics.config.constants.MechanicsConstants.MIN_FALLING_KNOCKBACK;

// TODO: Potentially add another knockback calculator for modern knockback
//  (the formula is entirely different) would lead to this being called "LegacyKnockbackCalculator"
//  Actually I think the only change would need to be in

/**
 * Component responsible for calculating knockback strength and direction.
 * Extracted from monolithic KnockbackHandler for better maintainability.
 */
public class KnockbackCalculator {

    /** Result of direction calculation, including proximity-based strength multiplier. */
    public record KnockbackDirectionResult(Vec direction, double proximityMultiplier) {}

    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackCalculator");

    private final KnockbackConfig config;

    public KnockbackCalculator(KnockbackConfig config) {
        this.config = config;
    }
    
    /**
     * Calculate knockback direction and proximity multiplier based on the configured mode.
     * Position-based modes blend with attacker/shooter look via lookWeight; VICTIM_FACING blends
     * position (away-from-attacker) with victim's facing via lookWeight.
     *
     * @param lookWeight 0–1; for position modes blends with attacker/shooter look; for VICTIM_FACING blends with victim look
     * @param degenerateFallback behavior when victim and origin are very close
     * @param proximityScaleDistance distance over which PROXIMITY_SCALE ramps from 0 to full strength (blocks)
     */
    public KnockbackDirectionResult calculateDirection(KnockbackSystem.KnockbackDirectionMode mode, LivingEntity victim,
                                                       @org.jetbrains.annotations.Nullable Entity attacker,
                                                       @org.jetbrains.annotations.Nullable Entity source,
                                                       @org.jetbrains.annotations.Nullable Pos shooterOriginPos,
                                                       double lookWeight,
                                                       KnockbackSystem.DegenerateFallback degenerateFallback,
                                                       double proximityScaleDistance) {
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

        double distance = origin != null ? horizontalDistance(victim.getPosition(), origin) : Double.POSITIVE_INFINITY;
        boolean degenerate = distance < MIN_KNOCKBACK_DISTANCE;

        Vec base;
        double proximityMult = 1.0;

        if (mode == KnockbackSystem.KnockbackDirectionMode.VICTIM_FACING) {
            Vec victimLook = directionFromLook(victim.getPosition());
            Vec posDir = origin != null
                    ? directionFromPosRaw(victim, origin, victimLook)
                    : fallback(victimLook, degenerateFallback, victimLook);
            base = blendDirection(posDir, victimLook, lookWeight);
            proximityMult = degenerateFallback == KnockbackSystem.DegenerateFallback.PROXIMITY_SCALE
                    ? Math.min(1.0, distance / Math.max(proximityScaleDistance, 1e-9)) : 1.0;
            return new KnockbackDirectionResult(base, proximityMult);
        }

        if (degenerate) {
            Vec fallbackDir = fallback(lookDir, degenerateFallback, lookDir);
            switch (degenerateFallback) {
                case LOOK, RANDOM -> proximityMult = 1.0;
                case PROXIMITY_SCALE -> proximityMult = Math.min(1.0, distance / Math.max(proximityScaleDistance, 1e-9));
            }
            return new KnockbackDirectionResult(fallbackDir, proximityMult);
        }

        base = switch (mode) {
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

        if (degenerateFallback == KnockbackSystem.DegenerateFallback.PROXIMITY_SCALE) {
            proximityMult = Math.min(1.0, distance / Math.max(proximityScaleDistance, 1e-9));
        }

        if (lookWeight <= 0) return new KnockbackDirectionResult(base, proximityMult);
        Vec lookDirForBlend = lookDir != null ? lookDir : base;
        return new KnockbackDirectionResult(blendDirection(base, lookDirForBlend, lookWeight), proximityMult);
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
            case PROXIMITY_SCALE -> preferred != null ? preferred : randomDirection();
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
     * Calculate knockback direction from victim to attacker position.
     * @deprecated Use {@link #calculateDirection} with a mode instead.
     */
    @Deprecated
    public Vec calculateKnockbackDirection(LivingEntity victim, Entity attacker) {
        double dx = victim.getPosition().x() - attacker.getPosition().x();
        double dz = victim.getPosition().z() - attacker.getPosition().z();

        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < MIN_KNOCKBACK_DISTANCE) {
            dx = Math.random() * 0.02 - 0.01;
            dz = Math.random() * 0.02 - 0.01;
            distance = Math.sqrt(dx * dx + dz * dz);
        }

        dx /= distance;
        dz /= distance;

        // Apply look weight if configured
        double lookWeight = config.lookWeight();
        if (lookWeight > 0 && attacker instanceof Player) {
            double yaw = Math.toRadians(attacker.getPosition().yaw());
            double lookX = -Math.sin(yaw);
            double lookZ = Math.cos(yaw);

            dx = dx * (1 - lookWeight) + lookX * lookWeight;
            dz = dz * (1 - lookWeight) + lookZ * lookWeight;

            double finalDistance = Math.sqrt(dx * dx + dz * dz);
            if (finalDistance > MIN_KNOCKBACK_DISTANCE) {
                dx /= finalDistance;
                dz /= finalDistance;
            }
        }

        return new Vec(dx, 0, dz);
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
     * Calculate final velocity from knockback components.
     */
    public Vec calculateFinalVelocity(LivingEntity victim, Vec direction, KnockbackSystem.KnockbackStrength strength,
                                      KnockbackSystem.KnockbackType type) {
        double tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        double horizontal = strength.horizontal() / tps;
        double vertical = strength.vertical() / tps;

        // TODO: Come up with an actual USABLE method to get the old player velocity
        Vec oldVelocity = victim.getVelocity();


        // TODO: Is this necessary?
        // Handle ground state velocity correction
        if (victim.isOnGround()) {
            Vec minestomVel = victim.getVelocity();
            if (minestomVel.y() > 0.1) {
                oldVelocity = oldVelocity.withY(minestomVel.y());
            } else {
                oldVelocity = oldVelocity.withY(Math.max(oldVelocity.y(), 0));
            }
        }

        // Calculate final vertical component
        double finalVertical = calculateVerticalComponent(victim, oldVelocity, vertical, tps);

        // 1.8 legacy calculation
        return new Vec(
                oldVelocity.x() / 2.0 + direction.x() * horizontal * tps,
                finalVertical,
                oldVelocity.z() / 2.0 + direction.z() * horizontal * tps
        );
    }

    /**
     * Calculate final vertical component of knockback.
     */
    private double calculateVerticalComponent(LivingEntity victim, Vec oldVelocity,
                                              double vertical, double tps) {
        if (victim.isOnGround()) {
            return oldVelocity.y() / 2.0 + vertical * tps;
        }

        // TODO: Make sure oldVelocity.y() DOESN'T SUFFER from the minenstom y velocity bug
        // UPDATE: It does. add new velocity tracking, maybe using movement packets + get player location on the previous tick?
        boolean isFalling = GameplayUtils.isFalling(victim);

        if (isFalling) {
            return Math.max(vertical * tps, MIN_FALLING_KNOCKBACK);
        }
        // TODO: Investigate if this is needed at all. Seems arbitrary, if someone configures
        //  their knockback profile in a way that would result in vertical being < MIN_FALLING_KNOCKBACK,
        //  we should allow that, not override it. If they get upset that's kinda their fault
        return oldVelocity.y() / 2.0 + vertical * tps;
    }
}

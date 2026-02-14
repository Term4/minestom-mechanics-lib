package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.systems.blocking.BlockingSystem;
import com.minestom.mechanics.systems.misc.VelocityEstimator;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import static com.minestom.mechanics.config.constants.CombatConstants.MIN_KNOCKBACK_DISTANCE;

/**
 * Computes knockback velocity from context. Handles direction, strength (sprint/enchant/sweeping/blocking),
 * range reduction, air multipliers, resistance, friction, and final velocity.
 * Stateless — receives full context per call.
 */
public class KnockbackCalculator {

    /** Optional sink for debug info. When non-null, computeKnockbackVelocity fills it. */
    public static final class DebugSink {
        public KnockbackSystem.KnockbackDebugInfo info;
    }

    /** Compute knockback velocity (no debug). */
    public Vec computeKnockbackVelocity(KnockbackSystem.KnockbackContext ctx) {
        return computeKnockbackVelocity(ctx, null);
    }

    /**
     * Compute the knockback velocity for the given context.
     * When debugSink is non-null, fills it with old velocity, post velocity, pre-sprint vector, and vertical limit info.
     */
    public Vec computeKnockbackVelocity(KnockbackSystem.KnockbackContext ctx, @Nullable DebugSink debugSink) {
        Double rangeDistance = null;
        Double rangeReductionH = null;
        Double rangeReductionV = null;

        KnockbackConfig c = ctx.config();
        KnockbackSystem.KnockbackDirectionMode dirMode = (ctx.type() == KnockbackSystem.KnockbackType.PROJECTILE)
                ? c.projectileDirection() : c.meleeDirection();
        double lookWeight = c.lookWeight();
        double sprintLookWeight = c.sprintLookWeight() != null ? c.sprintLookWeight() : lookWeight;

        KnockbackSystem.DirectionBlendMode blendMode = c.directionBlendMode();
        Vec direction;
        double horizontal;
        double vertical;

        if (ctx.wasSprinting() && ctx.type() != KnockbackSystem.KnockbackType.PROJECTILE) {
            var baseResult = computeDirectionAndHorizontal(dirMode, ctx.victim(), ctx.attacker(), ctx.source(), ctx.shooterOriginPos(), lookWeight, c.horizontal(), c.degenerateFallback(), blendMode);
            var sprintResult = computeDirectionAndHorizontal(dirMode, ctx.victim(), ctx.attacker(), ctx.source(), ctx.shooterOriginPos(), sprintLookWeight, c.sprintBonusHorizontal(), c.degenerateFallback(), blendMode);
            double hVecX = baseResult.horizontal() * baseResult.direction().x() + sprintResult.horizontal() * sprintResult.direction().x();
            double hVecZ = baseResult.horizontal() * baseResult.direction().z() + sprintResult.horizontal() * sprintResult.direction().z();
            double len = Math.sqrt(hVecX * hVecX + hVecZ * hVecZ);
            if (len < MIN_KNOCKBACK_DISTANCE) {
                direction = baseResult.direction();
                horizontal = baseResult.horizontal() + sprintResult.horizontal();
            } else {
                direction = new Vec(hVecX / len, 0, hVecZ / len);
                horizontal = len;
            }
            vertical = c.vertical() + c.sprintBonusVertical();
        } else {
            var result = computeDirectionAndHorizontal(dirMode, ctx.victim(), ctx.attacker(), ctx.source(), ctx.shooterOriginPos(), lookWeight, c.horizontal(), c.degenerateFallback(), blendMode);
            direction = result.direction();
            horizontal = result.horizontal();
            vertical = c.vertical();
        }

        // Enchantment bonus (melee only)
        if (ctx.kbEnchantLevel() > 0 && ctx.type() != KnockbackSystem.KnockbackType.PROJECTILE) {
            horizontal += ctx.kbEnchantLevel() * 0.6;
            vertical += 0.1;
        }

        // Sweeping reduction
        if (ctx.type() == KnockbackSystem.KnockbackType.SWEEPING) {
            horizontal *= 0.5;
            vertical *= 0.5;
        }

        KnockbackSystem.KnockbackStrength strength = new KnockbackSystem.KnockbackStrength(horizontal, vertical);

        // Blocking reduction
        if (ctx.victim() instanceof Player player) {
            try {
                BlockingSystem blocking = BlockingSystem.getInstance();
                if (blocking.isBlocking(player)) {
                    horizontal *= (1.0 - blocking.getKnockbackHorizontalReduction(player));
                    vertical *= (1.0 - blocking.getKnockbackVerticalReduction(player));
                    strength = new KnockbackSystem.KnockbackStrength(horizontal, vertical);
                }
            } catch (IllegalStateException ignored) {}
        }

        // Range reduction (separate config for sprint vs non-sprint)
        Pos origin = resolveOrigin(ctx);
        KnockbackSystem.RangeReductionConfig rangeCfg = ctx.wasSprinting() ? c.sprintRangeReduction() : c.rangeReduction();
        if (origin != null && (rangeCfg.factorHorizontal() > 0 || rangeCfg.factorVertical() > 0)) {
            double distance = computeDistanceForRangeReduction(ctx.victim().getPosition(), origin);
            double excessH = rangeCfg.startDistanceHorizontal() > 0 ? Math.max(0, distance - rangeCfg.startDistanceHorizontal()) : 0;
            double excessV = rangeCfg.startDistanceVertical() > 0 ? Math.max(0, distance - rangeCfg.startDistanceVertical()) : 0;
            double rawReductionH = excessH * rangeCfg.factorHorizontal();
            double rawReductionV = excessV * rangeCfg.factorVertical();
            double appliedReductionH = Math.min(rawReductionH, rangeCfg.maxHorizontal());
            double appliedReductionV = Math.min(rawReductionV, rangeCfg.maxVertical());
            if (debugSink != null) {
                rangeDistance = distance;
                rangeReductionH = appliedReductionH;
                rangeReductionV = appliedReductionV;
            }
            horizontal = Math.max(0, horizontal - appliedReductionH);
            vertical = Math.max(0, vertical - appliedReductionV);
            strength = new KnockbackSystem.KnockbackStrength(horizontal, vertical);
        }

        // Air multipliers
        if (!ctx.victim().isOnGround()) {
            strength = new KnockbackSystem.KnockbackStrength(
                    strength.horizontal() * c.airMultiplierHorizontal(),
                    strength.vertical() * c.airMultiplierVertical()
            );
        }

        // Resistance attribute
        double resistance = ctx.victim().getAttributeValue(net.minestom.server.entity.attribute.Attribute.KNOCKBACK_RESISTANCE);
        strength = new KnockbackSystem.KnockbackStrength(
                strength.horizontal() * (1 - resistance),
                strength.vertical() * (1 - resistance)
        );

        // Resolve friction: use sprint values when sprinting (melee only), else base
        double hFric = c.horizontalFriction();
        double vFric = c.verticalFriction();
        if (ctx.wasSprinting() && ctx.type() != KnockbackSystem.KnockbackType.PROJECTILE) {
            if (c.sprintHorizontalFriction() != null) hFric = c.sprintHorizontalFriction();
            if (c.sprintVerticalFriction() != null) vFric = c.sprintVerticalFriction();
        }

        // Pre-sprint vector (for sprint hits: velocity with base strength only; else same as computed)
        Vec preSprintVector = null;
        if (debugSink != null && ctx.wasSprinting() && ctx.type() != KnockbackSystem.KnockbackType.PROJECTILE) {
            var baseStrength = new KnockbackSystem.KnockbackStrength(c.horizontal(), c.vertical());
            preSprintVector = calculateFinalVelocityWithDebug(ctx.victim(), direction, baseStrength,
                    hFric, vFric, c.verticalLimit(), null);
        }

        // Final velocity with friction
        double[] vertDebug = debugSink != null ? new double[2] : null;
        Vec computed = calculateFinalVelocityWithDebug(ctx.victim(), direction, strength,
                hFric, vFric, c.verticalLimit(), vertDebug);

        Vec oldVel = VelocityEstimator.getVelocity(ctx.victim());
        Vec finalResult = c.velocityApplyMode() == KnockbackSystem.VelocityApplyMode.ADD
                ? oldVel.add(computed) : computed;

        if (debugSink != null) {
            Double vPre = (vertDebug != null && vertDebug[0] > vertDebug[1]) ? vertDebug[0] : null;
            Double vLimit = (vertDebug != null && vertDebug[0] > vertDebug[1]) ? vertDebug[1] : null;
            debugSink.info = new KnockbackSystem.KnockbackDebugInfo(
                    oldVel,
                    finalResult,
                    preSprintVector != null ? preSprintVector : finalResult,
                    vPre,
                    vLimit,
                    rangeDistance,
                    rangeReductionH,
                    rangeReductionV
            );
        }
        return finalResult;
    }

    /** Result of direction + horizontal magnitude computation. */
    private record DirAndMag(Vec direction, double horizontal) {}

    private DirAndMag computeDirectionAndHorizontal(KnockbackSystem.KnockbackDirectionMode mode, LivingEntity victim,
                                                    @Nullable Entity attacker, @Nullable Entity source, @Nullable Pos shooterOriginPos,
                                                    double lookWeight, double horizontalMag,
                                                    KnockbackSystem.DegenerateFallback degenerateFallback,
                                                    KnockbackSystem.DirectionBlendMode blendMode) {
        if (blendMode == KnockbackSystem.DirectionBlendMode.ADD_VECTORS) {
            RawDirections raw = getRawPositionAndLookDirections(mode, victim, attacker, source, shooterOriginPos, degenerateFallback);
            if (raw == null) {
                Vec fallbackDir = calculateDirection(mode, victim, attacker, source, shooterOriginPos, lookWeight, degenerateFallback);
                return new DirAndMag(fallbackDir, horizontalMag);
            }
            double positionMag = horizontalMag * (1 - lookWeight);
            double lookMag = horizontalMag * lookWeight;
            double cx = raw.positionDir().x() * positionMag + raw.lookDir().x() * lookMag;
            double cz = raw.positionDir().z() * positionMag + raw.lookDir().z() * lookMag;
            double len = Math.sqrt(cx * cx + cz * cz);
            if (len < MIN_KNOCKBACK_DISTANCE) {
                return new DirAndMag(raw.lookDir(), horizontalMag);
            }
            return new DirAndMag(new Vec(cx / len, 0, cz / len), len);
        }
        Vec dir = calculateDirection(mode, victim, attacker, source, shooterOriginPos, lookWeight, degenerateFallback);
        return new DirAndMag(dir, horizontalMag);
    }

    private record RawDirections(Vec positionDir, Vec lookDir) {}

    @Nullable
    private RawDirections getRawPositionAndLookDirections(KnockbackSystem.KnockbackDirectionMode mode, LivingEntity victim,
                                                          @Nullable Entity attacker, @Nullable Entity source, @Nullable Pos shooterOriginPos,
                                                          KnockbackSystem.DegenerateFallback degenerateFallback) {
        Pos lookSource = shooterOriginPos != null ? shooterOriginPos : (attacker != null ? attacker.getPosition() : null);
        Vec lookDir = lookSource != null ? directionFromLook(lookSource) : null;

        Pos origin = switch (mode) {
            case ATTACKER_POSITION -> attacker != null ? attacker.getPosition() : null;
            case SHOOTER_ORIGIN -> shooterOriginPos;
            case PROJECTILE_POSITION -> source != null ? source.getPosition() : null;
            case VICTIM_FACING -> (attacker != null || source != null) ? (attacker != null ? attacker : source).getPosition() : null;
        };

        if (origin != null && horizontalDistance(victim.getPosition(), origin) < MIN_KNOCKBACK_DISTANCE) {
            return null;
        }
        if (mode == KnockbackSystem.KnockbackDirectionMode.VICTIM_FACING) {
            Vec victimLook = directionFromLook(victim.getPosition());
            Vec posDir = origin != null ? directionFromPosRaw(victim, origin, victimLook) : fallback(victimLook);
            return new RawDirections(posDir, victimLook);
        }
        Vec posDir = switch (mode) {
            case ATTACKER_POSITION -> attacker != null ? directionFromPosRaw(victim, attacker.getPosition(), lookDir) : fallback(lookDir);
            case SHOOTER_ORIGIN -> shooterOriginPos != null ? directionFromPosRaw(victim, shooterOriginPos, lookDir) : fallback(lookDir);
            case PROJECTILE_POSITION -> source != null ? directionFromPosRaw(victim, source.getPosition(), lookDir) : fallback(lookDir);
            case VICTIM_FACING -> throw new IllegalStateException("handled above");
        };
        Vec lookDirForBlend = lookDir != null ? lookDir : posDir;
        return new RawDirections(posDir, lookDirForBlend);
    }

    private static Pos resolveOrigin(KnockbackSystem.KnockbackContext ctx) {
        if (ctx.type() == KnockbackSystem.KnockbackType.PROJECTILE && ctx.source() != null)
            return ctx.source().getPosition();
        if (ctx.type() == KnockbackSystem.KnockbackType.PROJECTILE && ctx.shooterOriginPos() != null)
            return ctx.shooterOriginPos();
        return ctx.attacker() != null ? ctx.attacker().getPosition() : null;
    }

    public Vec calculateDirection(KnockbackSystem.KnockbackDirectionMode mode, LivingEntity victim,
                                  @Nullable Entity attacker, @Nullable Entity source, @Nullable Pos shooterOriginPos,
                                  double lookWeight, KnockbackSystem.DegenerateFallback degenerateFallback) {
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
            Vec posDir = origin != null ? directionFromPosRaw(victim, origin, victimLook) : fallback(victimLook, degenerateFallback, victimLook);
            return blendDirection(posDir, victimLook, lookWeight);
        }

        if (degenerate) return fallback(lookDir, degenerateFallback, lookDir);

        Vec base = switch (mode) {
            case ATTACKER_POSITION -> attacker != null ? directionFromPosRaw(victim, attacker.getPosition(), lookDir) : fallback(lookDir, degenerateFallback, lookDir);
            case SHOOTER_ORIGIN -> shooterOriginPos != null ? directionFromPosRaw(victim, shooterOriginPos, lookDir) : fallback(lookDir, degenerateFallback, lookDir);
            case PROJECTILE_POSITION -> source != null ? directionFromPosRaw(victim, source.getPosition(), lookDir) : fallback(lookDir, degenerateFallback, lookDir);
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

    /**
     * Distance for range reduction. Default: XZ only. Future: 3D, pitch, Y.
     */
    public double computeDistanceForRangeReduction(Pos victim, Pos origin) {
        return horizontalDistance(victim, origin);
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

    private Vec directionFromPosRaw(LivingEntity victim, Pos origin, @Nullable Vec fallbackIfDegenerate) {
        double dx = victim.getPosition().x() - origin.x();
        double dz = victim.getPosition().z() - origin.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < MIN_KNOCKBACK_DISTANCE) return fallback(fallbackIfDegenerate);
        return new Vec(dx / distance, 0, dz / distance);
    }

    private Vec directionFromLook(Pos pos) {
        double yaw = Math.toRadians(pos.yaw());
        return new Vec(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    private Vec randomDirection() {
        double dx = Math.random() * 0.02 - 0.01;
        double dz = Math.random() * 0.02 - 0.01;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return dist > 0 ? new Vec(dx / dist, 0, dz / dist) : new Vec(1, 0, 0);
    }

    private Vec calculateFinalVelocityWithDebug(LivingEntity victim, Vec direction, KnockbackSystem.KnockbackStrength strength,
                                                double horizontalFriction, double verticalFriction, double verticalLimit,
                                                @Nullable double[] vertDebug) {
        double tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        double horizontal = strength.horizontal() / tps;
        double vertical = strength.vertical() / tps;

        Vec oldVelocity = VelocityEstimator.getVelocity(victim);
        double oldX = horizontalFriction > 0 ? oldVelocity.x() / horizontalFriction : 0;
        double oldZ = horizontalFriction > 0 ? oldVelocity.z() / horizontalFriction : 0;
        double oldY = verticalFriction > 0 ? oldVelocity.y() / verticalFriction : 0;

        double preLimit = oldY + vertical * tps;
        double finalVertical = Math.min(preLimit, verticalLimit);
        if (vertDebug != null && preLimit > verticalLimit) {
            vertDebug[0] = preLimit;
            vertDebug[1] = verticalLimit;
        }

        double vx = oldX + direction.x() * horizontal * tps;
        double vz = oldZ + direction.z() * horizontal * tps;

        return new Vec(vx, finalVertical, vz);
    }
}

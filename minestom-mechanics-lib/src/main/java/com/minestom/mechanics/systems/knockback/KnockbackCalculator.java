package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.systems.knockback.components.KnockbackStrength;
import com.minestom.mechanics.systems.knockback.components.KnockbackType;
import com.minestom.mechanics.util.GameplayUtils;
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
    
    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackCalculator");

    private final KnockbackConfig config;

    public KnockbackCalculator(KnockbackConfig config) {
        this.config = config;
    }
    
    /**
     * Calculate knockback direction from victim to attacker position.
     * ✅ REFACTORED: Using MIN_KNOCKBACK_DISTANCE constant
     */
    public Vec calculateKnockbackDirection(LivingEntity victim, Entity attacker) {
        double dx = victim.getPosition().x() - attacker.getPosition().x();
        double dz = victim.getPosition().z() - attacker.getPosition().z();

        double distance = Math.sqrt(dx * dx + dz * dz);

        // ✅ REFACTORED: Using constant instead of magic number
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
    public Vec calculateFinalVelocity(LivingEntity victim, Vec direction, KnockbackStrength strength,
                                      KnockbackType type) {
        double tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        double horizontal = strength.horizontal() / tps;
        double vertical = strength.vertical() / tps;

        Vec oldVelocity = victim.getVelocity();

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

        // TODO: Make sure oldVelocity.y() DOESN'T SUFFER from the minenstom y velocity bug (where it has a ~ -1.518 offset)
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

package com.minestom.mechanics.features.knockback.components;

import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.features.knockback.KnockbackProfile;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;

import static com.minestom.mechanics.config.combat.CombatConstants.*;

/**
 * Component responsible for calculating knockback strength and direction.
 * Extracted from monolithic KnockbackHandler for better maintainability.
 */
public class KnockbackCalculator {
    
    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackCalculator");
    
    private final KnockbackProfile profile;
    
    public KnockbackCalculator(KnockbackProfile profile) {
        this.profile = profile;
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
        double lookWeight = profile.getLookWeight();
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
     * Calculate knockback strength based on type and sprint state.
     */
    public KnockbackStrength calculateKnockbackStrength(LivingEntity victim,
                                                       Entity attacker,
                                                       KnockbackHandler.KnockbackType type,
                                                       boolean wasSprinting) {
        KnockbackHandler.KnockbackSettings settings = profile.getSettings();
        double horizontal = settings.horizontal();
        double vertical = settings.vertical();

        // ✅ USE CAPTURED SPRINT STATE instead of checking current state
        if (attacker instanceof Player player &&
                wasSprinting &&  // ← Use passed-in value, not player.isSprinting()!
                (type == KnockbackHandler.KnockbackType.ATTACK || type == KnockbackHandler.KnockbackType.DAMAGE)) {

            horizontal += settings.extraHorizontal();
            vertical += settings.extraVertical();
            player.setSprinting(false);
            log.debug("Sprint bonus applied for: " + player.getUsername());
        }

        // Apply sweeping reduction
        if (type == KnockbackHandler.KnockbackType.SWEEPING) {
            horizontal *= 0.5;
            vertical *= 0.5;
        }

        return new KnockbackStrength(horizontal, vertical);
    }
    
    /**
     * Apply modifiers to knockback strength.
     * ✅ REFACTORED: Using FALLING_VELOCITY_THRESHOLD and MIN_FALLING_KNOCKBACK constants
     */
    public KnockbackStrength applyModifiers(LivingEntity victim, KnockbackStrength base) {
        double horizontal = base.horizontal;
        double vertical = base.vertical;

        Vec velocity = victim.getVelocity();
        boolean isInAir = !victim.isOnGround();

        // ✅ REFACTORED: Using constant for falling detection
        boolean isFalling = isInAir && velocity.y() < FALLING_VELOCITY_THRESHOLD;

        // Apply air multipliers
        if (isInAir) {
            horizontal *= profile.getAirHorizontalMultiplier();
            vertical *= profile.getAirVerticalMultiplier();
        }

        // ✅ REFACTORED: Using constant for 1.8 no-falling knockback
        if (isFalling) {
            vertical = Math.max(Math.abs(vertical), MIN_FALLING_KNOCKBACK);
        }

        // Apply knockback resistance
        double kbResistance = victim.getAttributeValue(Attribute.KNOCKBACK_RESISTANCE);
        horizontal *= (1 - kbResistance);
        vertical *= (1 - kbResistance);

        return new KnockbackStrength(horizontal, vertical);
    }
    
    /**
     * Calculate final velocity from knockback components.
     */
    public Vec calculateFinalVelocity(LivingEntity victim, Vec direction, KnockbackStrength strength,
                                     KnockbackHandler.KnockbackType type) {
        double tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        double horizontal = strength.horizontal / tps;
        double vertical = strength.vertical / tps;

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
     * ✅ REFACTORED: Using MIN_FALLING_KNOCKBACK constant
     */
    private double calculateVerticalComponent(LivingEntity victim, Vec oldVelocity,
                                           double vertical, double tps) {
        boolean isInAir = !victim.isOnGround();
        boolean isFalling = isInAir && oldVelocity.y() <= 0.0;

        if (victim.isOnGround()) {
            return oldVelocity.y() / 2.0 + vertical * tps;
        }

        // ✅ REFACTORED: Using constant for no-falling KB
        if (isFalling) {
            return Math.max(vertical * tps, MIN_FALLING_KNOCKBACK);
        }

        return oldVelocity.y() / 2.0 + vertical * tps;
    }
    
    /**
     * Knockback strength data class.
     */
    public static class KnockbackStrength {
        public final double horizontal;
        public final double vertical;

        public KnockbackStrength(double horizontal, double vertical) {
            this.horizontal = horizontal;
            this.vertical = vertical;
        }
    }
}

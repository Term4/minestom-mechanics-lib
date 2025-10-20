package com.minestom.mechanics.validation.components;

import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.systems.gameplay.EyeHeightSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;

import static com.minestom.mechanics.constants.CombatConstants.PLAYER_HEIGHT;

// TODO: This is pretty solid actually,
//  may change with potential update to how we handle
//  disabling modern sneaking though. Right now
//  it's FUNCTIONAL, but the way we handle modern sneaking isn't
//  the smoothest. Would be ideal to somehow force a sync
//  for eye height, client + server side. Dig in minestom javadocs.

/**
 * Validates attack packets from clients.
 * Handles reach validation and angle checking for client-initiated attacks.
 */
public class AttackPacketValidator {
    private static final LogUtil.SystemLogger log = LogUtil.system("AttackPacketValidator");
    
    private final HitDetectionConfig hitDetectionConfig;

    public AttackPacketValidator(HitDetectionConfig hitDetectionConfig) {
        this.hitDetectionConfig = hitDetectionConfig;
    }

    /**
     * Validates reach for client-initiated attacks (attack packets).
     * Uses benefit of the doubt with LIMIT expansion for lag compensation:
     * 1. Check basic reach first (quick rejection of obvious cheats)
     * 2. Apply LIMIT expansion for benefit of the doubt
     * 3. Log precise data for analytics
     *
     * @param attacker Player attacking
     * @param victim Entity being attacked
     * @return true if hit is valid, false if impossibly far
     */
    public boolean isReachValid(Player attacker, LivingEntity victim) {
        // CAPTURE positions at this exact moment
        Pos attackerEye = EyeHeightSystem.getInstance().getEyePosition(attacker);
        Pos victimPos = victim.getPosition();

        // Quick horizontal distance check first
        double dx = victimPos.x() - attackerEye.x();
        double dz = victimPos.z() - attackerEye.z();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        double maxReach = hitDetectionConfig.attackPacketReach();
        double limitExpansion = hitDetectionConfig.hitboxExpansionLimit();
        
        // Quick check: if horizontal distance alone exceeds max reach + expansion, reject immediately
        if (horizontalDist > maxReach + limitExpansion) {
            logRejection(attacker, victim, attackerEye, horizontalDist, maxReach);
            return false;
        }

        // Detailed validation with benefit of the doubt
        // Check if the attack would hit with LIMIT expansion (for lag compensation)
        double victimCenterY = victimPos.y() + (PLAYER_HEIGHT / 2.0);
        Pos victimCenter = victimPos.withY(victimCenterY);
        
        // Calculate distance from eye to edge of expanded hitbox
        // This gives benefit of the doubt by considering the closest point on the expanded hitbox
        double effectiveDistance = calculateEffectiveDistance(attackerEye, victimCenter, limitExpansion);
        
        if (effectiveDistance > maxReach) {
            // Too far even with benefit of the doubt - reject
            logRejection(attacker, victim, attackerEye, effectiveDistance, maxReach);
            return false;
        }

        return true; // Valid hit
    }
    
    /**
     * Calculate effective distance considering hitbox expansion.
     * This gives the distance to the edge of the expanded hitbox.
     */
    private double calculateEffectiveDistance(Pos attackerEye, Pos victimCenter, double expansion) {
        double distance = attackerEye.distance(victimCenter);
        // Subtract expansion to get distance to edge of expanded hitbox
        return Math.max(0, distance - expansion);
    }

    /**
     * Logs attack packet rejection.
     */
    private void logRejection(Player attacker, LivingEntity victim,
                              Pos eyePos, double distance, double maxReach) {
        Pos victimPos = victim.getPosition();

        double dx = victimPos.x() - eyePos.x();
        double dy = victimPos.y() - eyePos.y();
        double dz = victimPos.z() - eyePos.z();

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double verticalDist = Math.abs(dy);

        log.warn("{} attack REJECTED | 3D={:.2f}b > max={:.2f}b | " +
                        "horizontal={:.2f}b vertical={:.2f}b",
                attacker.getUsername(), distance, maxReach,
                horizontalDist, verticalDist);
    }
}

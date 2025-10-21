package com.minestom.mechanics.validation.components;

import com.minestom.mechanics.hitbox.HitboxExpansion;
import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.geometry.RaycastUtils;
import com.minestom.mechanics.systems.gameplay.EyeHeightSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.geometry.RaycastUtils.RayHitResult;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;

import static com.minestom.mechanics.config.constants.CombatConstants.PLAYER_HEIGHT;

// TODO: This is pretty solid. Again,
//  could change with the eye height thing mentioned
//  in AttackPacketValidator.

/**
 * Consolidated server-side hit detection with distance calculation capabilities.
 * ✅ CONSOLIDATED: Merged RayDistanceCalculator into ServerSideDetector for better cohesion.
 */
public class ServerSideDetector {
    private static final LogUtil.SystemLogger log = LogUtil.system("ServerSideDetector");
    
    private final HitDetectionConfig hitDetectionConfig;
    private final HitboxExpansion hitboxExpansion;
    private final RaycastUtils raycastUtils;

    public ServerSideDetector(HitDetectionConfig hitDetectionConfig) {
        this.hitDetectionConfig = hitDetectionConfig;
        this.hitboxExpansion = new HitboxExpansion(hitDetectionConfig);
        this.raycastUtils = RaycastUtils.create(); // Use ServerConfig
    }

    /**
     * Performs server-side raycasting to find entity the player is aiming at.
     * Uses precise PRIMARY hitbox (server has authoritative positions).
     *
     * @param attacker Player swinging
     * @return Target entity, or null if none found
     */
    public LivingEntity findTargetFromSwing(Player attacker) {
        Pos eyePos = EyeHeightSystem.getInstance().getEyePosition(attacker);
        Vec direction = eyePos.direction();
        Vec normalizedDir = direction.normalize();

        // Filter out block interactions
        if (raycastUtils.isLookingAtBlock(attacker, eyePos, normalizedDir)) {
            return null;
        }

        return findClosestEntityTarget(attacker, eyePos, normalizedDir);
    }

    /**
     * Find the closest entity target within reach.
     */
    private LivingEntity findClosestEntityTarget(Player attacker, Pos eyePos, Vec direction) {
        double maxReach = hitDetectionConfig.serverSideReach();
        LivingEntity closestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : attacker.getInstance().getEntities()) {
            if (!(entity instanceof LivingEntity livingEntity) || entity == attacker) {
                continue;
            }

            // Use PRIMARY hitbox for server-side detection (most accurate)
            RayHitResult result = RaycastUtils.raycastToHitbox(
                    eyePos, direction, livingEntity.getPosition(),
                    hitboxExpansion.getPrimary(), maxReach
            );

            if (result != null) {
                double distance = result.getDistance();
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTarget = livingEntity;
                }
            }
        }

        if (closestTarget != null) {
            log.debug("Server-side target found: {} at distance {:.2f}",
                    closestTarget.getEntityType(), closestDistance);
        }

        return closestTarget;
    }

    // ===========================
    // DISTANCE CALCULATION (from RayDistanceCalculator)
    // ===========================

    /**
     * Calculate precise ray distance using captured positions.
     * This is separate from validation to avoid performance impact.
     */
    public HitSnapshot calculatePreciseDistance(Pos eyePos, Vec direction,
                                               Pos victimPos, double maxReach) {
        // Use the player's actual look direction for ray casting
        // This ensures different look angles produce different distances
        Vec rayDirection = direction.normalize();

        // Try PRIMARY raycast
        RayHitResult primaryResult = RaycastUtils.raycastToHitbox(
                eyePos, rayDirection, victimPos,
                hitboxExpansion.getPrimary(), maxReach
        );

        if (primaryResult != null) {
            // Use the actual hit point for accurate distance calculation
            Vec hitPoint = primaryResult.getHitPoint();
            double actualDistance = eyePos.distance(hitPoint);
            
            return new HitSnapshot(
                    actualDistance, // Use actual distance to hit point, not ray distance
                    ValidationTier.PRIMARY,
                    eyePos,
                    victimPos
            );
        }

        // Try LIMIT raycast
        RayHitResult limitResult = RaycastUtils.raycastToHitbox(
                eyePos, rayDirection, victimPos,
                hitboxExpansion.getLimit(), maxReach
        );

        if (limitResult != null) {
            // Use the actual hit point for accurate distance calculation
            Vec hitPoint = limitResult.getHitPoint();
            double actualDistance = eyePos.distance(hitPoint);
            
            return new HitSnapshot(
                    actualDistance, // Use actual distance to hit point, not ray distance
                    ValidationTier.LIMIT,
                    eyePos,
                    victimPos
            );
        }

        // Ray missed, use precise 3D distance (this should be rare now)
        double victimCenterY = victimPos.y() + (PLAYER_HEIGHT / 2.0);
        Pos victimCenter = victimPos.withY(victimCenterY);
        double preciseDistance = eyePos.distance(victimCenter);

        return new HitSnapshot(
                preciseDistance,
                ValidationTier.FALLBACK,
                eyePos,
                victimPos
        );
    }

    // ===========================
    // RESULT CLASSES
    // ===========================

    /**
     * Snapshot of a hit at a specific moment in time.
     * Captures all data needed for precise logging and analysis.
     */
    public static class HitSnapshot {
        public final double rayDistance;
        public final ValidationTier tier;
        public final Pos attackerEye;
        public final Pos victimPos;
        public final long timestamp;

        public HitSnapshot(double rayDistance, ValidationTier tier,
                           Pos attackerEye, Pos victimPos) {
            this.rayDistance = rayDistance;
            this.tier = tier;
            this.attackerEye = attackerEye;
            this.victimPos = victimPos;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Validation tier used for a hit.
     * Tracks confidence level for competitive analysis.
     */
    public enum ValidationTier {
        PRIMARY,   // High confidence: Ray hit with 0.1 expansion
        LIMIT,     // Medium confidence: Ray hit with 0.3 expansion
        FALLBACK   // Low confidence: Ray missed, used approximate distance
    }
}
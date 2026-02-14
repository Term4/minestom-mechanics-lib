package com.minestom.mechanics.systems.validation.hits;

import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import com.minestom.mechanics.systems.compatibility.hitbox.HitboxExpansion;
import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.systems.validation.RaycastUtils;
import com.minestom.mechanics.systems.compatibility.hitbox.EyeHeightSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.systems.validation.RaycastUtils.RayHitResult;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

import static com.minestom.mechanics.config.constants.CombatConstants.PLAYER_HEIGHT;

// TODO: This is pretty solid. Again,
//  could change with the eye height thing mentioned
//  in AttackPacketValidator.

/**
 * Consolidated server-side hit detection with distance calculation capabilities.
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
     * Uses primary expansion for all clients (server is modern; legacy benefits from same hitbox).
     *
     * @param attacker Player swinging
     * @return Target entity, or null if none found
     */
    public LivingEntity findTargetFromSwing(Player attacker) {
        return findTargetFromSwing(attacker, true);
    }

    /**
     * Performs server-side raycasting to find entity the player is aiming at.
     *
     * @param filterBlocks When true, returns null if looking at a block (avoids entity hit when mining). When false, skips this check (for combat swing window where crosshair-over-ground can reject valid hits).
     */
    public LivingEntity findTargetFromSwing(Player attacker, boolean filterBlocks) {
        Pos eyePos = EyeHeightSystem.getInstance().getEyePosition(attacker);
        Vec direction = eyePos.direction();
        Vec normalizedDir = direction.normalize();

        if (filterBlocks && raycastUtils.isLookingAtBlock(attacker, eyePos, normalizedDir)) {
            return null;
        }

        Vec expansion = hitboxExpansion.getPrimary();
        return findClosestEntityTarget(attacker, eyePos, normalizedDir, expansion);
    }

    /**
     * Find the closest entity target within reach.
     * Only returns entities that are not obstructed by a solid block between eye and hit point.
     */
    private LivingEntity findClosestEntityTarget(Player attacker, Pos eyePos, Vec direction, Vec expansion) {
        Instance instance = attacker.getInstance();
        if (instance == null) {
            return null;
        }

        double maxReach = hitDetectionConfig.serverSideReach();
        LivingEntity closestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : instance.getEntities()) {
            if (!(entity instanceof LivingEntity livingEntity) || entity == attacker) {
                continue;
            }

            RayHitResult result = RaycastUtils.raycastToHitbox(
                    eyePos, direction, livingEntity.getPosition(),
                    expansion, maxReach
            );

            if (result != null) {
                double distance = result.getDistance();
                // Obstruction: reject hit if any solid block is between eye and hit point (voxel traversal so corners are not missed)
                double obstructionCheckDistance = Math.max(1e-6, distance - 0.01);
                if (raycastUtils.findFirstSolidBlockAlongRayVoxel(instance, eyePos, direction, obstructionCheckDistance) != null) {
                    continue; // Block in the way, skip this entity
                }
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
     * Modern clients: use primary/limit expansion. Legacy: exact hitbox only.
     */
    public HitSnapshot calculatePreciseDistance(Player attacker, Pos eyePos, Vec direction,
                                                Pos victimPos, double maxReach) {
        Vec rayDirection = direction.normalize();
        Vec primary = isModernAttacker(attacker) ? hitboxExpansion.getPrimary() : Vec.ZERO;
        Vec limit = isModernAttacker(attacker) ? hitboxExpansion.getLimit() : Vec.ZERO;

        RayHitResult primaryResult = RaycastUtils.raycastToHitbox(
                eyePos, rayDirection, victimPos, primary, maxReach
        );

        if (primaryResult != null) {
            Vec hitPoint = primaryResult.getHitPoint();
            double actualDistance = eyePos.distance(hitPoint);
            return new HitSnapshot(
                    actualDistance, ValidationTier.PRIMARY, eyePos, victimPos
            );
        }

        RayHitResult limitResult = RaycastUtils.raycastToHitbox(
                eyePos, rayDirection, victimPos, limit, maxReach
        );

        if (limitResult != null) {
            Vec hitPoint = limitResult.getHitPoint();
            double actualDistance = eyePos.distance(hitPoint);
            return new HitSnapshot(
                    actualDistance, ValidationTier.LIMIT, eyePos, victimPos
            );
        }

        double victimCenterY = victimPos.y() + (PLAYER_HEIGHT / 2.0);
        Pos victimCenter = victimPos.withY(victimCenterY);
        double preciseDistance = eyePos.distance(victimCenter);
        return new HitSnapshot(
                preciseDistance, ValidationTier.FALLBACK, eyePos, victimPos
        );
    }

    private static boolean isModernAttacker(Player attacker) {
        return ClientVersionDetector.getInstance().getClientVersion(attacker)
                == ClientVersionDetector.ClientVersion.MODERN;
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
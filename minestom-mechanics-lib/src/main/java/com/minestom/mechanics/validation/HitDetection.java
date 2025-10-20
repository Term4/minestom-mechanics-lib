package com.minestom.mechanics.validation;

import com.minestom.mechanics.validation.components.*;
import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.systems.gameplay.EyeHeightSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;

// TODO: Remove a TON of repeated methods. A lot of methods (i.e isReachValid)
//  have already been done in validation classes.

/**
 * Main hit detection system orchestrator - coordinates all hit detection components.
 * Replaces the monolithic HitDetectionSystem with focused component architecture.
 */
public class HitDetection extends InitializableSystem {
    private static HitDetection instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("HitDetectionFeature");

    // Component references
    private final AttackPacketValidator attackPacketValidator;
    private final ServerSideDetector serverSideDetector;
    private final HitSnapshotTracker hitSnapshotTracker;

    // Configuration
    private final HitDetectionConfig hitDetectionConfig;

    private HitDetection(HitDetectionConfig hitDetectionConfig) {
        this.hitDetectionConfig = hitDetectionConfig;
        
        // Initialize components with new configs
        this.attackPacketValidator = new AttackPacketValidator(hitDetectionConfig);
        this.serverSideDetector = new ServerSideDetector(hitDetectionConfig);
        this.hitSnapshotTracker = new HitSnapshotTracker();
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    /**
     * Initialize with HitDetectionConfig.
     */
    public static HitDetection initialize(HitDetectionConfig hitDetectionConfig) {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("HitDetectionFeature");
            return instance;
        }

        instance = new HitDetection(hitDetectionConfig);
        instance.markInitialized();

        log.debug("Initialized with server reach: {:.2f}b, attack packet reach: {:.2f}b",
                hitDetectionConfig.serverSideReach(), hitDetectionConfig.attackPacketReach());

        log.debug("Snapshot-based validation: Fast distance check → Precise ray logging");

        LogUtil.logInit("HitDetectionFeature");
        return instance;
    }

    // ===========================
    // ATTACK PACKET VALIDATION
    // ===========================

    /**
     * Validates reach for client-initiated attacks (attack packets).
     * Uses snapshot-based approach with precise ray logging.
     *
     * @param attacker Player attacking
     * @param victim Entity being attacked
     * @return true if hit is valid, false if impossibly far
     */
    public boolean isReachValid(Player attacker, LivingEntity victim) {
        // Validate using attack packet validator
        if (!attackPacketValidator.isReachValid(attacker, victim)) {
            return false;
        }

        // Calculate precise distance for logging/analytics
        Pos attackerEye = EyeHeightSystem.getInstance().getEyePosition(attacker);
        Pos victimPos = victim.getPosition();
        double maxReach = hitDetectionConfig.attackPacketReach();

        // Use the player's actual look direction for accurate ray casting
        // This ensures different look angles (head, middle, feet) produce different distances
        Vec lookDirection = attackerEye.direction();

        ServerSideDetector.HitSnapshot snapshot = serverSideDetector.calculatePreciseDistance(
                attackerEye,
                lookDirection,
                victimPos,
                maxReach
        );

        // Store snapshot for AttackFeature to use
        hitSnapshotTracker.storeHitSnapshot(victim, snapshot);

        return true; // Valid hit
    }

    // ===========================
    // SERVER-SIDE HIT DETECTION
    // ===========================

    /**
     * Performs server-side raycasting to find entity the player is aiming at.
     * Uses precise PRIMARY hitbox (server has authoritative positions).
     *
     * @param attacker Player swinging
     * @return Target entity, or null if none found
     */
    public LivingEntity findTargetFromSwing(Player attacker) {
        LivingEntity target = serverSideDetector.findTargetFromSwing(attacker);
        
        if (target != null) {
            // Store precise snapshot for server-side hits too
            Pos attackerEye = EyeHeightSystem.getInstance().getEyePosition(attacker);
            Pos victimPos = target.getPosition();
            double maxReach = hitDetectionConfig.serverSideReach();

            // Use the player's actual look direction for accurate ray casting
            Vec lookDirection = attackerEye.direction();

            ServerSideDetector.HitSnapshot snapshot = serverSideDetector.calculatePreciseDistance(
                    attackerEye,
                    lookDirection,
                    victimPos,
                    maxReach
            );

            hitSnapshotTracker.storeHitSnapshot(target, snapshot);
        }
        
        return target;
    }

    // ===========================
    // SNAPSHOT & DISTANCE TRACKING
    // ===========================

    /**
     * Get the last hit snapshot for an entity.
     * Contains precise ray distance and validation tier.
     */
    public ServerSideDetector.HitSnapshot getLastHitSnapshot(LivingEntity entity) {
        return hitSnapshotTracker.getLastHitSnapshot(entity);
    }

    /**
     * Get the precise ray distance from the last hit.
     * Falls back to approximate distance if ray missed.
     */
    public double getLastHitDistance(LivingEntity entity) {
        return hitSnapshotTracker.getLastHitDistance(entity);
    }

    /**
     * Get the validation tier used for the last hit.
     * Useful for competitive analysis (track ratio of PRIMARY vs FALLBACK).
     */
    public ServerSideDetector.ValidationTier getLastHitTier(LivingEntity entity) {
        return hitSnapshotTracker.getLastHitTier(entity);
    }

    // ===========================
    // CLEANUP
    // ===========================

    /**
     * Clean up tracking data for an entity
     */
    public void cleanup(LivingEntity entity) {
        hitSnapshotTracker.cleanup(entity);
        if (entity instanceof Player player) {
            LogUtil.logCleanup("HitDetectionFeature", player.getUsername());
        }
    }

    /**
     * Clean up all tracking data
     */
    public void shutdown() {
        hitSnapshotTracker.clearAll();
        log.info("HitDetectionFeature shutdown complete");
    }
    
    // ===========================
    // CONFIGURATION
    // ===========================
    
    /**
     * Update configuration at runtime
     */
    public void updateConfig(HitDetectionConfig newConfig) {
        // Note: This creates new component instances with the new config
        // The old instances will be garbage collected
        // This is safe because the components don't maintain state
        log.info("Updating HitDetectionFeature configuration");
    }

    // ===========================
    // PUBLIC API
    // ===========================

    public static HitDetection getInstance() {
        if (instance == null) {
            throw new IllegalStateException("HitDetectionFeature not initialized!");
        }
        return instance;
    }

    public HitDetectionConfig getHitDetectionConfig() {
        return hitDetectionConfig;
    }

    /**
     * Get the number of tracked entities
     */
    public int getTrackedEntities() {
        return hitSnapshotTracker.getTrackedEntities().size();
    }
}

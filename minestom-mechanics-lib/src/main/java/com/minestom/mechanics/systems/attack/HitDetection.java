package com.minestom.mechanics.systems.attack;

import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.systems.compatibility.hitbox.EyeHeightSystem;
import com.minestom.mechanics.systems.validation.hits.AttackPacketValidator;
import com.minestom.mechanics.systems.validation.hits.HitSnapshotTracker;
import com.minestom.mechanics.systems.validation.hits.ServerSideDetector;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;

/**
 * Hit detection orchestrator. Determines if and what a player hit.
 * Uses validation components from {@code systems.validation.hits} for reach checks
 * and raycasting, but owns the detection decision.
 *
 * <p>Snapshot/distance tracking delegated to {@link HitSnapshotTracker} for analytics.</p>
 */
public class HitDetection extends InitializableSystem {
    private static HitDetection instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("HitDetection");

    private final AttackPacketValidator attackPacketValidator;
    private final ServerSideDetector serverSideDetector;
    private final HitSnapshotTracker hitSnapshotTracker;
    private final HitDetectionConfig config;

    private HitDetection(HitDetectionConfig config) {
        this.config = config;
        this.attackPacketValidator = new AttackPacketValidator(config);
        this.serverSideDetector = new ServerSideDetector(config);
        this.hitSnapshotTracker = new HitSnapshotTracker();
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static HitDetection initialize(HitDetectionConfig config) {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("HitDetection");
            return instance;
        }
        instance = new HitDetection(config);
        instance.markInitialized();
        log.debug("Initialized with server reach: {:.2f}b, attack packet reach: {:.2f}b",
                config.serverSideReach(), config.attackPacketReach());
        LogUtil.logInit("HitDetection");
        return instance;
    }

    // ===========================
    // CORE DETECTION
    // ===========================

    /**
     * Validate reach for a client-initiated attack (attack packet).
     * Stores a hit snapshot for analytics.
     */
    public boolean isReachValid(Player attacker, LivingEntity victim) {
        if (!attackPacketValidator.isReachValid(attacker, victim)) return false;

        // Calculate precise distance for analytics
        Pos eye = EyeHeightSystem.getInstance().getEyePosition(attacker);
        ServerSideDetector.HitSnapshot snapshot = serverSideDetector.calculatePreciseDistance(
                attacker, eye, eye.direction(), victim.getPosition(), config.attackPacketReach()
        );
        hitSnapshotTracker.storeHitSnapshot(victim, snapshot);
        return true;
    }

    /**
     * Check if attacker's look ray hits victim. Used for swing-window detection.
     * Skips block filter so crosshair-over-ground doesn't reject valid combat hits.
     */
    public boolean isLookHittingVictimInSwingWindow(Player attacker, LivingEntity victim) {
        LivingEntity target = serverSideDetector.findTargetFromSwing(attacker, false);
        return target == victim;
    }

    /**
     * Server-side raycast to find what entity the player is aiming at.
     * Used for modern clients with hitbox expansion.
     */
    public LivingEntity findTargetFromSwing(Player attacker) {
        LivingEntity target = serverSideDetector.findTargetFromSwing(attacker);
        if (target != null) {
            Pos eye = EyeHeightSystem.getInstance().getEyePosition(attacker);
            ServerSideDetector.HitSnapshot snapshot = serverSideDetector.calculatePreciseDistance(
                    attacker, eye, eye.direction(), target.getPosition(), config.serverSideReach()
            );
            hitSnapshotTracker.storeHitSnapshot(target, snapshot);
        }
        return target;
    }

    // ===========================
    // ANALYTICS (delegated to tracker)
    // ===========================

    public ServerSideDetector.HitSnapshot getLastHitSnapshot(LivingEntity entity) {
        return hitSnapshotTracker.getLastHitSnapshot(entity);
    }

    public double getLastHitDistance(LivingEntity entity) {
        return hitSnapshotTracker.getLastHitDistance(entity);
    }

    public ServerSideDetector.ValidationTier getLastHitTier(LivingEntity entity) {
        return hitSnapshotTracker.getLastHitTier(entity);
    }

    // ===========================
    // LIFECYCLE
    // ===========================

    public void cleanup(LivingEntity entity) {
        hitSnapshotTracker.cleanup(entity);
    }

    public void shutdown() {
        hitSnapshotTracker.clearAll();
        log.info("HitDetection shutdown complete");
    }

    public void updateConfig(HitDetectionConfig newConfig) {
        log.info("HitDetection configuration updated");
    }

    // ===========================
    // PUBLIC API
    // ===========================

    public static HitDetection getInstance() {
        if (instance == null) throw new IllegalStateException("HitDetection not initialized!");
        return instance;
    }

    public HitDetectionConfig getHitDetectionConfig() { return config; }
    public int getTrackedEntities() { return hitSnapshotTracker.getTrackedEntities().size(); }
}

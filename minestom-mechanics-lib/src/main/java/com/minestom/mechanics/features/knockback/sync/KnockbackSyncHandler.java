package com.minestom.mechanics.features.knockback.sync;

import com.minestom.mechanics.features.knockback.KnockbackSystem;
import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Re-implement

/**
 * Knockback synchronization handler for lag compensation
 */
public class KnockbackSyncHandler extends InitializableSystem {

    private static KnockbackSyncHandler instance;

    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackSyncHandler");

    // Configuration
    private boolean enabled = false;
    private boolean offGroundSyncEnabled = true;
    private long maxRewindTime = 1000; // ms
    // private int positionHistorySize = 30; // Unused field
    private double interpolationFactor = 0.75;

    // Player data
    private final Map<UUID, PlayerSyncData> syncDataMap = new ConcurrentHashMap<>();
    private final Map<UUID, PingTracker> pingTrackers = new ConcurrentHashMap<>();

    private Task pingTrackingTask;

    private KnockbackSyncHandler() {}

    public static KnockbackSyncHandler getInstance() {
        if (instance == null) {
            instance = new KnockbackSyncHandler();
        }
        return instance;
    }

    /**
     * Initialize the sync handler
     */
    public void initialize() {
        var eventHandler = MinecraftServer.getGlobalEventHandler();

        // Track player movement for position history
        eventHandler.addListener(PlayerMoveEvent.class, event -> {
            if (!enabled) return;

            Player player = event.getPlayer();
            PlayerSyncData data = getOrCreateSyncData(player);
            data.addPositionSnapshot(event.getNewPosition(), player.isOnGround());
        });

        this.markInitialized();

        // Clean up on disconnect
        eventHandler.addListener(PlayerDisconnectEvent.class, event -> {
            removePlayer(event.getPlayer());
        });

        this.pingTrackingTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    if (!enabled) return;

                    for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                        updatePing(player);
                    }
                })
                .repeat(TaskSchedule.seconds(1))
                .schedule();
    }

    /**
     * Apply lag compensation to knockback
     */
    public Vec compensateKnockback(Player victim, Vec baseKnockback, Entity attacker, boolean isGrounded) {
        if (!enabled || !shouldSyncPlayer(victim)) {
            return baseKnockback;
        }

        PlayerSyncData victimData = getSyncData(victim);
        if (victimData == null) {
            return baseKnockback;
        }

        // Skip if off-ground sync is disabled and player is airborne
        if (!offGroundSyncEnabled && !isGrounded) {
            return baseKnockback;
        }

        // Calculate rewind time
        long rewindTime = calculateRewindTime(victim, attacker, victimData);

        if (rewindTime <= 0 || rewindTime > maxRewindTime) {
            return baseKnockback;
        }

        // Get historical position
        Pos victimHistoricalPos = getInterpolatedPosition(victimData, rewindTime);

        if (victimHistoricalPos == null || victimHistoricalPos.equals(Pos.ZERO)) {
            return baseKnockback;
        }

        // Calculate compensated direction
        return calculateCompensatedKnockback(baseKnockback, victimHistoricalPos, attacker.getPosition());
    }

    /**
     * Calculate compensated knockback based on historical positions
     */
    private Vec calculateCompensatedKnockback(Vec baseKnockback, Pos victimHistoricalPos, Pos attackerPos) {
        // Calculate direction from historical positions
        double dx = victimHistoricalPos.x() - attackerPos.x();
        double dz = victimHistoricalPos.z() - attackerPos.z();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < 0.0001) {
            return baseKnockback;
        }

        // Normalize direction
        dx /= horizontalDistance;
        dz /= horizontalDistance;

        // Apply look weight if configured
        double lookWeight = KnockbackSystem.getInstance().getLookWeight();
        if (lookWeight > 0) {
            double yaw = Math.toRadians(attackerPos.yaw());
            double lookX = -Math.sin(yaw);
            double lookZ = Math.cos(yaw);

            // Blend directions
            dx = dx * (1 - lookWeight) + lookX * lookWeight;
            dz = dz * (1 - lookWeight) + lookZ * lookWeight;

            // Renormalize
            double newDist = Math.sqrt(dx * dx + dz * dz);
            if (newDist > 0.001) {
                dx /= newDist;
                dz /= newDist;
            }
        }

        // Extract original magnitude
        double originalMagnitude = Math.sqrt(
                baseKnockback.x() * baseKnockback.x() +
                        baseKnockback.z() * baseKnockback.z()
        );

        // Apply to compensated direction
        return new Vec(
                dx * originalMagnitude,
                baseKnockback.y(),
                dz * originalMagnitude
        );
    }

    /**
     * Get interpolated position at a specific rewind time
     */
    private Pos getInterpolatedPosition(PlayerSyncData syncData, long rewindMs) {
        if (syncData.positionHistory.isEmpty()) {
            return syncData.currentPosition;
        }

        long targetTime = System.currentTimeMillis() - rewindMs;
        PositionSnapshot before = null;
        PositionSnapshot after = null;

        // Find snapshots to interpolate between
        for (PositionSnapshot snapshot : syncData.positionHistory) {
            if (snapshot.timestamp <= targetTime) {
                before = snapshot;
            } else {
                after = snapshot;
                break;
            }
        }

        // Return appropriate position
        if (before == null) {
            return after != null ? after.position : syncData.currentPosition;
        }

        if (after == null) {
            return before.position;
        }

        // Interpolate between snapshots
        return interpolatePositions(before.position, after.position,
                before.timestamp, after.timestamp, targetTime);
    }

    /**
     * Linear interpolation between two positions
     */
    private Pos interpolatePositions(Pos from, Pos to, long fromTime, long toTime, long targetTime) {
        long timeDiff = toTime - fromTime;
        if (timeDiff == 0) {
            return from;
        }

        double t = (double)(targetTime - fromTime) / timeDiff;
        t = Math.max(0, Math.min(1, t));

        // Manual linear interpolation for Minestom Pos
        double x = from.x() + (to.x() - from.x()) * t;
        double y = from.y() + (to.y() - from.y()) * t;
        double z = from.z() + (to.z() - from.z()) * t;

        // Handle angle interpolation properly for yaw
        float yaw = interpolateAngle(from.yaw(), to.yaw(), (float)t);
        float pitch = from.pitch() + (to.pitch() - from.pitch()) * (float)t;

        return new Pos(x, y, z, yaw, pitch);
    }

    /**
     * Properly interpolate angles (handles wrapping at 360 degrees)
     */
    private float interpolateAngle(float from, float to, float t) {
        float diff = to - from;

        // Handle wrapping
        if (diff > 180) {
            diff -= 360;
        } else if (diff < -180) {
            diff += 360;
        }

        return from + diff * t;
    }

    /**
     * Calculate rewind time based on ping and interpolation
     */
    private long calculateRewindTime(Player victim, Entity attacker, PlayerSyncData victimData) {
        double victimPing = victimData.averagePing;

        // Add attacker ping if it's a player
        double attackerPing = 0;
        if (attacker instanceof Player attackerPlayer) {
            PlayerSyncData attackerData = getSyncData(attackerPlayer);
            if (attackerData != null) {
                attackerPing = attackerData.averagePing;
            }
        }

        // Calculate total compensation
        double totalPing = victimPing + attackerPing;

        // Apply interpolation factor
        return Math.round(totalPing * interpolationFactor);
    }

    /**
     * Update player state for sync calculations
     */
    public void updatePlayerState(Player player) {
        if (!enabled) return;

        PlayerSyncData data = getOrCreateSyncData(player);
        data.addVelocitySnapshot(player.getVelocity());
    }

    /**
     * Update ping tracking
     */
    private void updatePing(Player player) {
        PingTracker tracker = pingTrackers.computeIfAbsent(player.getUuid(), k -> new PingTracker());
        long ping = player.getLatency();
        tracker.recordPing(ping);

        PlayerSyncData syncData = getSyncData(player);
        if (syncData != null) {
            syncData.averagePing = tracker.getAveragePing();
            syncData.jitter = tracker.getJitter();
        }

        // Update knockback handler
        KnockbackSystem.PlayerKnockbackData kbData = KnockbackSystem.getInstance().getPlayerData(player);
        if (kbData != null) {
            kbData.lastPing = tracker.getAveragePing();
        }
    }

    /**
     * Record knockback application
     */
    public void recordKnockback(Player player, Vec knockback) {
        if (!enabled) return;

        PlayerSyncData data = getSyncData(player);
        if (data != null) {
            data.addVelocitySnapshot(knockback);
        }
    }

    /**
     * Check if player should be synced
     */
    private boolean shouldSyncPlayer(Player player) {
        PlayerSyncData data = getSyncData(player);
        return data != null && data.syncEnabled;
    }

    /**
     * Remove player from sync tracking
     */
    public void removePlayer(Player player) {
        UUID uuid = player.getUuid();

        // Remove and clear sync data
        PlayerSyncData data = syncDataMap.remove(uuid);
        if (data != null) {
            // CRITICAL: Clear the LinkedLists to free memory
            data.positionHistory.clear();
            data.velocityHistory.clear();
            log.debug("Cleared {} position snapshots, {} velocity snapshots",
                    data.positionHistory.size(), data.velocityHistory.size());
        }

        // Remove and clear ping tracker
        PingTracker tracker = pingTrackers.remove(uuid);
        if (tracker != null) {
            tracker.recentPings.clear();
        }

        log.debug("Cleaned up sync data for: {}", player.getUsername());
    }

    // Getters and setters

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setOffGroundSyncEnabled(boolean enabled) {
        this.offGroundSyncEnabled = enabled;
    }

    public void setMaxRewindTime(long maxRewindTime) {
        this.maxRewindTime = maxRewindTime;
    }

    public void setInterpolationFactor(double factor) {
        this.interpolationFactor = Math.max(0, Math.min(1, factor));
    }

    private PlayerSyncData getOrCreateSyncData(Player player) {
        return syncDataMap.computeIfAbsent(player.getUuid(), PlayerSyncData::new);
    }

    private PlayerSyncData getSyncData(Player player) {
        return syncDataMap.get(player.getUuid());
    }

    public int getTrackedPlayerCount() {
        return syncDataMap.size() + pingTrackers.size();
    }

    public void shutdown() {
        log.info("Shutting down KnockbackSyncHandler");

        if (pingTrackingTask != null) {
            pingTrackingTask.cancel();
            pingTrackingTask = null;
        }

        syncDataMap.clear();
        pingTrackers.clear();

        log.info("KnockbackSyncHandler shutdown complete");
    }

    /**
     * Player synchronization data
     */
    private static class PlayerSyncData {
        @SuppressWarnings("unused")
        public final UUID uuid;
        public final LinkedList<PositionSnapshot> positionHistory = new LinkedList<>();
        public final LinkedList<VelocitySnapshot> velocityHistory = new LinkedList<>();

        public volatile double averagePing = 0;
        @SuppressWarnings("unused")
        public volatile double jitter = 0;
        public volatile boolean syncEnabled = true;

        public volatile Pos currentPosition = Pos.ZERO;
        @SuppressWarnings("unused")
        public volatile Vec currentVelocity = Vec.ZERO;

        public PlayerSyncData(UUID uuid) {
            this.uuid = uuid;
        }

        public void addPositionSnapshot(Pos position, boolean onGround) {
            long now = System.currentTimeMillis();

            // Skip duplicate positions
            if (!positionHistory.isEmpty()) {
                PositionSnapshot last = positionHistory.getLast();
                if (last.position.equals(position)) {
                    return;
                }
            }

            positionHistory.add(new PositionSnapshot(position, onGround, now));

            // Limit history size
            while (positionHistory.size() > 30) {
                positionHistory.removeFirst();
            }

            currentPosition = position;
        }

        public void addVelocitySnapshot(Vec velocity) {
            long now = System.currentTimeMillis();
            velocityHistory.add(new VelocitySnapshot(velocity, now));

            // Limit history size
            while (velocityHistory.size() > 30) {
                velocityHistory.removeFirst();
            }

            currentVelocity = velocity;
        }
    }

    /**
     * Position snapshot
     */
    private static class PositionSnapshot {
        public final Pos position;
        @SuppressWarnings("unused")
        public final boolean onGround;
        public final long timestamp;

        public PositionSnapshot(Pos position, boolean onGround, long timestamp) {
            this.position = position;
            this.onGround = onGround;
            this.timestamp = timestamp;
        }
    }

    /**
     * Velocity snapshot
     */
    private static class VelocitySnapshot {
        @SuppressWarnings("unused")
        public final Vec velocity;
        @SuppressWarnings("unused")
        public final long timestamp;

        public VelocitySnapshot(Vec velocity, long timestamp) {
            this.velocity = velocity;
            this.timestamp = timestamp;
        }
    }

    /**
     * Ping tracking
     */
    private static class PingTracker {
        private final LinkedList<Long> recentPings = new LinkedList<>();

        public void recordPing(long pingMs) {
            recentPings.add(pingMs);
            while (recentPings.size() > 10) {
                recentPings.removeFirst();
            }
        }

        public double getAveragePing() {
            if (recentPings.isEmpty()) return 0;
            return recentPings.stream().mapToLong(Long::longValue).average().orElse(0);
        }

        public double getJitter() {
            if (recentPings.size() < 2) return 0;

            double avg = getAveragePing();
            double variance = recentPings.stream()
                    .mapToDouble(ping -> Math.pow(ping - avg, 2))
                    .average()
                    .orElse(0);

            return Math.sqrt(variance);
        }
    }
}

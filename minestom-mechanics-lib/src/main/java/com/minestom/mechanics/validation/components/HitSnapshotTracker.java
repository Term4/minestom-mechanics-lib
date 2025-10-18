package com.minestom.mechanics.validation.components;

import net.minestom.server.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks hit snapshots for analytics and logging.
 * Stores and retrieves hit data for competitive analysis.
 */
public class HitSnapshotTracker {

    private final Map<UUID, ServerSideDetector.HitSnapshot> lastHitSnapshots = new ConcurrentHashMap<>();

    /**
     * Store a hit snapshot for an entity
     */
    public void storeHitSnapshot(LivingEntity entity, ServerSideDetector.HitSnapshot snapshot) {
        lastHitSnapshots.put(entity.getUuid(), snapshot);
    }

    /**
     * Get the last hit snapshot for an entity.
     * Contains precise ray distance and validation tier.
     */
    public ServerSideDetector.HitSnapshot getLastHitSnapshot(LivingEntity entity) {
        return lastHitSnapshots.get(entity.getUuid());
    }

    /**
     * Get the precise ray distance from the last hit.
     * Falls back to approximate distance if ray missed.
     */
    public double getLastHitDistance(LivingEntity entity) {
        ServerSideDetector.HitSnapshot snapshot = lastHitSnapshots.get(entity.getUuid());
        return snapshot != null ? snapshot.rayDistance : -1.0;
    }

    /**
     * Get the validation tier used for the last hit.
     * Useful for competitive analysis (track ratio of PRIMARY vs FALLBACK).
     */
    public ServerSideDetector.ValidationTier getLastHitTier(LivingEntity entity) {
        ServerSideDetector.HitSnapshot snapshot = lastHitSnapshots.get(entity.getUuid());
        return snapshot != null ? snapshot.tier : null;
    }

    /**
     * Clean up tracking data for an entity
     */
    public void cleanup(LivingEntity entity) {
        lastHitSnapshots.remove(entity.getUuid());
        // Cleanup complete
    }

    /**
     * Clean up all tracking data
     */
    public void clearAll() {
        lastHitSnapshots.clear();
    }

    /**
     * Get all tracked entities
     */
    public java.util.Set<UUID> getTrackedEntities() {
        return lastHitSnapshots.keySet();
    }
}
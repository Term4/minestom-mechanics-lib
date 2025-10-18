package com.minestom.mechanics.geometry;

import com.minestom.mechanics.config.ServerConfig;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

/**
 * Consolidated raycasting utilities for both block and entity intersection detection.
 * ✅ CONSOLIDATED: Merged BlockRaycast + RayIntersection into single utility class.
 */
public class RaycastUtils {

    private final com.minestom.mechanics.config.world.WorldInteractionConfig worldConfig;

    public RaycastUtils(com.minestom.mechanics.config.world.WorldInteractionConfig worldConfig) {
        this.worldConfig = worldConfig;
    }
    
    /**
     * Create RaycastUtils using ServerConfig (recommended).
     */
    public static RaycastUtils create() {
        if (!ServerConfig.isWorldInteractionSet()) {
            throw new IllegalStateException("World interaction config not set! Call ServerConfig.setWorldInteraction() first.");
        }
        return new RaycastUtils(ServerConfig.getWorldInteraction());
    }

    // ===========================
    // BLOCK RAYCASTING
    // ===========================

    /**
     * Check if player is looking at a solid block within reach.
     * Uses gamemode-appropriate reach distances.
     *
     * @param player Player to check
     * @param origin Ray origin (typically eye position)
     * @param direction Ray direction (normalized)
     * @return true if aiming at a block within reach, false otherwise
     */
    public boolean isLookingAtBlock(Player player, Pos origin, Vec direction) {
        Instance instance = player.getInstance();
        if (instance == null) return false;

        // Determine reach based on gamemode
        double maxReach = player.getGameMode() == GameMode.CREATIVE
                ? worldConfig.creativeBlockReach()
                : worldConfig.survivalBlockReach();

        return isLookingAtBlock(instance, origin, direction, maxReach);
    }

    /**
     * Check if ray intersects a solid block within specified distance.
     *
     * @param instance World instance
     * @param origin Ray origin
     * @param direction Ray direction (normalized)
     * @param maxDistance Maximum distance to check
     * @return true if solid block found, false otherwise
     */
    public boolean isLookingAtBlock(Instance instance, Pos origin, Vec direction, double maxDistance) {
        Vec normalizedDir = direction.normalize();
        double stepSize = worldConfig.blockRaycastStep();
        int steps = (int) (maxDistance / stepSize);
        Vec stepVec = normalizedDir.mul(stepSize);

        Point current = origin;

        for (int i = 0; i < steps; i++) {
            Block block = instance.getBlock(current);

            // Check if block is solid (not air, not passable)
            if (!block.isAir() && block.isSolid()) {
                return true;
            }

            current = current.add(stepVec);
        }

        return false;
    }

    /**
     * Find the first solid block along a ray.
     * Returns null if no block found within distance.
     *
     * @param instance World instance
     * @param origin Ray origin
     * @param direction Ray direction (normalized)
     * @param maxDistance Maximum distance to check
     * @return Block position if found, null otherwise
     */
    public Pos findBlockAlongRay(Instance instance, Pos origin, Vec direction, double maxDistance) {
        Vec normalizedDir = direction.normalize();
        double stepSize = worldConfig.blockRaycastStep();
        int steps = (int) (maxDistance / stepSize);
        Vec stepVec = normalizedDir.mul(stepSize);

        Point current = origin;

        for (int i = 0; i < steps; i++) {
            Block block = instance.getBlock(current);

            if (!block.isAir() && block.isSolid()) {
                return new Pos(current.x(), current.y(), current.z(), 0, 0);
            }

            current = current.add(stepVec);
        }

        return null;
    }

    // ===========================
    // AABB RAYCASTING
    // ===========================

    /**
     * Performs ray-AABB intersection test with distance calculation.
     *
     * @param origin Ray origin (typically eye position)
     * @param direction Ray direction (will be normalized)
     * @param boxMin Minimum corner of AABB in world space
     * @param boxMax Maximum corner of AABB in world space
     * @param maxDistance Maximum ray distance to check
     * @return RayHitResult containing hit point and distance, or null if no hit
     */
    public static RayHitResult raycastAABB(Pos origin, Vec direction,
                                           Vec boxMin, Vec boxMax,
                                           double maxDistance) {
        Vec dir = direction.normalize();

        // Slab method for ray-AABB intersection
        double tMin = 0.0;
        double tMax = maxDistance;

        // X axis slab
        if (Math.abs(dir.x()) > 1e-9) {
            double t1 = (boxMin.x() - origin.x()) / dir.x();
            double t2 = (boxMax.x() - origin.x()) / dir.x();
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        } else if (origin.x() < boxMin.x() || origin.x() > boxMax.x()) {
            return null;
        }

        // Y axis slab
        if (Math.abs(dir.y()) > 1e-9) {
            double t1 = (boxMin.y() - origin.y()) / dir.y();
            double t2 = (boxMax.y() - origin.y()) / dir.y();
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        } else if (origin.y() < boxMin.y() || origin.y() > boxMax.y()) {
            return null;
        }

        // Z axis slab
        if (Math.abs(dir.z()) > 1e-9) {
            double t1 = (boxMin.z() - origin.z()) / dir.z();
            double t2 = (boxMax.z() - origin.z()) / dir.z();
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        } else if (origin.z() < boxMin.z() || origin.z() > boxMax.z()) {
            return null;
        }

        // Check validity
        if (tMin > tMax || tMax < 0) {
            return null;
        }

        // Handle ray starting inside box (avoid zero distance)
        double hitDistance = (tMin < 0) ? 0.001 : tMin;

        if (hitDistance > maxDistance) {
            return null;
        }

        Vec hitPoint = origin.asVec().add(dir.mul(hitDistance));
        return new RayHitResult(hitPoint, hitDistance);
    }

    /**
     * Convenience method for raycasting to an entity hitbox.
     * Automatically calculates world-space AABB bounds from entity position and hitbox.
     *
     * @param origin Ray origin (typically eye position)
     * @param direction Ray direction (will be normalized)
     * @param entityPos Entity position (at feet)
     * @param hitbox Entity's bounding box (relative to position)
     * @param maxDistance Maximum ray distance to check
     * @return RayHitResult containing hit point and distance, or null if no hit
     */
    public static RayHitResult raycastToHitbox(Pos origin, Vec direction,
                                               Pos entityPos, BoundingBox hitbox,
                                               double maxDistance) {
        // Calculate world-space AABB bounds (entity position is at feet)
        Vec boxMin = new Vec(
                entityPos.x() + hitbox.minX(),
                entityPos.y() + hitbox.minY(),
                entityPos.z() + hitbox.minZ()
        );

        Vec boxMax = new Vec(
                entityPos.x() + hitbox.maxX(),
                entityPos.y() + hitbox.maxY(),
                entityPos.z() + hitbox.maxZ()
        );

        return raycastAABB(origin, direction, boxMin, boxMax, maxDistance);
    }

    /**
     * Performs ray-AABB intersection test with hitbox expansion.
     * This is a convenience method for hit detection with expansion values.
     *
     * @param origin Ray origin (typically eye position)
     * @param direction Ray direction (will be normalized)
     * @param entityPos Entity position (at feet)
     * @param expansion Hitbox expansion values (Vec with x, y, z expansion)
     * @param maxDistance Maximum ray distance to check
     * @return RayHitResult containing hit point and distance, or null if no hit
     */
    public static RayHitResult raycastToHitbox(Pos origin, Vec direction,
                                               Pos entityPos, Vec expansion,
                                               double maxDistance) {
        // Base player hitbox dimensions (0.6x1.8x0.6)
        final double PLAYER_WIDTH = 0.6;
        final double PLAYER_HEIGHT = 1.8;
        
        // Calculate half-width with expansion
        double halfWidth = (PLAYER_WIDTH / 2.0) + expansion.x();
        
        // Calculate world-space AABB bounds with expansion added to base hitbox
        Vec boxMin = new Vec(
                entityPos.x() - halfWidth,
                entityPos.y(),
                entityPos.z() - halfWidth
        );

        Vec boxMax = new Vec(
                entityPos.x() + halfWidth,
                entityPos.y() + PLAYER_HEIGHT + expansion.y(),
                entityPos.z() + halfWidth
        );

        return raycastAABB(origin, direction, boxMin, boxMax, maxDistance);
    }

    // ===========================
    // RESULT CLASS
    // ===========================

    /**
     * Result of a ray-AABB intersection test.
     * Contains both the hit point and the distance along the ray.
     */
    public static class RayHitResult {
        private final Vec hitPoint;
        private final double distance;

        public RayHitResult(Vec hitPoint, double distance) {
            this.hitPoint = hitPoint;
            this.distance = distance;
        }

        /**
         * Get the world-space point where the ray intersects the AABB.
         */
        public Vec getHitPoint() {
            return hitPoint;
        }

        /**
         * Get the distance along the ray to the intersection point.
         * This is the actual ray distance, not Euclidean distance.
         */
        public double getDistance() {
            return distance;
        }
    }
}

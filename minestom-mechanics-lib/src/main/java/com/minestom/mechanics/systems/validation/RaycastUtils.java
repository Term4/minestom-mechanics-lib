package com.minestom.mechanics.systems.validation;

import com.minestom.mechanics.config.ServerConfig;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

/**
 * Raycasting utilities for block and entity intersection (block raycast, voxel traversal, ray-AABB).
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

    private static boolean isSolidBlock(Block block) {
        return !block.isAir() && block.isSolid();
    }

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
            if (isSolidBlock(instance.getBlock(current))) {
                return true;
            }

            current = current.add(stepVec);
        }

        return false;
    }

    /**
     * Find the first solid block along a ray using voxel (block-by-block) traversal.
     * Visits every block the ray passes through so corners and edges are not missed.
     * Used for swing obstruction detection.
     *
     * @param instance   World instance
     * @param origin     Ray origin (e.g. eye position)
     * @param direction  Ray direction (will be normalized)
     * @param maxDistance Maximum distance to check
     * @return Position of first solid block, or null if none within maxDistance
     */
    public Pos findFirstSolidBlockAlongRayVoxel(Instance instance, Pos origin, Vec direction, double maxDistance) {
        Vec d = direction.normalize();
        double ox = origin.x(), oy = origin.y(), oz = origin.z();
        double dx = d.x(), dy = d.y(), dz = d.z();

        int bx = (int) Math.floor(ox);
        int by = (int) Math.floor(oy);
        int bz = (int) Math.floor(oz);

        int stepX = dx > 1e-9 ? 1 : (dx < -1e-9 ? -1 : 0);
        int stepY = dy > 1e-9 ? 1 : (dy < -1e-9 ? -1 : 0);
        int stepZ = dz > 1e-9 ? 1 : (dz < -1e-9 ? -1 : 0);

        double tDeltaX = (Math.abs(dx) >= 1e-9) ? (1.0 / Math.abs(dx)) : Double.MAX_VALUE;
        double tDeltaY = (Math.abs(dy) >= 1e-9) ? (1.0 / Math.abs(dy)) : Double.MAX_VALUE;
        double tDeltaZ = (Math.abs(dz) >= 1e-9) ? (1.0 / Math.abs(dz)) : Double.MAX_VALUE;

        double tMaxX = stepX > 0 ? (bx + 1 - ox) / dx : (stepX < 0 ? (bx - ox) / dx : Double.MAX_VALUE);
        double tMaxY = stepY > 0 ? (by + 1 - oy) / dy : (stepY < 0 ? (by - oy) / dy : Double.MAX_VALUE);
        double tMaxZ = stepZ > 0 ? (bz + 1 - oz) / dz : (stepZ < 0 ? (bz - oz) / dz : Double.MAX_VALUE);

        int maxSteps = 1000;
        for (int i = 0; i < maxSteps; i++) {
            Point cellPoint = new Pos(bx + 0.5, by + 0.5, bz + 0.5, 0, 0);
            if (isSolidBlock(instance.getBlock(cellPoint))) {
                return new Pos(bx, by, bz, 0, 0);
            }

            double tNext = Math.min(Math.min(tMaxX, tMaxY), tMaxZ);
            if (tNext > maxDistance) {
                return null;
            }

            if (tNext == tMaxX) {
                bx += stepX;
                tMaxX += tDeltaX;
            } else if (tNext == tMaxY) {
                by += stepY;
                tMaxY += tDeltaY;
            } else {
                bz += stepZ;
                tMaxZ += tDeltaZ;
            }
        }
        return null;
    }

    // ===========================
    // AABB RAYCASTING
    // ===========================

    private static final double RAY_EPSILON = 1e-9;

    /** One axis of the slab test: updates tMinTMax[0] and tMinTMax[1]. Returns false if ray misses the slab. */
    private static boolean intersectSlab(double originComp, double dirComp, double boxLo, double boxHi, double[] tMinTMax) {
        if (Math.abs(dirComp) > RAY_EPSILON) {
            double t1 = (boxLo - originComp) / dirComp;
            double t2 = (boxHi - originComp) / dirComp;
            double tEnter = Math.min(t1, t2);
            double tExit = Math.max(t1, t2);
            tMinTMax[0] = Math.max(tMinTMax[0], tEnter);
            tMinTMax[1] = Math.min(tMinTMax[1], tExit);
            return tMinTMax[0] <= tMinTMax[1];
        }
        return originComp >= boxLo && originComp <= boxHi;
    }

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
        double[] tMinTMax = new double[]{0.0, maxDistance};

        if (!intersectSlab(origin.x(), dir.x(), boxMin.x(), boxMax.x(), tMinTMax)) return null;
        if (!intersectSlab(origin.y(), dir.y(), boxMin.y(), boxMax.y(), tMinTMax)) return null;
        if (!intersectSlab(origin.z(), dir.z(), boxMin.z(), boxMax.z(), tMinTMax)) return null;

        double tMin = tMinTMax[0];
        double tMax = tMinTMax[1];
        if (tMin > tMax || tMax < 0) return null;

        double hitDistance = (tMin < 0) ? 0.001 : tMin;
        if (hitDistance > maxDistance) return null;

        Vec hitPoint = origin.asVec().add(dir.mul(hitDistance));
        return new RayHitResult(hitPoint, hitDistance);
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

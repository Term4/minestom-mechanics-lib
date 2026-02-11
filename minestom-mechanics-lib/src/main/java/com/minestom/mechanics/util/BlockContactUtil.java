package com.minestom.mechanics.util;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Utility for detecting player-block contact via AABB intersection.
 * Reusable across damage types (cactus, berry bushes, etc.) and any
 * system that needs to know if a player is touching a specific block type.
 */
public final class BlockContactUtil {

    private BlockContactUtil() {}

    /**
     * Check if a player's bounding box intersects any block matching the predicate.
     *
     * @param instance the world instance
     * @param player the player to check
     * @param blockTest predicate to match blocks (e.g. {@code b -> b.compare(Block.CACTUS)})
     * @param excludeFace if non-null, contacts on this face are ignored
     *                    (e.g. {@link BlockFace#BOTTOM} to ignore standing under a block)
     * @return true if the player is touching a matching block on a non-excluded face
     */
    public static boolean isTouching(Instance instance, Player player, Predicate<Block> blockTest, @Nullable BlockFace excludeFace) {
        Pos pos = player.getPosition();
        BoundingBox box = player.getBoundingBox();

        double halfW = box.width() / 2;
        double halfD = box.depth() / 2;
        double minX = pos.x() - halfW, maxX = pos.x() + halfW;
        double minY = pos.y(), maxY = pos.y() + box.height();
        double minZ = pos.z() - halfD, maxZ = pos.z() + halfD;

        int bMinX = (int) Math.floor(minX), bMaxX = (int) Math.floor(maxX);
        int bMinY = (int) Math.floor(minY), bMaxY = (int) Math.floor(maxY);
        int bMinZ = (int) Math.floor(minZ), bMaxZ = (int) Math.floor(maxZ);

        for (int bx = bMinX; bx <= bMaxX; bx++) {
            for (int by = bMinY; by <= bMaxY; by++) {
                for (int bz = bMinZ; bz <= bMaxZ; bz++) {
                    if (!blockTest.test(instance.getBlock(bx, by, bz))) continue;

                    // Check AABB intersection
                    if (!aabbIntersects(minX, maxX, minY, maxY, minZ, maxZ,
                            bx, bx + 1, by, by + 1, bz, bz + 1)) continue;

                    // Check face exclusion
                    if (excludeFace != null) {
                        BlockFace face = getTouchedFace(pos, box, bx, by, bz);
                        if (face == excludeFace) continue;
                    }

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Convenience: check touching with no face exclusion.
     */
    public static boolean isTouching(Instance instance, Player player, Predicate<Block> blockTest) {
        return isTouching(instance, player, blockTest, null);
    }

    /**
     * Determine which face of a block the player's bounding box is touching,
     * using the smallest overlap axis.
     */
    public static BlockFace getTouchedFace(Pos playerPos, BoundingBox box, int bx, int by, int bz) {
        double halfW = box.width() / 2;
        double halfD = box.depth() / 2;
        double pMinX = playerPos.x() - halfW, pMaxX = playerPos.x() + halfW;
        double pMinY = playerPos.y(), pMaxY = playerPos.y() + box.height();
        double pMinZ = playerPos.z() - halfD, pMaxZ = playerPos.z() + halfD;

        double overlapX = Math.max(0, Math.min(pMaxX, bx + 1) - Math.max(pMinX, bx));
        double overlapY = Math.max(0, Math.min(pMaxY, by + 1) - Math.max(pMinY, by));
        double overlapZ = Math.max(0, Math.min(pMaxZ, bz + 1) - Math.max(pMinZ, bz));

        double centerX = playerPos.x();
        double centerY = playerPos.y() + box.height() * 0.5;
        double centerZ = playerPos.z();

        if (overlapY <= overlapX && overlapY <= overlapZ) {
            return centerY < by + 0.5 ? BlockFace.BOTTOM : BlockFace.TOP;
        }
        if (overlapX <= overlapZ) {
            return centerX < bx + 0.5 ? BlockFace.WEST : BlockFace.EAST;
        }
        return centerZ < bz + 0.5 ? BlockFace.NORTH : BlockFace.SOUTH;
    }

    /**
     * Check if a player is standing inside a block matching the predicate.
     * Scans a 3x2x3 area around the player's feet and body.
     */
    public static boolean isInsideBlock(Instance instance, Player player, Predicate<Block> blockTest) {
        Pos pos = player.getPosition();
        int bx = pos.blockX(), by = pos.blockY(), bz = pos.blockZ();

        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (blockTest.test(instance.getBlock(bx + dx, by + dy, bz + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ===========================
    // INTERNAL
    // ===========================

    private static boolean aabbIntersects(
            double aMinX, double aMaxX, double aMinY, double aMaxY, double aMinZ, double aMaxZ,
            double bMinX, double bMaxX, double bMinY, double bMaxY, double bMinZ, double bMaxZ) {
        return aMinX < bMaxX && aMaxX > bMinX
                && aMinY < bMaxY && aMaxY > bMinY
                && aMinZ < bMaxZ && aMaxZ > bMinZ;
    }
}

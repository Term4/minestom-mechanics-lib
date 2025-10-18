package com.minestom.mechanics.projectile.utils;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for projectile physics simulation.
 * Provides 1.8-style projectile movement with proper water physics.
 * 
 * Based on MinestomPVP's implementation.
 */
public class ProjectileUtil {
    /**
     * Simulate projectile movement with proper 1.8-style physics.
     * This handles water drag, air resistance, and collision detection.
     * 
     * @param entityPosition The current position of the projectile
     * @param entityVelocityPerTick The velocity per tick
     * @param entityBoundingBox The projectile's bounding box
     * @param worldBorder The world border
     * @param blockGetter Block getter for collision detection
     * @param entityHasPhysics Whether the entity has physics
     * @param previousPhysicsResult Previous physics result for optimization
     * @param singleCollision Whether to stop at first collision
     * @return The physics simulation result
     */
    public static @NotNull PhysicsResult simulateMovement(@NotNull Pos entityPosition, @NotNull Vec entityVelocityPerTick,
                                                          @NotNull BoundingBox entityBoundingBox, @NotNull WorldBorder worldBorder,
                                                          @NotNull Block.Getter blockGetter, boolean entityHasPhysics,
                                                          @Nullable PhysicsResult previousPhysicsResult,
                                                          boolean singleCollision) {
        final PhysicsResult physicsResult = entityHasPhysics ?
                CollisionUtils.handlePhysics(blockGetter, entityBoundingBox, entityPosition, entityVelocityPerTick, previousPhysicsResult, singleCollision) :
                CollisionUtils.blocklessCollision(entityPosition, entityVelocityPerTick);

        Pos newPosition = physicsResult.newPosition();
        Vec newVelocity = physicsResult.newVelocity();

        Pos positionWithinBorder = CollisionUtils.applyWorldBorder(worldBorder, entityPosition, newPosition);
        
        // Return result with proper position within world border
        return new PhysicsResult(
            positionWithinBorder, 
            newVelocity, 
            physicsResult.isOnGround(), 
            physicsResult.collisionX(), 
            physicsResult.collisionY(), 
            physicsResult.collisionZ(),
            physicsResult.originalDelta(), 
            physicsResult.collisionPoints(), 
            physicsResult.collisionShapes(), 
            physicsResult.collisionShapePositions(), 
            physicsResult.hasCollision(), 
            physicsResult.res()
        );
    }
}
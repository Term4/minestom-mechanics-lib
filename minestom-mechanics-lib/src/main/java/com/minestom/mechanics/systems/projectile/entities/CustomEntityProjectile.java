package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.config.constants.ProjectileConstants;
import com.minestom.mechanics.systems.projectile.utils.ProjectileUtil;
import net.minestom.server.ServerFlag;
import net.minestom.server.collision.*;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.projectile.ProjectileMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent;
import net.minestom.server.event.entity.projectile.ProjectileUncollideEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.EntityTeleportPacket;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.utils.chunk.ChunkCache;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.EntityCollisionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

// TODO: When I add the entity tracker system create an entry for each projectile entity!!! Necessary for proper cleanup / tracking

// TODO: Potentially add a feature to prevent modern clients from sending
//  arm swing packets when throwing projectiles? UNSURE if this is possible

// TODO: LOOOONNG way off, but add a couple extra projectile methods to the public API, like getting where
//  the projectile landed, the direction it ended up, etc. Could make for easier
//  custom projectiles (think rideable pearls, etc)

/**
 * Base class for all custom projectile entities in the mechanics system.
 * Provides proper 1.8-style physics, collision detection, and projectile behavior.
 * 
 * Based on the MinestomPVP implementation with proper water physics.
 */
public abstract class CustomEntityProjectile extends Entity {
    private static final BoundingBox UNSTUCK_BOX = new BoundingBox(0.12, 0.6, 0.12);

    // Core projectile state
    protected Vec velocity = Vec.ZERO;
    protected Pos shooterOriginPos;
    protected boolean onGround = false;

    // Shooter reference
    @Nullable
    protected Entity shooter;
    
    // Physics state
    protected boolean noClip = false;
    protected Vec collisionDirection = null;
    // Store velocity direction at moment of collision for rotation preservation
    protected Vec stuckVelocityDirection = null;
    
    // Track previous physics result for better collision
    private PhysicsResult previousPhysicsResult = null;
    
    // Track view angles for smooth rotation
    private float prevYaw, prevPitch;
    
    // Knockback configuration (common to all projectiles)
    protected boolean useKnockbackHandler = true;

    /**
     * Constructor for custom projectile entities.
     * 
     * @param shooter The entity that shot this projectile (can be null)
     * @param entityType The type of projectile entity
     */
    public CustomEntityProjectile(@Nullable Entity shooter, @NotNull EntityType entityType) {
        super(entityType);
        this.shooter = shooter;
        this.shooterOriginPos = shooter != null ? shooter.getPosition() : Pos.ZERO;
        setup();
    }
    
    private void setup() {
        // Configure entity for projectile physics
        collidesWithEntities = false;
        preventBlockPlacement = false;
        setAerodynamics(new Aerodynamics(getAerodynamics().gravity(), 0.99, 0.99));
        if (getEntityMeta() instanceof ProjectileMeta) {
            ((ProjectileMeta) getEntityMeta()).setShooter(shooter);
        }
        setSynchronizationTicks(getUpdateInterval());
        double hitboxSize = ProjectileConstants.PROJECTILE_HITBOX_SIZE;
        setBoundingBox(hitboxSize, hitboxSize, hitboxSize);
    }
    
    /**
     * Get the entity that shot this projectile.
     * 
     * @return The shooter entity, or null if no shooter
     */
    @Nullable
    public Entity getShooter() {
        return shooter;
    }
    
    /**
     * Set the shooter of this projectile.
     * 
     * @param shooter The new shooter entity
     */
    public void setShooter(@Nullable Entity shooter) {
        this.shooter = shooter;
        if (shooter != null) {
            this.shooterOriginPos = shooter.getPosition();
        }
    }
    
    /**
     * Get the current velocity of this projectile.
     * 
     * @return Current velocity vector
     */
    @NotNull
    public Vec getVelocity() {
        return velocity;
    }
    
    /**
     * Set the velocity of this projectile.
     * 
     * @param velocity The new velocity vector
     */
    public void setVelocity(@NotNull Vec velocity) {
        this.velocity = velocity;
        // Also set velocity on the entity for Minestom's physics
        super.setVelocity(velocity);
    }

    // TODO: Figure out what this is??? Maybe use ProjectileBehavior interface?
    /**
     * Called when this projectile hits an entity.
     * Override in subclasses to implement hit behavior.
     * 
     * @param entity The entity that was hit
     * @return True if the hit was processed, false otherwise
     */
    public boolean onHit(@NotNull Entity entity) {
        return false;
    }
    
    /**
     * Called when this projectile gets stuck (hits a block).
     * Override in subclasses to implement stuck behavior.
     * 
     * @return True if the projectile should be removed, false to continue
     */
    public boolean onStuck() {
        return false;
    }
    
    /**
     * Called when this projectile becomes unstuck.
     * Override in subclasses to implement unstuck behavior.
     */
    public void onUnstuck() {
        // Default: no special behavior
    }
    
    /**
     * Check if this projectile can hit the specified entity.
     * Override in subclasses to implement hit filtering.
     * 
     * @param entity The entity to check
     * @return True if the projectile can hit this entity, false otherwise
     */
    public boolean canHit(@NotNull Entity entity) {
        // Default: can hit any living entity except spectators
        // Allow hitting the shooter (self-hits are allowed)
        if (!(entity instanceof LivingEntity livingEntity)) return false;
        if (entity instanceof Player player) {
            // Don't hit players in spectator mode (GameMode or custom spectator tag)
            if (player.getGameMode() == GameMode.SPECTATOR) return false;
            
            // Check for custom spectator mode tag (used by test server spectator command)
            boolean isSpectator = Boolean.TRUE.equals(player.getTag(net.minestom.server.tag.Tag.Boolean("spectator_mode")));
            if (isSpectator) return false;
            
            // Don't hit dead players (they have no hitbox)
            // If player has health > 0, they're alive and can be hit regardless of tag state
            // Only block hits if they have the IS_DEAD tag AND health <= 0
            boolean isDead = Boolean.TRUE.equals(player.getTag(com.minestom.mechanics.systems.player.PlayerDeathHandler.IS_DEAD));
            if (isDead && livingEntity.getHealth() <= 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the origin position of the shooter when this projectile was created.
     * 
     * @return The shooter's position when projectile was shot
     */
    @NotNull
    public Pos getShooterOriginPos() {
        return shooterOriginPos;
    }
    
    /**
     * Set the origin position of the shooter.
     * 
     * @param shooterOriginPos The shooter's origin position
     */
    public void setShooterOriginPos(@NotNull Pos shooterOriginPos) {
        this.shooterOriginPos = shooterOriginPos;
    }
    
    /**
     * Check if this projectile is currently on the ground.
     * 
     * @return True if on ground, false otherwise
     */
    public boolean isOnGround() {
        return onGround;
    }
    
    /**
     * Set whether this projectile is on the ground.
     * 
     * @param onGround True if on ground, false otherwise
     */
    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }
    
    /**
     * Check if this projectile is stuck (hit a block).
     * 
     * @return True if stuck, false otherwise
     */
    public boolean isStuck() {
        return collisionDirection != null;
    }
    
    /**
     * Set whether this projectile is stuck.
     * 
     * @param stuck True if stuck, false otherwise
     */
    public void setStuck(boolean stuck) {
        if (stuck) {
            // Set collision direction to current velocity direction
            collisionDirection = velocity.normalize();
        } else {
            collisionDirection = null;
        }
    }
    
    @Override
    public void tick(long time) {
        super.tick(time);
        if (isRemoved()) return;
        
        if (isStuck() && shouldUnstuck()) {
            EventDispatcher.call(new ProjectileUncollideEvent(this));
            collisionDirection = null;
            stuckVelocityDirection = null;
            setNoGravity(false);
            onUnstuck();
        }
    }

    // TODO: Very mathy (could move to math / physics package). Plus ensure this is correct
    @Override
    protected void movementTick() {
        // Core physics implementation for 1.8-style projectiles
        this.gravityTickCount = isStuck() ? 0 : gravityTickCount + 1;
        if (vehicle != null) return;
        
        if (!isStuck()) {
            Vec diff = velocity.div(ServerFlag.SERVER_TICKS_PER_SECOND);
            
            // Prevent entity infinitely in the void
            if (instance.isInVoid(position)) {
                scheduler().scheduleNextProcess(this::remove);
                return;
            }
            
            // Always use block collision for our custom movement (entity hasPhysics is often false for projectiles, which would skip blocks)
            ChunkCache blockGetter = new ChunkCache(instance, currentChunk, Block.AIR);
            PhysicsResult physicsResult = ProjectileUtil.simulateMovement(
                position, diff, getBoundingBox(), instance.getWorldBorder(), blockGetter,
                true, previousPhysicsResult, true
            );
            this.previousPhysicsResult = physicsResult;
            
            Pos newPosition = physicsResult.newPosition();
            
            if (!noClip) {
                // We won't check collisions with self for first ticks of projectile's life
                boolean noCollideShooter = getAliveTicks() < ProjectileConstants.SHOOTER_COLLISION_DELAY_TICKS;
                Collection<EntityCollisionResult> entityResult = CollisionUtils.checkEntityCollisions(
                    instance, boundingBox.expand(0.1, 0.3, 0.1),
                    position.add(0, -0.3, 0), diff, 3, e -> {
                        if (noCollideShooter && e == shooter) return false;
                        return e != this && canHit(e);
                    }, physicsResult
                );
                
                if (!entityResult.isEmpty()) {
                    EntityCollisionResult collided = entityResult.stream().findFirst().orElse(null);
                    
                    var event = new ProjectileCollideWithEntityEvent(
                        this, new Pos(collided.collisionPoint()), collided.entity()
                    );
                    
                    EventDispatcher.call(event);
                    if (!event.isCancelled()) {
                        if (onHit(collided.entity())) {
                            scheduler().scheduleNextProcess(this::remove);
                            return;
                        }
                    }
                }
            }
            
            Chunk finalChunk = ChunkUtils.retrieve(instance, currentChunk, physicsResult.newPosition());
            if (!ChunkUtils.isLoaded(finalChunk)) return;
            
            // Track whether this tick transitions from flying to stuck,
            // so we can send an absolute teleport after yaw/pitch are computed.
            boolean justBecameStuck = false;
            
            if (physicsResult.hasCollision() && !isStuck()) {
                double signumX = physicsResult.collisionX() ? Math.signum(velocity.x()) : 0;
                double signumY = physicsResult.collisionY() ? Math.signum(velocity.y()) : 0;
                double signumZ = physicsResult.collisionZ() ? Math.signum(velocity.z()) : 0;
                Vec collisionDirection = new Vec(signumX, signumY, signumZ);
                
                Point collidedPosition = collisionDirection.add(physicsResult.newPosition()).apply(Vec.Operator.FLOOR);
                Block block = instance.getBlock(collidedPosition);
                
                var event = new ProjectileCollideWithBlockEvent(this, physicsResult.newPosition().withCoord(collidedPosition), block);
                EventDispatcher.call(event);
                if (!event.isCancelled()) {
                    // Store velocity direction before zeroing it out (for rotation preservation)
                    if (velocity.lengthSquared() > 0.0001) {
                        this.stuckVelocityDirection = velocity.normalize();
                    }
                    
                    setNoGravity(true);
                    setVelocity(Vec.ZERO);
                    this.collisionDirection = collisionDirection;
                    justBecameStuck = true;
                    
                    if (onStuck()) {
                        scheduler().scheduleNextProcess(this::remove);
                    }
                }
            }

            Aerodynamics aerodynamics = getAerodynamics();
            velocity = velocity.mul(
                    aerodynamics.horizontalAirResistance(),
                    aerodynamics.verticalAirResistance(),
                    aerodynamics.horizontalAirResistance()
            ).sub(0, hasNoGravity() ? 0 : getAerodynamics().gravity() * ServerFlag.SERVER_TICKS_PER_SECOND, 0);

            // CRITICAL FIX: Update parent entity velocity so synchronization packets have correct velocity
            super.setVelocity(velocity);

            // Ground state is updated by physics simulation
            onGround = physicsResult.isOnGround();
            
            float yaw = position.yaw();
            float pitch = position.pitch();
            
            if (!noClip) {
                if (isStuck() && stuckVelocityDirection != null) {
                    // When stuck, use the velocity direction from when collision occurred
                    // This preserves the arrow's orientation from its flight path
                    double horizontalLength = Math.sqrt(stuckVelocityDirection.x() * stuckVelocityDirection.x() + stuckVelocityDirection.z() * stuckVelocityDirection.z());
                    
                    // Calculate pitch from stored velocity direction
                    pitch = (float) Math.toDegrees(Math.atan2(stuckVelocityDirection.y(), horizontalLength));
                    
                    // Calculate yaw from stored velocity direction
                    yaw = (float) Math.toDegrees(Math.atan2(stuckVelocityDirection.x(), stuckVelocityDirection.z()));
                } else if (velocity.lengthSquared() > 0.0001) {
                    // Calculate direction from velocity (matches calculateDirectionFromVelocity logic)
                    // Use velocity direction for consistent rotation matching flight path
                    double horizontalLength = Math.sqrt(velocity.x() * velocity.x() + velocity.z() * velocity.z());
                    
                    // Calculate pitch (vertical angle) - matches old implementation: atan2(dy, sqrt(dx² + dz²))
                    pitch = (float) Math.toDegrees(Math.atan2(velocity.y(), horizontalLength));
                    
                    // Calculate yaw (horizontal angle) - matches old implementation: atan2(dx, dz)
                    yaw = (float) Math.toDegrees(Math.atan2(velocity.x(), velocity.z()));
                    
                    // Smooth rotation for better visuals
                    yaw = lerp(prevYaw, yaw);
                    pitch = lerp(prevPitch, pitch);
                }
            }
            
            this.prevYaw = yaw;
            this.prevPitch = pitch;
            
            Pos finalPos = newPosition.withView(yaw, pitch);
            
            // sendPackets is always false here.  During flight isStuck() was already
            // false so nothing changes.  On the collision tick we now also pass false
            // instead of true: refreshPosition still calls setPositionInternal (so
            // this.position is updated — critical for shouldUnstuck), but does NOT
            // send a relative EntityPositionAndRotationPacket.  The justBecameStuck
            // teleport below is the sole position packet on the collision tick.
            //
            // Sending both a relative move AND a teleport caused 1.8 visual glitches:
            // the relative move (from spawn to landing, encoded with 1/32 precision
            // loss by ViaVersion) briefly placed the entity inside the block, then the
            // teleport corrected it — visible as arrows clipping through glass then
            // snapping back, or bobbers dipping underground then popping up.
            refreshPosition(finalPos, noClip, false);
            
            // On collision, send an explicit zero-velocity + absolute teleport.
            // This is the critical fix for 1.8 clients connected via ViaVersion:
            // during flight no per-tick position packets are sent, so the client runs
            // its own physics and accumulates downward velocity.  The teleport resets
            // the client's tracked position to the exact landing point, and the
            // zero-velocity packet cancels accumulated client-side motion so it can't
            // dip the entity underground during interpolation.
            if (justBecameStuck) {
                sendPacketToViewersAndSelf(new EntityVelocityPacket(getEntityId(), Vec.ZERO));
                sendPacketToViewersAndSelf(new EntityTeleportPacket(
                        getEntityId(), finalPos, Vec.ZERO, 0, onGround
                ));
                this.lastSyncedPosition = finalPos;
            }
        }
    }
    
    private boolean shouldUnstuck() {
        if (collisionDirection == null) return false;
        
        Point collidedPoint = position.add(collisionDirection.mul(0.003));
        Point collidedBlockVec = new BlockVec(collidedPoint);
        Block block = instance.getBlock(collidedPoint);
        
        return !block.registry().collisionShape().intersectBox(collidedPoint.sub(collidedBlockVec).sub(0, 0.6, 0), UNSTUCK_BOX);
    }
    
    private static float lerp(float first, float second) {
        return first + (second - first) * ProjectileConstants.ROTATION_LERP_FACTOR;
    }
    
    @Override
    public void setView(float yaw, float pitch) {
        this.prevYaw = yaw;
        this.prevPitch = pitch;
        super.setView(yaw, pitch);
    }
    
    @Override
    protected void synchronizePosition() {
        if (isStuck()) {
            // When stuck, send an absolute teleport to correct any client-side drift.
            // Previously this returned early, which created a deadlock with
            // refreshPosition's sync-tick suppression: if collision happened on the
            // same tick as scheduled sync, NEITHER path sent a packet and the client
            // never learned the landing position.
            Pos pos = getPosition();
            sendPacketToViewersAndSelf(new EntityTeleportPacket(
                    getEntityId(), pos, Vec.ZERO, 0, isOnGround()
            ));
            this.lastSyncedPosition = pos;
            return;
        }
        super.synchronizePosition();
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        // Use shooter's entity ID for projectiles
        int data = 0;
        if (shooter != null) {
            data = shooter.getEntityId();
        }

        // Convert velocity from blocks/second to blocks/tick for the packet
        Vec velocityPerTick = velocity.div(ServerFlag.SERVER_TICKS_PER_SECOND);

        SpawnEntityPacket customSpawnPacket = new SpawnEntityPacket(
            getEntityId(),
            getUuid(),
            getEntityType(),
            getPosition(),
            getPosition().yaw(),
            data,
            velocityPerTick  // Send velocity in blocks/tick, not blocks/second
        );

        player.sendPacket(customSpawnPacket);
        player.sendPacket(getMetadataPacket());
    }
    
    protected int getUpdateInterval() {
        return ProjectileConstants.POSITION_UPDATE_INTERVAL;
    }
    
    /**
     * Set whether this projectile should use the KnockbackHandler system.
     * 
     * @param useKnockbackHandler True to use KnockbackHandler, false for simple knockback
     */
    public void setUseKnockbackHandler(boolean useKnockbackHandler) {
        this.useKnockbackHandler = useKnockbackHandler;
    }
    
    /**
     * Get whether this projectile uses the KnockbackHandler system.
     * 
     * @return True if using KnockbackHandler, false otherwise
     */
    public boolean isUseKnockbackHandler() {
        return useKnockbackHandler;
    }
}
package com.minestom.mechanics.projectile.entities;

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
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.utils.chunk.ChunkCache;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.EntityCollisionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

// TODO: Potentially add a feature to prevent modern clients from sending
//  arm swing packets when throwing projectiles? Would k

/**
 * Base class for all custom projectile entities in the mechanics system.
 * Provides proper 1.8-style physics, collision detection, and projectile behavior.
 * 
 * Based on the MinestomPVP implementation with proper water physics.
 */
public abstract class CustomEntityProjectile extends Entity {
    // TODO: Ensure these are the correct values
    private static final BoundingBox POINT_BOX = new BoundingBox(0, 0, 0);
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
        double hitboxSize = com.minestom.mechanics.projectile.ProjectileConstants.PROJECTILE_HITBOX_SIZE;
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

    // TODO: Very mathy, could move to aformentioned math / physics package
    /**
     * Shoot the projectile from a rotation with specified velocity and inaccuracy.
     * 
     * @param pitch The pitch angle in degrees
     * @param yaw The yaw angle in degrees  
     * @param yBias The y-bias for trajectory adjustment
     * @param power The power level (0-3, typically for bow power)
     * @param spread The inaccuracy factor (0.0 = perfect aim, higher = more spread)
     */
    public void shootFromRotation(float pitch, float yaw, float yBias, double power, double spread) {
        double dx = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double dy = -Math.sin(Math.toRadians(pitch + yBias));
        double dz = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= length;
        dy /= length;
        dz /= length;
        
        // Apply spread
        if (spread > 0) {
            java.util.Random random = java.util.concurrent.ThreadLocalRandom.current();
            spread *= com.minestom.mechanics.projectile.ProjectileConstants.SPREAD_MULTIPLIER;
            dx += random.nextGaussian() * spread;
            dy += random.nextGaussian() * spread;
            dz += random.nextGaussian() * spread;
        }
        
        // Set velocity in blocks per second (not per tick!)
        final double mul = ServerFlag.SERVER_TICKS_PER_SECOND * power;
        this.velocity = new Vec(dx * mul, dy * mul, dz * mul);
        
        // Also set velocity on the entity for Minestom's physics
        setVelocity(this.velocity);
        
        // Set view direction
        setView(
            (float) Math.toDegrees(Math.atan2(dx, dz)),
            (float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))
        );
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
        if (!(entity instanceof LivingEntity)) return false;
        if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) return false;
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
            
            // Use ProjectileUtil for proper 1.8-style physics with water drag
            ChunkCache blockGetter = new ChunkCache(instance, currentChunk, Block.AIR);
            PhysicsResult physicsResult = com.minestom.mechanics.projectile.utils.ProjectileUtil.simulateMovement(
                position, diff, POINT_BOX, instance.getWorldBorder(), blockGetter, 
                hasPhysics, previousPhysicsResult, true
            );
            this.previousPhysicsResult = physicsResult;
            
            Pos newPosition = physicsResult.newPosition();
            
            if (!noClip) {
                // We won't check collisions with self for first ticks of projectile's life
                boolean noCollideShooter = getAliveTicks() < com.minestom.mechanics.projectile.ProjectileConstants.SHOOTER_COLLISION_DELAY_TICKS;
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
                    setNoGravity(true);
                    setVelocity(Vec.ZERO);
                    this.collisionDirection = collisionDirection;
                    
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
                yaw = (float) Math.toDegrees(Math.atan2(diff.x(), diff.z()));
                pitch = (float) Math.toDegrees(Math.atan2(diff.y(), Math.sqrt(diff.x() * diff.x() + diff.z() * diff.z())));
                
                // Smooth rotation for better visuals
                yaw = lerp(prevYaw, yaw);
                pitch = lerp(prevPitch, pitch);
            }
            
            this.prevYaw = yaw;
            this.prevPitch = pitch;
            
            // Update position - use refreshPosition for proper sync (matches OLD implementation)
            refreshPosition(newPosition.withView(yaw, pitch), noClip, isStuck());
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
        return first + (second - first) * com.minestom.mechanics.projectile.ProjectileConstants.ROTATION_LERP_FACTOR;
    }
    
    @Override
    public void setView(float yaw, float pitch) {
        this.prevYaw = yaw;
        this.prevPitch = pitch;
        super.setView(yaw, pitch);
    }
    
    @Override
    protected void synchronizePosition() {
        // Don't sync position when stuck to prevent client-side jitter
        if (isStuck()) return;
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
        return com.minestom.mechanics.projectile.ProjectileConstants.POSITION_UPDATE_INTERVAL;
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
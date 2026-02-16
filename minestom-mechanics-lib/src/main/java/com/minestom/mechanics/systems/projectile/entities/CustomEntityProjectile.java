package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.config.constants.ProjectileConstants;
import com.minestom.mechanics.config.timing.TickScaler;
import com.minestom.mechanics.config.timing.TickScalingConfig;
import com.minestom.mechanics.systems.projectile.utils.ProjectileUtil;
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

    /**
     * Velocity hint magnitude sent to clients when stuck (in the FLIGHT DIRECTION).
     *
     * The 1.8 client's EntityArrow.onUpdate() flying branch raycasts from pos to
     * pos+motion to detect blocks.  For GROUND hits, gravity pulls the motion
     * downward into the ground — the client detects the block on its own.  For
     * WALLS and CEILINGS, gravity goes AWAY from the surface, so the client never
     * detects the block and the arrow visually falls.
     *
     * By sending a velocity hint in the flight direction:
     *   - The raycast goes toward the block (guaranteed — the arrow hit it)
     *   - hitVec - pos ≈ flightDir * bbox_half_extent → atan2 gives flight angle
     *   - inGround = true → rotation locked at flight angle
     *
     * 1.0 block/tick ensures even glancing angles cover the bbox half-extent.
     * Packet encoding: 1.0 * 8000 = 8000, within short range.
     */

    // TODO: Check if this is even necessary. I don't think it is (also move to projectile constants u FUCK)
    private static final double STUCK_VELOCITY_HINT = 1.0;

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
    /** Hit point from PhysicsResult (arrow tip with tiny bbox). Used for position and unstuck check. */
    protected Point stuckCollisionPoint = null;
    /**
     * Velocity sent to clients when stuck: flightDir * STUCK_VELOCITY_HINT.
     * Makes the 1.8 client's raycast detect the block for walls/ceilings,
     * and gives correct rotation via atan2(hitVec - pos) ≈ atan2(flightDir).
     */
    private Vec stuckPacketVelocity = Vec.ZERO;

    // Track previous physics result for better collision
    private PhysicsResult previousPhysicsResult = null;

    // Track view angles for displacement-based rotation
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
        collidesWithEntities = false;
        preventBlockPlacement = false;
        setAerodynamics(new Aerodynamics(getAerodynamics().gravity(), 0.99, 0.99));
        if (getEntityMeta() instanceof ProjectileMeta) {
            ((ProjectileMeta) getEntityMeta()).setShooter(shooter);
        }
        setSynchronizationTicks(getUpdateInterval());
        // CHANGE 1/2: Small bbox for block collision physics (matches Atlas's 0.01).
        // With a tiny bbox, the collision point IS the arrow tip — no surface offset
        // needed.  Entity collision uses expand() so hit detection is unaffected.
        setBoundingBox(0.01, 0.01, 0.01);
    }

    @Nullable
    public Entity getShooter() {
        return shooter;
    }

    public void setShooter(@Nullable Entity shooter) {
        this.shooter = shooter;
        if (shooter != null) {
            this.shooterOriginPos = shooter.getPosition();
        }
    }

    @NotNull
    public Vec getVelocity() {
        return velocity;
    }

    public void setVelocity(@NotNull Vec velocity) {
        this.velocity = velocity;
        super.setVelocity(velocity);
    }

    @Override
    protected Vec getVelocityForPacket() {
        if (isStuck() && stuckPacketVelocity.lengthSquared() > 0) {
            return stuckPacketVelocity;
        }
        return velocity.div(TickScaler.velocityPacketDivisor(TickScalingConfig.getMode()));
    }

    public boolean onHit(@NotNull Entity entity) {
        return false;
    }

    public boolean onStuck() {
        return false;
    }

    public void onUnstuck() {
    }

    // TODO: Turn this into a property (any entity can have "canHit" not just these fucking shitty presets)
    // will simplify codebase a FUCK tone
    public boolean canHit(@NotNull Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return false;
        if (entity instanceof Player player) {
            if (player.getGameMode() == GameMode.SPECTATOR) return false;
            boolean isSpectator = Boolean.TRUE.equals(player.getTag(net.minestom.server.tag.Tag.Boolean("spectator_mode")));
            if (isSpectator) return false;
            boolean isDead = Boolean.TRUE.equals(player.getTag(com.minestom.mechanics.systems.player.PlayerDeathHandler.IS_DEAD));
            if (isDead && livingEntity.getHealth() <= 0) return false;
        }
        return true;
    }

    @NotNull
    public Pos getShooterOriginPos() {
        return shooterOriginPos;
    }

    public void setShooterOriginPos(@NotNull Pos shooterOriginPos) {
        this.shooterOriginPos = shooterOriginPos;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean isStuck() {
        return collisionDirection != null;
    }

    public void setStuck(boolean stuck) {
        if (stuck) {
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
            stuckCollisionPoint = null;
            stuckPacketVelocity = Vec.ZERO;
            setNoGravity(false);
            onUnstuck();
        }
    }

    @Override
    protected void movementTick() {
        this.gravityTickCount = isStuck() ? 0 : gravityTickCount + 1;
        if (vehicle != null) return;

        if (!isStuck()) {
            Vec diff = velocity.div(TickScaler.velocityDivisor());

            if (instance.isInVoid(position)) {
                scheduler().scheduleNextProcess(this::remove);
                return;
            }

            ChunkCache blockGetter = new ChunkCache(instance, currentChunk, Block.AIR);
            PhysicsResult physicsResult = ProjectileUtil.simulateMovement(
                    position, diff, getBoundingBox(), instance.getWorldBorder(), blockGetter,
                    true, previousPhysicsResult, true
            );
            this.previousPhysicsResult = physicsResult;

            Pos newPosition = physicsResult.newPosition();

            if (!noClip) {
                int scaledDelay = TickScaler.scale(ProjectileConstants.SHOOTER_COLLISION_DELAY_TICKS, TickScalingConfig.getMode());
                boolean noCollideShooter = getAliveTicks() < scaledDelay;
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

            boolean justBecameStuck = false;

            // TODO: Go through and test with various combinations for the 1.8 / legacy fixes, see which are needed, and which are redundant.
            //  ALSO make the legacy fix A. optional, and B. target ONLY legacy clients

            // =====================================================================
            // COLLISION DETECTION (this is so cooked but it works)
            // =====================================================================
            // Select primary collision axis by largest velocity component among
            // axes that actually collided (collisionX/Y/Z booleans).
            //
            // With the tiny bbox (0.01), hitPoint ≈ arrow tip.  No surface offset
            // needed — just stick where physics says.
            // =====================================================================
            if (physicsResult.hasCollision() && !isStuck()) {
                boolean cx = physicsResult.collisionX();
                boolean cy = physicsResult.collisionY();
                boolean cz = physicsResult.collisionZ();

                double vx = Math.abs(velocity.x());
                double vy = Math.abs(velocity.y());
                double vz = Math.abs(velocity.z());

                int hitAxis = -1;
                if (cx && vx >= vy && vx >= vz) hitAxis = 0;
                else if (cy && vy >= vx && vy >= vz) hitAxis = 1;
                else if (cz && vz >= vx && vz >= vy) hitAxis = 2;
                else if (cy) hitAxis = 1;
                else if (cx) hitAxis = 0;
                else if (cz) hitAxis = 2;

                if (hitAxis != -1) {
                    Vec collisionDir = switch (hitAxis) {
                        case 0 -> new Vec(Math.signum(velocity.x()), 0, 0);
                        case 1 -> new Vec(0, Math.signum(velocity.y()), 0);
                        case 2 -> new Vec(0, 0, Math.signum(velocity.z()));
                        default -> Vec.ZERO;
                    };

                    Point[] points = physicsResult.collisionPoints();
                    Point hitPoint = (points != null && points.length > hitAxis && points[hitAxis] != null)
                            ? points[hitAxis]
                            : physicsResult.newPosition();

                    // Nudge 0.5 blocks into block for lookup: hitPoint is entity CENTER
                    // when bbox touched the face, not the surface contact point.
                    Point blockLookup = hitPoint.add(collisionDir.mul(0.5));
                    Block hitBlock = instance.getBlock(blockLookup, Block.Getter.Condition.TYPE);

                    if (hitBlock != null && !hitBlock.isAir()) {
                        // CHANGE 2/2: With tiny bbox, hitPoint IS the tip — use directly.
                        // No surface offset needed (0.01/2 = 0.005 blocks, invisible).
                        Point stuckPosition = hitPoint;

                        var event = new ProjectileCollideWithBlockEvent(
                                this, new Pos(stuckPosition.x(), stuckPosition.y(), stuckPosition.z()), hitBlock);
                        EventDispatcher.call(event);
                        if (!event.isCancelled()) {
                            // Flight-direction velocity hint BEFORE zeroing velocity.
                            if (velocity.lengthSquared() > 0.0001) {
                                this.stuckPacketVelocity = velocity.normalize().mul(STUCK_VELOCITY_HINT);
                            } else {
                                this.stuckPacketVelocity = collisionDir.mul(STUCK_VELOCITY_HINT);
                            }

                            setNoGravity(true);
                            setVelocity(Vec.ZERO);
                            this.collisionDirection = collisionDir;
                            this.stuckCollisionPoint = stuckPosition;
                            justBecameStuck = true;

                            if (onStuck()) {
                                scheduler().scheduleNextProcess(this::remove);
                            }
                        }
                    }
                }
            }

            Aerodynamics aerodynamics = getAerodynamics();
            var mode = TickScalingConfig.getMode();
            double hDrag = TickScaler.dragPerTick(aerodynamics.horizontalAirResistance(), mode);
            double vDrag = TickScaler.dragPerTick(aerodynamics.verticalAirResistance(), mode);
            double gravMult = TickScaler.gravityMultiplier(mode);
            velocity = velocity.mul(hDrag, vDrag, hDrag)
                    .sub(0, hasNoGravity() ? 0 : getAerodynamics().gravity() * gravMult, 0);

            super.setVelocity(velocity);

            onGround = physicsResult.isOnGround();

            float yaw = position.yaw();
            float pitch = position.pitch();

            if (!noClip) {
                if (isStuck() && !justBecameStuck) {
                    // Already stuck from a previous tick — keep frozen angle.
                    yaw = prevYaw;
                    pitch = prevPitch;
                } else {
                    // Flying OR just became stuck this tick.
                    // Use displacement for rotation (matches Atlas and 1.8 client).
                    Vec displacement = Vec.fromPoint(newPosition.sub(position));

                    if (displacement.lengthSquared() > 1e-8) {
                        double horizontalLength = Math.sqrt(displacement.x() * displacement.x() + displacement.z() * displacement.z());
                        yaw = (float) Math.toDegrees(Math.atan2(displacement.x(), displacement.z()));
                        pitch = (float) Math.toDegrees(Math.atan2(displacement.y(), horizontalLength));
                    } else if (justBecameStuck && stuckPacketVelocity.lengthSquared() > 1e-8) {
                        // Arrow hit on its very first tick (or with near-zero displacement).
                        // prevYaw/prevPitch are still default (0,0) = south.
                        // Use the flight direction from stuckPacketVelocity instead.
                        Vec dir = stuckPacketVelocity;
                        double hl = Math.sqrt(dir.x() * dir.x() + dir.z() * dir.z());
                        yaw = (float) Math.toDegrees(Math.atan2(dir.x(), dir.z()));
                        pitch = (float) Math.toDegrees(Math.atan2(dir.y(), hl));
                    }
                    // else: negligible movement and not just stuck, keep previous angles
                }
            }

            this.prevYaw = yaw;
            this.prevPitch = pitch;

            Pos finalPos = (justBecameStuck && stuckCollisionPoint != null)
                    ? new Pos(stuckCollisionPoint.x(), stuckCollisionPoint.y(), stuckCollisionPoint.z(), yaw, pitch)
                    : newPosition.withView(yaw, pitch);

            // sendPackets=false: the justBecameStuck teleport is the sole position
            // packet on the collision tick.  Sending both a relative move AND a
            // teleport caused 1.8 visual glitches via ViaVersion precision loss.
            refreshPosition(finalPos, noClip, false);

            // On collision: flight-direction velocity hint + absolute teleport.
            // The hint makes the 1.8 client's raycast detect the block (critical
            // for walls/ceilings where gravity goes away from the surface).
            if (justBecameStuck) {
                sendPacketToViewersAndSelf(new EntityVelocityPacket(getEntityId(), stuckPacketVelocity));
                sendPacketToViewersAndSelf(new EntityTeleportPacket(
                        getEntityId(), finalPos, stuckPacketVelocity, 0, onGround
                ));
                this.lastSyncedPosition = finalPos;
                scheduler().scheduleNextProcess(this::resyncStuckPosition);
            }
        }
    }

    // Could probably simplify this no lie
    private boolean shouldUnstuck() {
        if (collisionDirection == null || stuckCollisionPoint == null) return false;

        // Probe into the block we're stuck to.  stuckCollisionPoint is the
        // arrow tip (tiny bbox), so +0.5 goes well inside the block.
        Point intoBlock = stuckCollisionPoint.add(collisionDirection.mul(0.5));
        Point blockPos = new BlockVec(intoBlock);
        Block block = instance.getBlock(blockPos);

        if (block.isAir()) return true;

        Point localInBlock = stuckCollisionPoint.sub(blockPos).add(collisionDirection.mul(0.5));
        return !block.registry().collisionShape().intersectBox(localInBlock, UNSTUCK_BOX);
    }

    // 99% sure this DOES help a lot, would need more testing to confirm
    /** Re-send stuck position next tick to fix client disagreement. Zero velocity — client is already inGround. */
    private void resyncStuckPosition() {
        if (!isRemoved() && isStuck()) {
            sendPacketToViewersAndSelf(new EntityTeleportPacket(getEntityId(), getPosition(), Vec.ZERO, 0, isOnGround()));
            this.lastSyncedPosition = getPosition();
        }
    }

    @Override
    public void setView(float yaw, float pitch) {
        this.prevYaw = yaw;
        this.prevPitch = pitch;
        super.setView(yaw, pitch);
    }

    @Override
    protected void synchronizePosition() {
        // Test this, pretty sure it's unnecessary (was an attempt to remove stuck projectiles shakiness when legacy clients load them in on relog)
        if (isStuck()) {
            this.lastSyncedPosition = getPosition();
            return;
        }
        super.synchronizePosition();
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        int data = 0;
        if (shooter != null) {
            data = shooter.getEntityId();
        }

        if (isStuck()) {
            // Ensure data > 0 so 1.8 SpawnObject includes velocity.
            // Use viewer's entity ID — guaranteed valid in ViaVersion's tracker.
            data = player.getEntityId();
        }

        // For stuck arrows: zero yaw/pitch triggers the 1.8 init block
        // (checks prevRotation == 0,0) which instantly sets correct rotation
        // from the velocity hint.  getVelocityForPacket() returns
        // stuckPacketVelocity when stuck.
        // PRETTY SURE this is the ONLY necessary fix for legacy clients
        Pos spawnPos = isStuck() ? getPosition().withView(0f, 0f) : getPosition();

        SpawnEntityPacket customSpawnPacket = new SpawnEntityPacket(
                getEntityId(),
                getUuid(),
                getEntityType(),
                spawnPos,
                spawnPos.yaw(),
                data,
                getVelocityForPacket()
        );

        // Probably can just target legacy clients with this, and only if the fix is enabled for the instance / world
        // (we are NOT doing per player / per item tags for this because oh HELL no)
        player.sendPacket(customSpawnPacket);
        player.sendPacket(getMetadataPacket());
    }

    protected int getUpdateInterval() {
        return ProjectileConstants.POSITION_UPDATE_INTERVAL;
    }

    public void setUseKnockbackHandler(boolean useKnockbackHandler) {
        this.useKnockbackHandler = useKnockbackHandler;
    }

    public boolean isUseKnockbackHandler() {
        return useKnockbackHandler;
    }
}
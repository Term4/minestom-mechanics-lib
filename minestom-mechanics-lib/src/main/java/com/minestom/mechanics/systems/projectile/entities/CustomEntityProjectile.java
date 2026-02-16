package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.config.constants.ProjectileConstants;
import com.minestom.mechanics.config.timing.TickScaler;
import com.minestom.mechanics.config.timing.TickScalingConfig;
import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
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
import net.minestom.server.utils.PacketSendingUtils;
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
//  ALSO make the legacy fix optional / configurable.

/**
 * Base class for all custom projectile entities in the mechanics system.
 * Provides proper 1.8-style physics, collision detection, and projectile behavior.
 *
 * <h2>Stuck arrow rendering strategy (version-branched):</h2>
 *
 * <p><b>Problem:</b> Legacy (1.8) clients need a velocity "hint" pointing into the
 * block so their flying-branch raycast detects the surface and sets {@code inGround=true}.
 * There might be a better way to do this (NBT data maybe), but I haven't gotten that to work,
 * if you DO get that to work, please submit a PR.
 * Modern (1.21+) clients apply velocity before checking collisions, so any non-zero
 * velocity causes the arrow to slide off.</p>
 *
 * <p><b>Solution — "last-word hint":</b></p>
 * <ul>
 *   <li>{@code getVelocityForPacket()} always returns zero when stuck. All internal
 *       Minestom syncs (periodic sync, setVelocity broadcasts) send zero.</li>
 *   <li>{@code super.tick()} runs normally even when stuck — no freeze. The periodic
 *       sync fires on schedule and sends zero to everyone.</li>
 *   <li><b>After</b> {@code super.tick()} returns, our {@code tick()} override sends
 *       the velocity hint to legacy viewers only. This is always the LAST velocity
 *       packet legacy receives each tick, guaranteeing it overwrites any zero.</li>
 *   <li>Modern clients never receive the hint — they only see zero from internal syncs.</li>
 *   <li>On relog, legacy gets the hint in the spawn packet, plus the per-tick hint
 *       reinforcement from {@code tick()}. No race conditions possible.</li>
 * </ul>
 */
public abstract class CustomEntityProjectile extends Entity {
    private static final BoundingBox UNSTUCK_BOX = new BoundingBox(0.12, 0.6, 0.12);

    /**
     * Velocity hint magnitude for LEGACY (1.8) clients ONLY, in the FLIGHT DIRECTION.
     *
     * The 1.8 client's EntityArrow.onUpdate() flying branch raycasts from pos to
     * pos+motion to detect blocks. For walls/ceilings, gravity goes AWAY from
     * the surface, so without a hint the client never detects the block.
     *
     * NEVER sent to modern (1.21+) clients — they apply velocity before processing
     * inGround metadata, causing the arrow to slide off the block.
     * NEVER returned from getVelocityForPacket — only sent via explicit per-player
     * packets to legacy viewers from tick() and updateNewViewer().
     */
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
     * Stored flight direction * STUCK_VELOCITY_HINT.
     * ONLY sent to legacy clients via explicit per-player packets.
     * NEVER returned from getVelocityForPacket.
     */
    private Vec stuckPacketVelocity = Vec.ZERO;

    // Track previous physics result for better collision
    private PhysicsResult previousPhysicsResult = null;

    // Track view angles for displacement-based rotation
    private float prevYaw, prevPitch;

    // Knockback configuration (common to all projectiles)
    protected boolean useKnockbackHandler = true;

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
        setBoundingBox(0.01, 0.01, 0.01);
    }

    // =========================================================================
    // Public API
    // =========================================================================

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

    public boolean onHit(@NotNull Entity entity) {
        return false;
    }

    public boolean onStuck() {
        return false;
    }

    public void onUnstuck() {
    }

    // TODO: Turn this into a property (any entity can have "canHit" not just these fucking shitty presets)
    //  will simplify codebase a FUCK tone
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

    public void setUseKnockbackHandler(boolean useKnockbackHandler) {
        this.useKnockbackHandler = useKnockbackHandler;
    }

    public boolean isUseKnockbackHandler() {
        return useKnockbackHandler;
    }

    // =========================================================================
    // Velocity packet override — ALWAYS returns zero when stuck.
    // Internal Minestom syncs use this, so zero goes to everyone.
    // Legacy gets the hint AFTER via explicit sends in tick().
    // =========================================================================

    @Override
    protected Vec getVelocityForPacket() {
        // velocity is Vec.ZERO when stuck, so this naturally returns zero.
        return velocity.div(TickScaler.velocityPacketDivisor(TickScalingConfig.getMode()));
    }

    // =========================================================================
    // Tick — NO FREEZE. super.tick() always runs.
    // After super.tick() (and any internal syncs it does), we send the hint
    // to legacy viewers. This is always the LAST velocity packet they receive
    // this tick, guaranteeing it overwrites any zero the internal sync sent.
    // =========================================================================

    @Override
    public void tick(long time) {
        super.tick(time);
        if (isRemoved()) return;

        if (isStuck()) {
            // "Last word" hint: sent AFTER super.tick() so it overwrites any
            // zero that the periodic sync might have sent this tick.
            // Only legacy viewers receive this. Modern never sees the hint.
            if (stuckPacketVelocity.lengthSquared() > 0) {
                PacketSendingUtils.sendGroupedPacket(
                        getViewers(),
                        new EntityVelocityPacket(getEntityId(), stuckPacketVelocity),
                        this::isLegacyClient
                );
            }

            if (shouldUnstuck()) {
                EventDispatcher.call(new ProjectileUncollideEvent(this));
                collisionDirection = null;
                stuckCollisionPoint = null;
                stuckPacketVelocity = Vec.ZERO;
                setNoGravity(false);
                onUnstuck();
            }
        }
    }

    // =========================================================================
    // Movement tick — physics, collision, stuck handling
    // =========================================================================

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

            // --- Entity collision ---
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

            // =================================================================
            // BLOCK COLLISION DETECTION
            // =================================================================
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

                    Point blockLookup = hitPoint.add(collisionDir.mul(0.5));
                    Block hitBlock = instance.getBlock(blockLookup, Block.Getter.Condition.TYPE);

                    if (hitBlock != null && !hitBlock.isAir()) {
                        Point stuckPosition = hitPoint;

                        var event = new ProjectileCollideWithBlockEvent(
                                this, new Pos(stuckPosition.x(), stuckPosition.y(), stuckPosition.z()), hitBlock);
                        EventDispatcher.call(event);
                        if (!event.isCancelled()) {
                            // Compute hint BEFORE zeroing velocity
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

            // =================================================================
            // DRAG / GRAVITY
            // =================================================================
            Aerodynamics aerodynamics = getAerodynamics();
            var mode = TickScalingConfig.getMode();
            double hDrag = TickScaler.dragPerTick(aerodynamics.horizontalAirResistance(), mode);
            double vDrag = TickScaler.dragPerTick(aerodynamics.verticalAirResistance(), mode);
            double gravMult = TickScaler.gravityMultiplier(mode);
            velocity = velocity.mul(hDrag, vDrag, hDrag)
                    .sub(0, hasNoGravity() ? 0 : getAerodynamics().gravity() * gravMult, 0);

            super.setVelocity(velocity);

            onGround = physicsResult.isOnGround();

            // =================================================================
            // ROTATION
            // =================================================================
            float yaw = position.yaw();
            float pitch = position.pitch();

            if (!noClip) {
                if (isStuck() && !justBecameStuck) {
                    yaw = prevYaw;
                    pitch = prevPitch;
                } else {
                    Vec displacement = Vec.fromPoint(newPosition.sub(position));

                    if (displacement.lengthSquared() > 1e-8) {
                        double horizontalLength = Math.sqrt(displacement.x() * displacement.x() + displacement.z() * displacement.z());
                        yaw = (float) Math.toDegrees(Math.atan2(displacement.x(), displacement.z()));
                        pitch = (float) Math.toDegrees(Math.atan2(displacement.y(), horizontalLength));
                    } else if (justBecameStuck && stuckPacketVelocity.lengthSquared() > 1e-8) {
                        Vec dir = stuckPacketVelocity;
                        double hl = Math.sqrt(dir.x() * dir.x() + dir.z() * dir.z());
                        yaw = (float) Math.toDegrees(Math.atan2(dir.x(), dir.z()));
                        pitch = (float) Math.toDegrees(Math.atan2(dir.y(), hl));
                    }
                }
            }

            this.prevYaw = yaw;
            this.prevPitch = pitch;

            // =================================================================
            // POSITION UPDATE
            // =================================================================
            Pos finalPos = (justBecameStuck && stuckCollisionPoint != null)
                    ? new Pos(stuckCollisionPoint.x(), stuckCollisionPoint.y(), stuckCollisionPoint.z(), yaw, pitch)
                    : newPosition.withView(yaw, pitch);

            refreshPosition(finalPos, noClip, false);

            // =================================================================
            // INITIAL STICK PACKETS
            // =================================================================
            // On the stick tick, send version-branched teleports. The "last word"
            // hint in tick() will also fire after super.tick() returns, but these
            // explicit sends give the correct position immediately.
            // =================================================================
            if (justBecameStuck) {
                for (Player viewer : getViewers()) {
                    if (isLegacyClient(viewer)) {
                        viewer.sendPacket(new EntityVelocityPacket(getEntityId(), stuckPacketVelocity));
                        viewer.sendPacket(new EntityTeleportPacket(
                                getEntityId(), finalPos, stuckPacketVelocity, 0, onGround
                        ));
                    } else {
                        viewer.sendPacket(new EntityVelocityPacket(getEntityId(), Vec.ZERO));
                        viewer.sendPacket(new EntityTeleportPacket(
                                getEntityId(), finalPos, Vec.ZERO, 0, onGround
                        ));
                    }
                }
                this.lastSyncedPosition = finalPos;
                // Note: tick()'s "last word" hint fires after super.tick() returns,
                // which is after movementTick(). On the stick tick, this provides
                // an additional hint reinforcement for legacy after any periodic
                // sync zero. No global scheduler needed.
            }
        }
    }

    // =========================================================================
    // Unstuck check
    // =========================================================================

    private boolean shouldUnstuck() {
        if (collisionDirection == null || stuckCollisionPoint == null) return false;

        Point intoBlock = stuckCollisionPoint.add(collisionDirection.mul(0.5));
        Point blockPos = new BlockVec(intoBlock);
        Block block = instance.getBlock(blockPos);

        if (block.isAir()) return true;

        Point localInBlock = stuckCollisionPoint.sub(blockPos).add(collisionDirection.mul(0.5));
        return !block.registry().collisionShape().intersectBox(localInBlock, UNSTUCK_BOX);
    }

    // =========================================================================
    // Position sync — suppress position correction when stuck (we already
    // sent the correct position in justBecameStuck). The periodic velocity
    // sync in tick() still fires (sends zero via getVelocityPacket), but
    // our "last word" hint in tick() overwrites it for legacy.
    // =========================================================================

    @Override
    public void setView(float yaw, float pitch) {
        this.prevYaw = yaw;
        this.prevPitch = pitch;
        super.setView(yaw, pitch);
    }

    @Override
    protected void synchronizePosition() {
        if (isStuck()) {
            // No-op the position sync — arrow doesn't move.
            // IMPORTANT: we do NOT call super, so nextSynchronizationTick is
            // never advanced. This means the periodic sync's condition
            // (ticks >= nextSynchronizationTick) stays true every tick, and
            // sendPacketToViewers(getVelocityPacket()) sends zero every tick.
            // That's fine — our "last word" hint in tick() always follows it.
            this.lastSyncedPosition = getPosition();
            return;
        }
        super.synchronizePosition();
    }

    // =========================================================================
    // New viewer (relog / chunk enter) — version-branched spawn packets.
    // The per-tick "last word" hint in tick() will reinforce on the next tick.
    // =========================================================================

    @Override
    public void updateNewViewer(@NotNull Player player) {
        int data = (shooter != null) ? shooter.getEntityId() : 0;
        boolean isLegacy = isLegacyClient(player);

        // Legacy stuck: data > 0 ensures ViaVersion includes velocity bytes
        // in the 1.8 SpawnObject packet.
        if (isStuck() && isLegacy) {
            data = player.getEntityId();
        }

        Pos spawnPos;
        Vec spawnVelocity;

        if (isStuck()) {
            if (isLegacy) {
                // Legacy relog: hint velocity so the 1.8 flying branch raycasts
                // into the block on first tick → inGround=true.
                // Zero view (0,0) triggers the 1.8 init block that computes
                // rotation from the velocity direction.
                spawnPos = getPosition().withView(0f, 0f);
                spawnVelocity = stuckPacketVelocity;
            } else {
                // Modern relog: real position + view + zero velocity.
                spawnPos = getPosition();
                spawnVelocity = Vec.ZERO;
            }
        } else {
            spawnPos = getPosition();
            spawnVelocity = getVelocityForPacket();
        }

        player.sendPacket(new SpawnEntityPacket(
                getEntityId(), getUuid(), getEntityType(),
                spawnPos, spawnPos.yaw(), data, spawnVelocity
        ));
        player.sendPacket(getMetadataPacket());

        // Immediate hint reinforcement after spawn for legacy.
        // Even if something overwrites the spawn packet's velocity, this
        // separate EntityVelocityPacket re-establishes the hint.
        // And on the next server tick, tick()'s "last word" hint fires too.
        if (isStuck() && isLegacy) {
            player.sendPacket(new EntityVelocityPacket(getEntityId(), stuckPacketVelocity));
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    protected boolean isLegacyClient(@NotNull Player player) {
        return ClientVersionDetector.getInstance().getClientVersion(player) == ClientVersionDetector.ClientVersion.LEGACY;
    }

    protected int getUpdateInterval() {
        return ProjectileConstants.POSITION_UPDATE_INTERVAL;
    }
}
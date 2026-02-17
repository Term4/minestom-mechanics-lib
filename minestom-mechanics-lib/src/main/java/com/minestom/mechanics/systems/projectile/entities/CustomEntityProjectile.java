package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.config.constants.ProjectileConstants;
import com.minestom.mechanics.config.timing.TickScaler;
import com.minestom.mechanics.config.timing.TickScalingConfig;
import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import com.minestom.mechanics.systems.compatibility.legacy_1_8.LegacyProjectileCompat;
import com.minestom.mechanics.systems.projectile.utils.ProjectileUtil;
import net.minestom.server.MinecraftServer;
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

/**
 * Base class for all custom projectile entities in the mechanics system.
 * Provides proper 1.8-style physics, collision detection, and projectile behavior.
 *
 * <h2>Collision detection:</h2>
 * <p>Uses zero-size bounding box (matching MinestomPVP's POINT_BOX) with combined
 * collision direction + {@code newPosition} for block lookup. Zero bbox ensures
 * collision points land exactly on block boundaries, and {@code newPosition} is
 * the fully-resolved coordinate that both 1.8 and 1.21 clients agree with.</p>
 *
 * <h2>Stuck arrow rendering — version-branched:</h2>
 *
 * <h3>Modern (1.21+) — radio silence:</h3>
 * <p>When stuck, {@code super.tick()} is NOT called. No periodic velocity or
 * position syncs reach the client. The {@code inGround=true} metadata (set by
 * subclass in {@code onStuck()}) tells the client to stop flight physics.
 * Radio silence ensures nothing disturbs this state.</p>
 *
 * <h3>Legacy (1.8) — delegated to {@link LegacyProjectileCompat}:</h3>
 * <p>All legacy-specific hint, pullback, and relog logic is handled by the
 * {@link LegacyProjectileCompat} instance. See that class for the edge-aware
 * hint system (center hits → immediate hints, edge hits → natural prediction
 * → pullback + face-normal hints).</p>
 *
 * <p>Legacy compat can be disabled entirely via
 * {@link LegacyProjectileCompat#setEnabled(boolean)}, in which case only
 * modern (radio silence) behavior applies.</p>
 */
public abstract class CustomEntityProjectile extends Entity {

    // Core projectile state
    protected Vec velocity = Vec.ZERO;
    protected Pos shooterOriginPos;
    protected boolean onGround = false;

    // Shooter reference
    @Nullable
    protected Entity shooter;

    // Physics state
    protected boolean noClip = false;

    /**
     * Single-axis collision direction (face-normal of the primary hit surface).
     * Non-null when stuck. Used for unstuck checks.
     */
    protected Vec collisionDirection = null;

    /**
     * The collision point (from physicsResult.collisionPoints). Used for block lookup,
     * unstuck checks, and legacy edge detection.
     */
    protected Point stuckCollisionPoint = null;

    /**
     * Physics-resolved stuck position (physicsResult.newPosition()). Used for entity
     * placement — fixes 1.21 clients seeing arrow floating in front of block face.
     */
    protected Pos stuckResolvedPosition = null;

    // Legacy compatibility handler
    private final LegacyProjectileCompat legacyCompat = new LegacyProjectileCompat();

    // Track previous physics result for better collision
    private PhysicsResult previousPhysicsResult = null;

    // Track view angles for displacement-based rotation
    private float prevYaw, prevPitch;

    /** Set when we stick this tick; used for rotation/position update. */
    private boolean justBecameStuck;
    /** True if the stick was near a block edge (narrow shape). Used to skip legacy packets on stick tick — edge hits use Phase 1 "no packets" so client predicts naturally until pullback. */
    private boolean lastStickWasEdgeHit;
    private float stuckYaw, stuckPitch;

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
        // Zero-size bbox — collision resolves exactly on block boundaries.
        // Critical: even 0.01 causes overshoot that breaks 1.21 client's
        // floor(pos) → block lookup → inGround check.
        setBoundingBox(0, 0, 0);
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

    /** Called when the projectile hits an entity. Return true to remove the projectile. */
    public boolean onHit(@NotNull Entity entity) {
        return false;
    }

    /** Called when the projectile sticks in a block. Return true to remove the projectile. */
    public boolean onStuck() {
        return false;
    }

    /** Called when the projectile unsticks (block broken). */
    public void onUnstuck() {
    }

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

    /**
     * @return the legacy compatibility handler for this projectile
     */
    @NotNull
    public LegacyProjectileCompat getLegacyCompat() {
        return legacyCompat;
    }

    // =========================================================================
    // Velocity packet override
    //
    // Returns current velocity scaled for packets. When stuck, velocity is
    // Vec.ZERO so this returns zero. Safety net — during radio silence
    // super.tick() doesn't run, but anything else querying this gets zero.
    // =========================================================================

    @Override
    protected Vec getVelocityForPacket() {
        return velocity.div(TickScaler.velocityPacketDivisor(TickScalingConfig.getMode()));
    }

    // =========================================================================
    // Tick — RADIO SILENCE when stuck.
    //
    // When stuck, super.tick() is NOT called. This prevents ALL of Minestom's
    // internal periodic syncs from reaching any client.
    //
    //   Modern clients: absolute silence. inGround metadata persists.
    //
    //   Legacy clients: LegacyProjectileCompat handles per-tick hints.
    //   For center hits, hints are active immediately. For edge hits,
    //   hints are a no-op until the pullback fires.
    //
    // Subclass update(time) still runs for pickup, stuck time, removal, etc.
    // =========================================================================

    @Override
    public void tick(long time) {
        if (isStuck()) {
            if (isRemoved()) return;

            // Subclass update — pickup, stuck time, removal timer.
            // AbstractArrow.update() does NOT call super.update(), so no
            // Minestom internals are triggered.
            update(time);
            if (isRemoved()) return;

            // Legacy hint — delegates to LegacyProjectileCompat.
            // Center hits: active from first tick (flight direction hint).
            // Edge hits: no-op until pullback fires (natural prediction).
            legacyCompat.tickStuck(getEntityId(), getViewers());

            // Check if the block was broken
            if (shouldUnstuck()) {
                unstick();
            }
            return;
        }

        // Not stuck — normal tick with all Minestom internals
        super.tick(time);
        if (isRemoved()) return;
    }

    /**
     * Reset all stuck state and resume normal physics.
     */
    private void unstick() {
        EventDispatcher.call(new ProjectileUncollideEvent(this));

        // Let legacy compat clean up and send zero velocity
        legacyCompat.onUnstick(getEntityId(), getViewers());

        collisionDirection = null;
        stuckCollisionPoint = null;
        stuckResolvedPosition = null;
        setNoGravity(false);

        onUnstuck();
    }

    // =========================================================================
    // Movement tick — physics, collision, stuck handling
    // =========================================================================

    @Override
    protected void movementTick() {
        this.gravityTickCount = isStuck() ? 0 : gravityTickCount + 1;
        if (vehicle != null) return;

        if (isStuck()) return; // Radio silence — no movement processing

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
                        remove();
                        return;
                    }
                }
            }
        }

        Chunk finalChunk = ChunkUtils.retrieve(instance, currentChunk, physicsResult.newPosition());
        if (!ChunkUtils.isLoaded(finalChunk)) return;

        this.justBecameStuck = false;

        // =================================================================
        // BLOCK COLLISION — Atlas approach (ShapeImpl + collisionPoints).
        // =================================================================
        if (physicsResult.hasCollision()) {
            Block hitBlock = null;
            Point hitPoint = null;
            int hitAxis = -1;
            if (physicsResult.collisionShapes()[0] instanceof ShapeImpl) {
                hitBlock = instance.getBlock(physicsResult.collisionPoints()[0].sub(0, Vec.EPSILON, 0), Block.Getter.Condition.TYPE);
                hitPoint = physicsResult.collisionPoints()[0];
                hitAxis = 0;
            }
            if (physicsResult.collisionShapes()[1] instanceof ShapeImpl) {
                hitBlock = instance.getBlock(physicsResult.collisionPoints()[1].sub(0, Vec.EPSILON, 0), Block.Getter.Condition.TYPE);
                hitPoint = physicsResult.collisionPoints()[1];
                hitAxis = 1;
            }
            if (physicsResult.collisionShapes()[2] instanceof ShapeImpl) {
                hitBlock = instance.getBlock(physicsResult.collisionPoints()[2].sub(0, Vec.EPSILON, 0), Block.Getter.Condition.TYPE);
                hitPoint = physicsResult.collisionPoints()[2];
                hitAxis = 2;
            }
            if (hitBlock != null && hitPoint != null) {
                processBlockCollision(hitBlock, hitPoint, hitAxis, computePrimaryAxis(hitAxis), newPosition);
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

        if (!justBecameStuck) {
            super.setVelocity(velocity);
        }
        onGround = physicsResult.isOnGround();

        // =================================================================
        // ROTATION — displacement-based
        // =================================================================
        float yaw = position.yaw();
        float pitch = position.pitch();

        if (!noClip) {
            if (isStuck() && !justBecameStuck) {
                yaw = prevYaw;
                pitch = prevPitch;
            } else if (justBecameStuck) {
                yaw = stuckYaw;
                pitch = stuckPitch;
            } else {
                Vec displacement = newPosition.sub(position).asVec();
                if (displacement.lengthSquared() > 1e-8) {
                    double hl = Math.sqrt(displacement.x() * displacement.x() + displacement.z() * displacement.z());
                    yaw = (float) Math.toDegrees(Math.atan2(displacement.x(), displacement.z()));
                    pitch = (float) Math.toDegrees(Math.atan2(displacement.y(), hl));
                }
            }
        }

        this.prevYaw = yaw;
        this.prevPitch = pitch;

        // =================================================================
        // POSITION UPDATE
        // =================================================================
        // Use physics-resolved newPosition for stuck, not collision point. Atlas uses
        // collision point, but newPosition is where the engine places the entity after
        // resolution — fixes 1.21 clients sometimes seeing the arrow floating in front
        // of the block face (collision point can be offset in some hit configurations).
        Pos finalPos = (justBecameStuck && stuckResolvedPosition != null)
                ? stuckResolvedPosition.withView(yaw, pitch)
                : newPosition.withView(yaw, pitch);

        refreshPosition(finalPos, noClip, false);

        // =================================================================
        // INITIAL STICK — no explicit packets needed!
        //
        // Modern: radio silence. refreshPosition() updated position
        // internally, inGround metadata (set by onStuck()) handles client.
        //
        // Legacy center: LegacyProjectileCompat activated hints immediately.
        // Next tick's tickStuck() will send the first hint.
        //
        // Legacy edge: LegacyProjectileCompat scheduled the pullback.
        // No packets until it fires — 1.8 client predicts naturally.
        // =================================================================
        if (justBecameStuck) {
            this.lastSyncedPosition = finalPos;
        }
    }

    /** Process block collision (stuck logic). Returns true if collision was handled. */
    private boolean processBlockCollision(Block hitBlock, Point hitPoint, int hitAxis, Vec primaryDir, Pos resolvedPosition) {
        var event = new ProjectileCollideWithBlockEvent(
                this, new Pos(hitPoint.x(), hitPoint.y(), hitPoint.z()), hitBlock);
        EventDispatcher.call(event);
        if (event.isCancelled()) return false;

        Vec flightVelocity = velocity;
        if (flightVelocity.lengthSquared() > 1e-8) {
            double hl = Math.sqrt(flightVelocity.x() * flightVelocity.x()
                    + flightVelocity.z() * flightVelocity.z());
            stuckYaw = (float) Math.toDegrees(Math.atan2(flightVelocity.x(), flightVelocity.z()));
            stuckPitch = (float) Math.toDegrees(Math.atan2(flightVelocity.y(), hl));
        } else {
            stuckYaw = prevYaw;
            stuckPitch = prevPitch;
        }

        setNoGravity(true);
        this.velocity = Vec.ZERO;
        this.collisionDirection = primaryDir;
        this.stuckCollisionPoint = hitPoint;
        this.stuckResolvedPosition = resolvedPosition;
        this.justBecameStuck = true;

        boolean nearEdge = LegacyProjectileCompat.isNearEdge(hitPoint, hitAxis, hitBlock);
        this.lastStickWasEdgeHit = nearEdge;
        // Legacy uses resolved position for relog/spawn — same as what we render
        legacyCompat.onStick(getEntityId(), resolvedPosition, flightVelocity,
                primaryDir, stuckYaw, stuckPitch, nearEdge, getViewers());

        // Atlas: delay sync by one tick — "in rare situations there will be a slight
        // disagreement with the client and server on if it hit or not; scheduling next
        // tick so it doesn't jump to the hit position until it has actually hit".
        // We must send (not just return) when stuck for this to take effect.
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            if (isStuck() && !isRemoved()) {
                sendPacketToViewersAndSelf(new EntityTeleportPacket(
                        getEntityId(), getPosition(), Vec.ZERO, 0, true));
                this.lastSyncedPosition = getPosition();
            }
        });

        if (onStuck()) {
            remove();
        }
        return true;
    }

    /**
     * Convert an axis index to a single-axis unit vector in the direction
     * of travel on that axis.
     */
    private Vec computePrimaryAxis(int hitAxis) {
        return switch (hitAxis) {
            case 0 -> new Vec(Math.signum(velocity.x()), 0, 0);
            case 1 -> new Vec(0, Math.signum(velocity.y()), 0);
            case 2 -> new Vec(0, 0, Math.signum(velocity.z()));
            default -> {
                // Defensive fallback — combined direction
                double sx = Math.signum(velocity.x());
                double sy = Math.signum(velocity.y());
                double sz = Math.signum(velocity.z());
                yield new Vec(sx, sy, sz);
            }
        };
    }

    // =========================================================================
    // Unstuck check — is the block still there?
    // =========================================================================

    private boolean shouldUnstuck() {
        if (collisionDirection == null || stuckCollisionPoint == null) return false;

        Point intoBlock = stuckCollisionPoint.add(collisionDirection.mul(0.5));
        Block block = instance.getBlock(new BlockVec(intoBlock), Block.Getter.Condition.TYPE);

        // Only unstuck when block is broken. The intersectBox check caused false unsticks
        // on fences/slabs — narrow shapes don't reliably overlap the test box.
        return block.isAir();
    }

    // =========================================================================
    // Position sync — suppressed when stuck.
    // During radio silence super.tick() doesn't run so this is rarely
    // reached. Safety net in case anything else triggers a sync.
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
            this.lastSyncedPosition = getPosition();
            return;
        }
        // Use EntityTeleportPacket (absolute position) instead of super's relative packets.
        // Relative position packets are wrong for 1.8 clients (ViaVersion); legacy clients
        // never see the arrow move when we rely on the default Entity sync.
        Pos pos = getPosition();
        Vec vel = getVelocityForPacket();
        sendPacketToViewersAndSelf(new EntityTeleportPacket(
                getEntityId(), pos, vel, 0, isOnGround()
        ));
        sendPacketToViewersAndSelf(new EntityVelocityPacket(getEntityId(), vel));
        this.lastSyncedPosition = pos;
    }

    // =========================================================================
    // New viewer (relog / chunk enter) — version-branched spawn packets.
    //
    // Legacy stuck: delegated to LegacyProjectileCompat (hint in spawn
    //               packet, immediate reinforcement, zero view for rotation).
    //
    // Modern stuck: real position + zero velocity. inGround metadata handles
    //               the rest on the client side.
    //
    // Not stuck: standard spawn for both versions.
    // =========================================================================

    @Override
    public void updateNewViewer(@NotNull Player player) {
        int shooterData = (shooter != null) ? shooter.getEntityId() : 0;

        if (isStuck() && LegacyProjectileCompat.isEnabled()
                && ClientVersionDetector.getInstance().isLegacy(player)) {
            // Legacy relog/chunk enter — delegate to compat handler.
            legacyCompat.sendStuckSpawnPackets(
                    player, getEntityId(), getUuid(), getEntityType(),
                    getPosition(), shooterData
            );
            player.sendPacket(getMetadataPacket());
            return;
        }

        // Modern (stuck or not) + legacy (not stuck) — standard spawn
        Pos spawnPos = getPosition();
        Vec spawnVelocity = isStuck() ? Vec.ZERO : getVelocityForPacket();

        player.sendPacket(new SpawnEntityPacket(
                getEntityId(), getUuid(), getEntityType(),
                spawnPos, spawnPos.yaw(), shooterData, spawnVelocity
        ));
        player.sendPacket(getMetadataPacket());
    }

    // =========================================================================
    // Utility
    // =========================================================================

    protected int getUpdateInterval() {
        return ProjectileConstants.POSITION_UPDATE_INTERVAL;
    }
}
package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.config.projectiles.ProjectileConfig;
import com.minestom.mechanics.config.constants.ProjectileConstants;
import com.minestom.mechanics.systems.health.HealthSystem;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackPresets;
import com.minestom.mechanics.systems.projectile.components.ProjectileBehavior;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.other.FishingHookMeta;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO: Clean up AI slop

// TODO: Allow multiple bobbers to be hooked to one "victim" as of now new bobbers cause old ones to unhook
//  (maybe add a configurable "Max bobbers per player" value with a default set in projectile constants? Unsure.
/**
 * Fishing bobber entity with Minement-style visual hooking.
 * <p>
 * When the bobber hits a player it hooks for one tick (client sees the line), then unhooks.
 * Until the rod is retracted, when the hit player moves (position change only, not look),
 * the bobber re-hooks for one tick and unhooks again. The move listener is registered by {@link com.minestom.mechanics.systems.projectile.features.FishingRod}.
 */
public class FishingBobber extends CustomEntityProjectile implements ProjectileBehavior {

    // State enum
    public enum State {
        IN_AIR,
        HOOKED_ENTITY,
        BOBBING
    }

    // State tracking
    private State currentState = State.IN_AIR;
    private Entity hookedEntity;
    /** Player we hit for visual-only hook (hook then unhook next tick, re-hook on their move until rod retracted). */
    private Player pseudoHookedPlayer;
    private int stuckTime = 0;

    // Vision obstruction tracking
    private boolean shouldFallAfterHit = false;
    private int fallTicks = 0;
    private boolean hasAppliedFallVelocity = false;

    private final boolean legacy;
    private final double customGravity;

    // Knockback configuration
    private ProjectileKnockbackConfig knockbackConfig;
    private ProjectileConfig.FishingRodKnockbackMode knockbackMode;

    // Logging
    private static final LogUtil.SystemLogger log = LogUtil.system("FishingBobber");

    /** Tag on the hit player so FishingRod's PlayerMoveEvent can find this bobber for re-hook on move. */
    public static final Tag<FishingBobber> PSEUDO_HOOKED_BY = Tag.Transient("fishingPseudoHookedBy");

    public FishingBobber(@Nullable Entity shooter, boolean legacy) {
        super(shooter, EntityType.FISHING_BOBBER);
        this.legacy = legacy;
        setOwnerEntity(shooter);

        this.customGravity = legacy ?
                ProjectileConstants.FISHING_BOBBER_LEGACY_GRAVITY :
                ProjectileConstants.FISHING_BOBBER_MODERN_GRAVITY;

        this.knockbackConfig = ProjectileKnockbackPresets.FISHING_ROD;
        this.knockbackMode = ProjectileConfig.FishingRodKnockbackMode.BOBBER_RELATIVE;

        setAerodynamics(getAerodynamics().withGravity(0).withHorizontalAirResistance(0.92).withVerticalAirResistance(0.92));
        // No setSynchronizationTicks — use default
    }

    @Override
    public void tick(long time) {
        Pos before = getPosition();
        velocity = velocity.add(0, -customGravity * ServerFlag.SERVER_TICKS_PER_SECOND, 0);
        super.tick(time);
        Pos after = getPosition();

        // Log large movements — if the bobber teleports past a block server-side, it's tunneling
        double delta = before.distance(after);
        if (delta > 0.8) {
            log.info("[collision] bobber moved {:.2f} blocks in one tick: {} -> {} | onGround={} | velocity={}",
                    delta, before, after, onGround, velocity.length());
        }
    }

    @Override
    protected void movementTick() {
        if (currentState == State.HOOKED_ENTITY) {
            if (hookedEntity != null && isHookedEntityValid(hookedEntity, getInstance())) {
                Pos targetPos = hookedEntity.getPosition().add(0, hookedEntity.getBoundingBox().height() * 0.8, 0);
                teleport(targetPos);

                if (!(hookedEntity instanceof Player)) {
                    pullEntity(hookedEntity);
                }
            }
            return;
        }

        boolean wasMoving = velocity.lengthSquared() > 1.0;

        super.movementTick();

        if (wasMoving && (onGround || isStuck())) {
            // Zero velocity on landing so the 1.8 client stops predicting.
            sendPacketToViewersAndSelf(new net.minestom.server.network.packet.server.play.EntityVelocityPacket(
                    getEntityId(), Vec.ZERO
            ));
        } else if (!isStuck() && !onGround) {
            // Fix medium-cast overshoot on 1.8 clients.
            //
            // The server applies gravity in FishingBobber.tick() BEFORE movementTick,
            // then sends the post-drag velocity.  On the NEXT tick, gravity is applied
            // again before movement, so the server moves by (V_postDrag + gravity) / TPS.
            // But the 1.8 client uses the received velocity directly for movement
            // WITHOUT pre-applying gravity, so it moves by V_postDrag / TPS — which is
            // 0.04 blocks/tick MORE than the server (gravity / TPS = 0.04).
            //
            // Pre-apply the next tick's gravity to the sent velocity so the client's
            // per-tick displacement matches the server's exactly.
            Vec corrected = velocity.add(0, -customGravity * ServerFlag.SERVER_TICKS_PER_SECOND, 0);
            sendPacketToViewersAndSelf(new net.minestom.server.network.packet.server.play.EntityVelocityPacket(
                    getEntityId(), corrected.div(ServerFlag.SERVER_TICKS_PER_SECOND)
            ));
        }
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        Entity shooter = getShooter();
        int data = shooter != null ? shooter.getEntityId() : 0;

        // Send zero velocity in spawn packet — prevents 1.8 client from
        // predicting movement and overshooting into blocks. The server's
        // position packets drive all visual movement instead.
        var spawnPacket = new net.minestom.server.network.packet.server.play.SpawnEntityPacket(
                getEntityId(), getUuid(), getEntityType(), getPosition(),
                getPosition().yaw(), data, Vec.ZERO
        );

        player.sendPacket(spawnPacket);
        player.sendPacket(getMetadataPacket());
    }

    @Override
    public void update(long time) {
        if (!shouldFallAfterHit && currentState == State.IN_AIR) {
            Entity shooter = getShooter();
            if (shooter instanceof Player player) {
                Pos bobberPos = getPosition();
                Pos eyePos = player.getPosition().add(0, player.getEyeHeight(), 0);

                double distance = bobberPos.distance(eyePos);

                if (distance < 0.5 && getVelocity().length() > 0.1) {
                    startFallSequence();
                }
            }
        }

        if (shouldFallAfterHit) {
            fallTicks++;

            if (!hasAppliedFallVelocity && fallTicks >= 3) {
                Entity shooter = getShooter();
                if (shooter != null) {
                    Vec fallVelocity = calculateFallVelocity(getPosition(), shooter.getPosition());
                    setVelocity(fallVelocity);
                    hasAppliedFallVelocity = true;
                    log.debug("Applied fall velocity to bobber");
                }
            }
        }

        /* TODO: Have this updated to RETRACT the rod after a certain time, not just remove the bobber
        // Remove if stuck too long
        if (onGround) {
            stuckTime++;
            if (stuckTime >= 1200) { // 60 seconds
                remove();
                return;
            }
        } else {
            stuckTime = 0;
        }
        */
    }

    @Override
    public boolean onHit(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (!canHookEntity(entity, (Player) getShooter())) return false;

        // Do not hit the same player again after initial hit (when unhooked we re-hook on move; bobber must not re-apply damage)
        if (entity == pseudoHookedPlayer) {
            return false;
        }

        // Damage/knockback for players
        if (entity instanceof Player hitPlayer) {
            LivingEntity living = hitPlayer;

            HealthSystem.applyDamage(living, new Damage(DamageType.GENERIC, this, getShooter(), null, 0));

            // Pseudo-hook: hook now (bobber teleports to victim this tick), then unhook next tick so we stop following.
            // Re-hook on PlayerMoveEvent when they move; canHit excludes this player so bobber can fall when they stand still.
            pseudoHookedPlayer = hitPlayer;
            hitPlayer.setTag(PSEUDO_HOOKED_BY, this);
            setHookedEntity(hitPlayer);
            currentState = State.HOOKED_ENTITY;
            scheduleUnhookNextTick();
            return false;
        }

        // Hook — non-players only: stay hooked and pull until rod retracted
        setHookedEntity(entity);
        currentState = State.HOOKED_ENTITY;
        return false;
    }

    // ===========================
    // VISUAL HOOK LIFECYCLE
    // ===========================

    /**
     * Hook this bobber to the player visually (sets hookedEntity metadata so
     * the client draws the line). Does NOT pull. Call {@link #scheduleUnhookNextTick()}
     * immediately after.
     */
    public void hookVisualToPlayer(Player player) {
        setHookedEntity(player);
        currentState = State.HOOKED_ENTITY;
    }

    /**
     * Clear the visual hook (hookedEntity + state) without clearing pseudoHookedPlayer.
     * The bobber returns to IN_AIR visually but the pseudo-hook relationship persists.
     * Velocity is zeroed so it doesn't shoot off; gravity will take over next tick.
     */
    private void clearHookedEntityVisual() {
        setHookedEntity(null);
        currentState = State.IN_AIR;
        velocity = Vec.ZERO;
        setVelocity(Vec.ZERO);
    }

    /**
     * Schedule clearing the visual hook on the next tick so the client sees the
     * hook for exactly one tick before it disappears.
     */
    public void scheduleUnhookNextTick() {
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            if (!isRemoved()) {
                clearHookedEntityVisual();
            }
        }).delay(TaskSchedule.tick(1)).schedule();
    }

    @Nullable
    public Player getPseudoHookedPlayer() {
        return pseudoHookedPlayer;
    }

    // ===========================
    // ENTITY METADATA
    // ===========================

    private void setHookedEntity(@Nullable Entity entity) {
        this.hookedEntity = entity;
        ((FishingHookMeta) getEntityMeta()).setHookedEntity(entity);
    }

    private void setOwnerEntity(@Nullable Entity entity) {
        ((FishingHookMeta) getEntityMeta()).setOwnerEntity(entity);
    }

    // ===========================
    // ROD RETRACTION
    // ===========================

    public int retrieve() {
        if (!(getShooter() instanceof Player)) return 0;

        clearPseudoHookedPlayer();
        int durability = 0;
        if (hookedEntity != null) {
            if (!legacy) {
                pullEntity(hookedEntity);
                triggerStatus((byte) 31);
            }
            durability = hookedEntity instanceof ItemEntity ? 3 : 5;
        }

        remove();
        return durability;
    }

    private void clearPseudoHookedPlayer() {
        if (pseudoHookedPlayer != null) {
            pseudoHookedPlayer.removeTag(PSEUDO_HOOKED_BY);
            pseudoHookedPlayer = null;
        }
    }

    @Override
    public void remove() {
        clearPseudoHookedPlayer();
        super.remove();
    }

    // ===========================
    // PULL LOGIC
    // ===========================

    private void pullEntity(Entity entity) {
        Entity shooter = getShooter();
        if (shooter == null) return;

        if (entity instanceof Player) {
            var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
            var config = projectileManager.getProjectileConfig();
            if (!config.isFishingRodPullPlayers()) {
                return;
            }
        }

        Pos shooterPos = shooter.getPosition();
        Pos pos = getPosition();
        Vec velocity = new Vec(
                shooterPos.x() - pos.x(),
                shooterPos.y() - pos.y(),
                shooterPos.z() - pos.z()
        ).mul(0.1).mul(ServerFlag.SERVER_TICKS_PER_SECOND);

        entity.setVelocity(entity.getVelocity().add(velocity));
    }

    // ===========================
    // KNOCKBACK CONFIGURATION
    // ===========================

    public void setKnockbackConfig(ProjectileKnockbackConfig config) {
        this.knockbackConfig = config;
    }

    public ProjectileKnockbackConfig getKnockbackConfig() {
        return knockbackConfig;
    }

    public void setKnockbackMode(ProjectileConfig.FishingRodKnockbackMode mode) {
        this.knockbackMode = mode;
    }

    public ProjectileConfig.FishingRodKnockbackMode getKnockbackMode() {
        return knockbackMode;
    }

    // ===========================
    // PRIVATE HELPERS
    // ===========================

    private Vec calculateFallVelocity(Pos bobberPos, Pos shooterPos) {
        double dx = bobberPos.x() - shooterPos.x();
        double dz = bobberPos.z() - shooterPos.z();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance > 0.01) {
            double awayX = (dx / distance) * 0.8;
            double awayZ = (dz / distance) * 0.8;
            return new Vec(awayX, -0.4 * ServerFlag.SERVER_TICKS_PER_SECOND, awayZ);
        } else {
            return new Vec(0, -0.4 * ServerFlag.SERVER_TICKS_PER_SECOND, 0);
        }
    }

    private boolean isHookedEntityValid(Entity hookedEntity, net.minestom.server.instance.Instance currentInstance) {
        return hookedEntity != null &&
                !hookedEntity.isRemoved() &&
                hookedEntity.getInstance() == currentInstance;
    }

    private boolean canHookEntity(Entity entity, Player shooter) {
        if (entity == null || entity == shooter) {
            return false;
        }

        if (entity instanceof Player player && player.getGameMode() == GameMode.CREATIVE) {
            var dt = com.minestom.mechanics.systems.health.damage.DamageType.find(DamageType.GENERIC);
            // Check the shooter's held item for bypass tags
            Entity shooterEntity = getShooter();
            net.minestom.server.item.ItemStack rodItem = (shooterEntity instanceof Player p) ? p.getItemInMainHand() : null;
            var props = dt != null
                    ? dt.resolveProperties(this, shooterEntity, (LivingEntity) entity, rodItem)
                    : com.minestom.mechanics.systems.health.damage.DamageTypeProperties.ATTACK_DEFAULT;
            if (!props.bypassCreative()) return false;
        }

        return true;
    }

    private void startFallSequence() {
        this.shouldFallAfterHit = true;
        this.fallTicks = 0;
        this.hasAppliedFallVelocity = false;
    }

    // ===========================
    // POSITION SYNC
    // ===========================

    @Override
    protected void synchronizePosition() {
        Pos pos = getPosition();
        sendPacketToViewersAndSelf(new net.minestom.server.network.packet.server.play.EntityTeleportPacket(
                getEntityId(), pos, Vec.ZERO, 0, isOnGround()
        ));
        // Update lastSyncedPosition so that subsequent relative-move packets
        // (from refreshPosition) compute their deltas from the correct base.
        // Without this, lastSyncedPosition stays at the spawn position and every
        // relative delta is wildly wrong from the 1.8 client's perspective, since
        // the teleport above already moved the client's tracked serverPos.
        this.lastSyncedPosition = pos;
    }

    // ===========================
    // COLLISION
    // ===========================

    /**
     * Exclude the pseudo-hooked player from collision so the bobber can fall through them
     * and reach the ground when the victim isn't moving (no re-hook on move).
     */
    @Override
    public boolean canHit(@NotNull Entity entity) {
        if (entity == pseudoHookedPlayer) {
            return false;
        }
        return super.canHit(entity);
    }

    // ===========================
    // PROJECTILE BEHAVIOR INTERFACE
    // ===========================

    @Override
    public boolean onHit(Entity projectile, Entity hit) {
        return onHit(hit);
    }

    @Override
    public boolean onBlockHit(Entity projectile, Point position) {
        return false;
    }

    @Override
    public void onExpire(Entity projectile) {
        remove();
    }
}
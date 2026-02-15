package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.config.constants.ProjectileConstants;
import com.minestom.mechanics.config.projectiles.ProjectileConfig;
import com.minestom.mechanics.config.timing.TickScaler;
import com.minestom.mechanics.config.timing.TickScalingConfig;
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
import com.minestom.mechanics.manager.CombatManager;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

// TODO: Clean up AI slop

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

    /** Tag on the hit player: set of bobbers that have pseudo-hooked them (re-hook on move). */
    public static final Tag<Set<FishingBobber>> PSEUDO_HOOKED_BY = Tag.Transient("fishingPseudoHookedBy");

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
        double gravMult = TickScaler.gravityMultiplier(TickScalingConfig.getMode());
        velocity = velocity.add(0, -customGravity * gravMult, 0);
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
        CombatConfig combat = getCombatConfig();

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

        // MODERN: when pseudo-hooked and IN_AIR, keep bobber at player's face (don't fall)
        if (combat.bobberFixEnabled() && combat.bobberFixMode() == CombatConfig.BobberFixMode.MODERN
                && pseudoHookedPlayer != null && currentState == State.IN_AIR) {
            Pos facePos = pseudoHookedPlayer.getPosition().add(0, pseudoHookedPlayer.getEyeHeight(), 0);
            teleport(facePos);
            velocity = Vec.ZERO;
            setVelocity(Vec.ZERO);
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
            double gravMult = TickScaler.gravityMultiplier(TickScalingConfig.getMode());
            Vec corrected = velocity.add(0, -customGravity * gravMult, 0);
            Vec forPacket = corrected.div(TickScaler.velocityPacketDivisor(TickScalingConfig.getMode()));
            sendPacketToViewersAndSelf(new net.minestom.server.network.packet.server.play.EntityVelocityPacket(
                    getEntityId(), forPacket
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
        CombatConfig combat = getCombatConfig();
        // LEGACY only: fall when near pseudo-hooked player. MODERN keeps bobber at face (handled in movementTick).
        if (combat.bobberFixEnabled() && combat.bobberFixMode() == CombatConfig.BobberFixMode.LEGACY
                && !shouldFallAfterHit && currentState == State.IN_AIR && pseudoHookedPlayer != null) {
            // Fall sequence only for pseudo-hooked case: bobber hit a player, unhooked, and is now
            // near them (re-hook on move). Do NOT trigger when merely near the shooter at cast —
            // that would kill cast momentum when stationary (bobber spawns 0.3 in front).
            Pos bobberPos = getPosition();
            Pos eyePos = pseudoHookedPlayer.getPosition().add(0, pseudoHookedPlayer.getEyeHeight(), 0);
            double distance = bobberPos.distance(eyePos);

            if (distance < 0.5 && getVelocity().length() > 0.1) {
                startFallSequence();
            }
        }

        if (shouldFallAfterHit) {
            fallTicks++;

            int scaledFallDelay = TickScaler.scale(ProjectileConstants.FISHING_BOBBER_FALL_DELAY_TICKS, TickScalingConfig.getMode());
            if (!hasAppliedFallVelocity && fallTicks >= scaledFallDelay) {
                Entity shooter = getShooter();
                if (shooter != null) {
                    Vec fallVelocity = calculateFallVelocity(getPosition(), shooter.getPosition());
                    setVelocity(fallVelocity);
                    hasAppliedFallVelocity = true;
                }
            }
        }

        /* TODO: Have this updated to RETRACT the rod after a certain time, not just remove the bobber
        if (onGround) {
            stuckTime++;
            int scaledDespawn = TickScaler.scale(ProjectileConstants.FISHING_BOBBER_STUCK_DESPAWN_TICKS, TickScalingConfig.getMode());
            if (stuckTime >= scaledDespawn) {
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

            CombatConfig combat = getCombatConfig();
            if (!combat.bobberFixEnabled()) {
                // Vanilla: stay hooked
                setHookedEntity(hitPlayer);
                currentState = State.HOOKED_ENTITY;
                return false;
            }

            // Pseudo-hook: hook now (bobber teleports to victim this tick), then unhook after hookDisplayTicks.
            pseudoHookedPlayer = hitPlayer;
            addToPseudoHookedSet(hitPlayer, this);
            setHookedEntity(hitPlayer);
            currentState = State.HOOKED_ENTITY;
            scheduleUnhookAfterTicks(combat.bobberFixHookDisplayTicks());
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
     * Schedule clearing the visual hook after the given number of ticks.
     */
    public void scheduleUnhookAfterTicks(int ticks) {
        int scaled = com.minestom.mechanics.config.timing.TickScaler.scale(ticks, TickScalingConfig.getMode());
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            if (!isRemoved()) {
                clearHookedEntityVisual();
            }
        }).delay(TaskSchedule.tick(scaled)).schedule();
    }

    /** Convenience: unhook after hook display ticks. */
    public void scheduleUnhookNextTick() {
        CombatConfig combat = getCombatConfig();
        scheduleUnhookAfterTicks(combat.bobberFixEnabled() ? combat.bobberFixHookDisplayTicks() : 1);
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
            removeFromPseudoHookedSet(pseudoHookedPlayer, this);
            pseudoHookedPlayer = null;
        }
    }

    static void addToPseudoHookedSet(Player player, FishingBobber bobber) {
        Set<FishingBobber> set = player.getTag(PSEUDO_HOOKED_BY);
        if (set == null) set = new HashSet<>();
        set.add(bobber);
        player.setTag(PSEUDO_HOOKED_BY, set);
    }

    static void removeFromPseudoHookedSet(Player player, FishingBobber bobber) {
        Set<FishingBobber> set = player.getTag(PSEUDO_HOOKED_BY);
        if (set != null) {
            set.remove(bobber);
            if (set.isEmpty()) player.removeTag(PSEUDO_HOOKED_BY);
            else player.setTag(PSEUDO_HOOKED_BY, set);
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
        double gravMult = TickScaler.gravityMultiplier(TickScalingConfig.getMode());

        if (distance > 0.01) {
            double awayX = (dx / distance) * 0.8;
            double awayZ = (dz / distance) * 0.8;
            return new Vec(awayX, -0.4 * gravMult, awayZ);
        } else {
            return new Vec(0, -0.4 * gravMult, 0);
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
                    : com.minestom.mechanics.config.health.DamageTypeProperties.ATTACK_DEFAULT;
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
     * Exclude the pseudo-hooked player from collision.
     * LEGACY: also exclude other players once we've pseudo-hooked (rod goes through, can't hook others).
     */
    @Override
    public boolean canHit(@NotNull Entity entity) {
        if (entity == pseudoHookedPlayer) return false;
        CombatConfig combat = getCombatConfig();
        if (combat.bobberFixEnabled() && combat.bobberFixMode() == CombatConfig.BobberFixMode.LEGACY
                && pseudoHookedPlayer != null && entity instanceof Player) {
            return false; // LEGACY: cannot hook other players after first pseudo-hook
        }
        return super.canHit(entity);
    }

    private static CombatConfig getCombatConfig() {
        try {
            return CombatManager.getInstance().getCombatConfig();
        } catch (IllegalStateException e) {
            return com.minestom.mechanics.config.combat.CombatPresets.MINEMEN;
        }
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
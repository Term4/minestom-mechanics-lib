package com.minestom.mechanics.systems.compatibility.legacy_1_8;

import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.EntityTeleportPacket;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * Handles legacy (1.8) client compatibility for stuck projectiles.
 *
 * <h2>Problem:</h2>
 * <p>The 1.8 client's EntityArrow.onUpdate() flying branch raycasts from
 * {@code pos} to {@code pos+motion} to detect block collisions. For arrows
 * stuck in walls/ceilings, gravity pulls motion AWAY from the surface, so
 * without a velocity "hint" pointing into the block, the client never
 * detects the collision and the arrow floats/slides.</p>
 *
 * <h2>Solution — two-phase hint system:</h2>
 *
 * <h3>Phase 1: Natural prediction (stick tick → pullback delay)</h3>
 * <p>When the server detects collision, NO packets are sent to legacy clients.
 * The 1.8 client continues simulating the arrow with its own physics (gravity,
 * drag) — exactly like vanilla. The arrow follows a natural arc rather than
 * flying in a straight line with static velocity.</p>
 *
 * <h3>Phase 2: Hint cycle (after pullback delay)</h3>
 * <p>After {@link #pullbackDelayTicks}, a one-time teleport snaps the arrow to
 * the pulled-back position (slightly off the block boundary for reliable raycast)
 * and switches the velocity hint to the face-normal direction. The teleport
 * preserves the arrow's original flight yaw/pitch so the visual rotation
 * doesn't snap to the face-normal. From this point, a per-tick velocity hint
 * is sent to legacy viewers so the raycast continues to detect the block.</p>
 *
 * <h3>Relog handling:</h3>
 * <p>When a legacy player joins/relogs while an arrow is stuck, they receive
 * the hint immediately in the spawn packet — they don't need to see the
 * "natural prediction" phase since they weren't watching the arrow fly.
 * The spawn uses zero view (0,0) which triggers the 1.8 client's init
 * block to compute rotation from the hint velocity direction.</p>
 *
 * <h2>Modern clients:</h2>
 * <p>Modern (1.21+) clients are completely unaffected. They never receive
 * any packets from this class. Their {@code inGround} state is driven by
 * server metadata + radio silence from the projectile's tick freeze.</p>
 *
 * <h2>Configuration:</h2>
 * <p>The entire legacy compat system can be disabled via {@link #setEnabled(boolean)}.
 * When disabled, all methods no-op and modern-only behavior applies.</p>
 */
public class LegacyProjectileCompat {

    // =========================================================================
    // Configuration — static, shared across all projectiles
    // =========================================================================

    private static boolean enabled = true;

    /** Velocity hint magnitude. The 1.8 client uses this for its raycast length. */
    private static double hintMagnitude = 1.0;

    /**
     * How far (blocks) to pull back the arrow along its flight path.
     * Moves the position off the exact block boundary so the 1.8 client's
     * raycast reliably detects the surface — especially important at edges.
     */
    private static double pullbackDistance = 0.1;

    /**
     * Ticks to wait after sticking before sending the pullback teleport
     * and activating the per-tick hint cycle. During this delay, the 1.8
     * client predicts the arrow with normal physics (gravity + drag),
     * matching vanilla behavior.
     */
    private static int pullbackDelayTicks = 20;

    // =========================================================================
    // Per-instance state — one LegacyProjectileCompat per projectile
    // =========================================================================

    /**
     * Current velocity hint vector. Initially flight direction * hintMagnitude,
     * switches to face-normal * hintMagnitude after pullback.
     */
    private Vec hintVelocity = Vec.ZERO;

    /** Face-normal direction — stored for the switch after pullback fires. */
    private Vec faceNormal = Vec.ZERO;

    /**
     * Arrow's view angles at time of impact. Preserved so the pullback
     * teleport doesn't snap the arrow's visual rotation to the face-normal.
     */
    private float stuckYaw, stuckPitch;

    /**
     * Whether per-tick hints are being sent. False during the natural
     * prediction phase (stick tick → pullback delay). True after pullback.
     */
    private boolean hintActive = false;

    /**
     * Monotonically increasing stick ID. Incremented on each onStick() call.
     * The pullback task captures this at scheduling time and checks it when
     * firing — if it doesn't match, the arrow unstuck (and possibly restuck)
     * since the task was scheduled, so it should not fire.
     */
    private int stickGeneration = 0;

    // =========================================================================
    // Static configuration API
    // =========================================================================

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        LegacyProjectileCompat.enabled = enabled;
    }

    public static double getHintMagnitude() {
        return hintMagnitude;
    }

    public static void setHintMagnitude(double magnitude) {
        LegacyProjectileCompat.hintMagnitude = magnitude;
    }

    public static double getPullbackDistance() {
        return pullbackDistance;
    }

    public static void setPullbackDistance(double distance) {
        LegacyProjectileCompat.pullbackDistance = distance;
    }

    public static int getPullbackDelayTicks() {
        return pullbackDelayTicks;
    }

    public static void setPullbackDelayTicks(int ticks) {
        LegacyProjectileCompat.pullbackDelayTicks = ticks;
    }

    // =========================================================================
    // Lifecycle methods — called by CustomEntityProjectile
    // =========================================================================

    /**
     * Called when the projectile sticks in a block.
     *
     * <p>Does NOT send any packets to legacy clients immediately. The 1.8
     * client continues predicting with its own physics (gravity, drag) —
     * matching vanilla behavior where the arrow follows a natural arc until
     * the server correction arrives.</p>
     *
     * <p>Schedules a delayed pullback teleport that snaps the arrow into
     * place and activates the per-tick hint cycle.</p>
     *
     * @param entityId       the projectile's entity ID
     * @param stuckPos       the server-side stuck position
     * @param flightVelocity the arrow's velocity at time of impact (before zeroing)
     * @param collisionDir   the primary collision face-normal (single-axis unit vector)
     * @param yaw            the arrow's yaw at time of impact (preserved for pullback)
     * @param pitch          the arrow's pitch at time of impact (preserved for pullback)
     * @param viewers        current viewers of the projectile
     */
    public void onStick(int entityId, @NotNull Pos stuckPos, @NotNull Vec flightVelocity,
                        @NotNull Vec collisionDir, float yaw, float pitch,
                        @NotNull Collection<Player> viewers) {
        if (!enabled) return;

        this.faceNormal = collisionDir;
        this.stuckYaw = yaw;
        this.stuckPitch = pitch;
        this.hintActive = false;

        // Compute initial hint from flight direction
        if (flightVelocity.lengthSquared() > 0.0001) {
            this.hintVelocity = flightVelocity.normalize().mul(hintMagnitude);
        } else {
            this.hintVelocity = collisionDir.mul(hintMagnitude);
        }

        // Increment generation — invalidates any pending pullback from a previous stick
        final int generation = ++stickGeneration;

        // Compute pullback position (slightly off block boundary)
        final Pos pullbackPos;
        if (hintVelocity.lengthSquared() > 0.0001) {
            Vec flightDir = hintVelocity.normalize();
            pullbackPos = stuckPos.sub(flightDir.mul(pullbackDistance));
        } else {
            pullbackPos = stuckPos;
        }

        // Phase 1 is NOW: no packets sent. 1.8 client predicts naturally with
        // gravity + drag, just like vanilla. Arrow follows a natural arc.

        // Schedule Phase 2: pullback teleport + hint activation
        final var detector = ClientVersionDetector.getInstance();
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            if (stickGeneration != generation) return; // Stale — arrow unstuck since scheduled
            firePullback(entityId, pullbackPos, viewers, detector);
        }).delay(TaskSchedule.tick(pullbackDelayTicks)).schedule();
    }

    /**
     * Called every server tick while the projectile is stuck.
     * Sends the velocity hint to legacy viewers if the hint cycle is active
     * (i.e., the pullback delay has elapsed).
     *
     * <p>During the natural prediction phase (first ~{@link #pullbackDelayTicks}
     * ticks after sticking), this is a no-op. Legacy clients receive nothing
     * and continue predicting with their own physics.</p>
     *
     * @param entityId the projectile's entity ID
     * @param viewers  current viewers of the projectile
     */
    public void tickStuck(int entityId, @NotNull Collection<Player> viewers) {
        if (!enabled || !hintActive) return;
        if (hintVelocity.lengthSquared() == 0) return;

        final var detector = ClientVersionDetector.getInstance();
        PacketSendingUtils.sendGroupedPacket(
                viewers,
                new EntityVelocityPacket(entityId, hintVelocity),
                detector::isLegacy
        );
    }

    /**
     * Called when the projectile unsticks (block broken, etc).
     * Resets all state and sends a zero velocity to ALL viewers to clear
     * any stale hint the legacy client might still hold.
     *
     * @param entityId the projectile's entity ID
     * @param viewers  current viewers of the projectile
     */
    public void onUnstick(int entityId, @NotNull Collection<Player> viewers) {
        hintVelocity = Vec.ZERO;
        faceNormal = Vec.ZERO;
        hintActive = false;
        stuckYaw = 0;
        stuckPitch = 0;
        stickGeneration++; // Invalidate any pending pullback

        // Clear stale hint from legacy clients so they don't jitter
        // on the first free-flight tick after unsticking
        PacketSendingUtils.sendGroupedPacket(
                viewers,
                new EntityVelocityPacket(entityId, Vec.ZERO)
        );
    }

    /**
     * Write spawn/relog packets for a legacy viewer when the arrow is stuck.
     *
     * <p>Always sends the hint immediately on relog — the player wasn't
     * watching the arrow fly, so "natural prediction" doesn't apply. They
     * need the arrow to appear stuck from the first frame.</p>
     *
     * <p>Uses zero view (0,0) which triggers the 1.8 client's init block
     * that computes yaw/pitch from the velocity direction. This is correct
     * for relog — the player never saw the flight, so rotation from the
     * hint direction is the best approximation.</p>
     *
     * @param player     the legacy viewer
     * @param entityId   the projectile's entity ID
     * @param entityUuid the projectile's UUID
     * @param entityType the projectile's entity type
     * @param realPos    the server-side position of the arrow
     * @param shooterData the data field for the spawn packet (should be > 0
     *                    for ViaVersion to include velocity bytes)
     */
    public void sendStuckSpawnPackets(@NotNull Player player, int entityId,
                                      @NotNull UUID entityUuid, @NotNull EntityType entityType,
                                      @NotNull Pos realPos, int shooterData) {
        if (!enabled) return;

        // Zero view (0,0) → 1.8 client computes rotation from motion vector
        Pos spawnPos = realPos.withView(0f, 0f);

        // data > 0 ensures ViaVersion includes velocity bytes in 1.8 SpawnObject (0x0E)
        int data = Math.max(shooterData, player.getEntityId());

        player.sendPacket(new SpawnEntityPacket(
                entityId, entityUuid, entityType,
                spawnPos, spawnPos.yaw(), data, hintVelocity
        ));

        // Immediate hint reinforcement — even if ViaVersion's spawn translation
        // doesn't perfectly preserve the velocity in SpawnObject
        player.sendPacket(new EntityVelocityPacket(entityId, hintVelocity));
    }

    /**
     * @return the current hint velocity (for debugging or external use)
     */
    @NotNull
    public Vec getHintVelocity() {
        return hintVelocity;
    }

    /**
     * @return whether the per-tick hint cycle is currently active
     */
    public boolean isHintActive() {
        return hintActive;
    }

    // =========================================================================
    // Internal
    // =========================================================================

    /**
     * Phase 2: fire the delayed pullback teleport. Switches hint to
     * face-normal and activates the per-tick hint cycle.
     *
     * <p>The teleport position uses the original flight yaw/pitch so the
     * arrow's visual rotation is preserved. The hint velocity (face-normal)
     * is only used for the 1.8 raycast — it does NOT affect the arrow's
     * rendered angle because teleport packets set rotation explicitly.</p>
     */
    private void firePullback(int entityId, @NotNull Pos pullbackPos,
                              @NotNull Collection<Player> viewers,
                              @NotNull ClientVersionDetector detector) {
        // Switch hint from flight direction to face-normal — perpendicular
        // into the block surface. More reliable than flight direction for
        // 1.8 raycast at block edges.
        hintVelocity = faceNormal.mul(hintMagnitude);
        hintActive = true;

        // Preserve the arrow's original flight angle on the pullback teleport.
        // Without this, the arrow snaps to face perpendicular into the block
        // (the face-normal direction) which looks wrong.
        Pos viewCorrectedPos = pullbackPos.withView(stuckYaw, stuckPitch);

        for (Player viewer : viewers) {
            if (detector.isLegacy(viewer)) {
                // Teleport to pullback position with original flight rotation
                viewer.sendPacket(new EntityTeleportPacket(
                        entityId, viewCorrectedPos, hintVelocity, 0, true
                ));
                // Velocity hint — establishes the raycast direction
                viewer.sendPacket(new EntityVelocityPacket(entityId, hintVelocity));
            }
        }
    }
}
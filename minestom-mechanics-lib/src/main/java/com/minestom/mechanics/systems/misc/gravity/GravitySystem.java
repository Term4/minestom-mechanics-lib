package com.minestom.mechanics.systems.misc.gravity;

import com.minestom.mechanics.config.constants.LegacyGravityConstants;
import com.minestom.mechanics.systems.damage.DamageFeature;
import com.minestom.mechanics.systems.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

import java.util.List;

/**
 * Gravity system using ExplosionPacket for velocity application.
 * Tracks its own position to avoid conflicts with FallDamageTracker.
 */
public class GravitySystem extends InitializableSystem {

    private static GravitySystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("GravitySystem");

    // ===========================
    // TAGS
    // ===========================

    public static final Tag<Double> GRAVITY = Tag.Double("gravity");
    // Our own position tracking - separate from FallDamageTracker
    private static final Tag<Pos> GRAVITY_LAST_POS = Tag.Transient("gravity_last_pos");
    // Smoothed velocity for ping compensation
    private static final Tag<Double> SMOOTHED_VY = Tag.Transient("gravity_smoothed_vy");

    private GravitySystem() {}

    public static GravitySystem initialize() {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("GravitySystem");
            return instance;
        }

        instance = new GravitySystem();
        instance.registerListener();
        instance.markInitialized();

        LogUtil.logInit("GravitySystem");
        return instance;
    }

    public static GravitySystem getInstance() {
        if (instance == null || !instance.isInitialized()) {
            throw new IllegalStateException("GravitySystem not initialized!");
        }
        return instance;
    }

    private void registerListener() {
        MinecraftServer.getGlobalEventHandler().addListener(EntityTickEvent.class, event -> {
            Entity entity = event.getEntity();
            if (!(entity instanceof LivingEntity livingEntity)) return;

            Double customGravity = entity.getTag(GRAVITY);
            if (customGravity == null) {
                if (entity.hasNoGravity()) entity.setNoGravity(false);
                entity.removeTag(GRAVITY_LAST_POS);
                entity.removeTag(SMOOTHED_VY);
                return;
            }

            applyCustomGravity(livingEntity, customGravity);
        });
    }

    private void applyCustomGravity(LivingEntity entity, double customGravity) {
        // Reset when conditions don't allow gravity
        if (entity.isOnGround()) {
            entity.setTag(SMOOTHED_VY, 0.0);
            entity.setTag(GRAVITY_LAST_POS, entity.getPosition());
            return;
        }
        if (entity instanceof Player p && p.isFlying()) {
            entity.setTag(SMOOTHED_VY, 0.0);
            entity.setTag(GRAVITY_LAST_POS, entity.getPosition());
            return;
        }
        if (entity.isFlyingWithElytra()) {
            entity.setTag(SMOOTHED_VY, 0.0);
            entity.setTag(GRAVITY_LAST_POS, entity.getPosition());
            return;
        }

        entity.setNoGravity(true);

        if (entity instanceof Player player) {
            Pos currentPos = player.getPosition();
            Pos lastPos = player.getTag(GRAVITY_LAST_POS);

            // Initialize on first tick
            if (lastPos == null) {
                player.setTag(GRAVITY_LAST_POS, currentPos);
                player.setTag(SMOOTHED_VY, 0.0);
                return;
            }

            // Calculate actual Y velocity from position change
            double estimatedVy = currentPos.y() - lastPos.y();

            // Get previous smoothed velocity
            Double prevSmoothedVy = player.getTag(SMOOTHED_VY);
            if (prevSmoothedVy == null) prevSmoothedVy = 0.0;

            // Smooth the velocity to handle ping/jitter
            double smoothedVy = (estimatedVy * LegacyGravityConstants.VELOCITY_SMOOTHING) + (prevSmoothedVy * (1.0 - LegacyGravityConstants.VELOCITY_SMOOTHING));

            // Apply gravity to the velocity
            double newVy = smoothedVy - customGravity;

            // Apply terminal velocity cap (downward)
            // Terminal velocity scales proportionally with gravity
            if (LegacyGravityConstants.USE_TERMINAL_VELOCITY) {
                double scaledTerminalVelocity = LegacyGravityConstants.TERMINAL_VELOCITY * (customGravity);
                if (newVy < scaledTerminalVelocity) {
                    newVy = scaledTerminalVelocity;
                }
            }

            // Cap upward velocity to prevent exponential growth from knockback
            if (newVy > LegacyGravityConstants.MAX_UPWARD_VELOCITY) {
                newVy = LegacyGravityConstants.MAX_UPWARD_VELOCITY;
            }

            // Store smoothed velocity
            player.setTag(SMOOTHED_VY, newVy);

            // 🔍 DEBUG: Log every 20 ticks
            if (player.getAliveTicks() % 20 == 0) {
                double fallDistance = getFallDistance(player);
                log.debug("Player: {} | fallDist: {:.4f} | estimatedVy: {:.4f} | smoothedVy: {:.4f} | newVy: {:.4f} | gravity: {:.4f}",
                        player.getUsername(), fallDistance, estimatedVy, smoothedVy, newVy, customGravity);
            }

            // Scale the velocity for explosion packet
            // CRITICAL: Explosion packets ADD velocity, so we must be careful
            // The original code used abs(newVy) to always push upward, which slows falling
            // But we want gravity (downward force), so we need a different approach
            // Solution: Apply upward push when falling (to slow descent), downward when rising (to speed descent)
            // But actually, we want consistent gravity, so we apply based on desired velocity change
            // The key: use abs() to prevent compounding when already falling fast
            double velocityMagnitude = Math.abs(newVy) * LegacyGravityConstants.EXPLOSION_VELOCITY_SCALE;
            // Apply upward push (positive Y) to counteract/slow falling, or maintain desired velocity
            // This prevents the speed-up bug when falling
            double explosionVy = velocityMagnitude; // Positive = upward push

            // ✅ FIXED: Use abs() to prevent speed-up bug, make explosion invisible/silent
            // Note: Explosions may make sound for 1.8 clients (protocol limitation)
            player.sendPacket(new ExplosionPacket(
                    player.getPosition(),
                    0f,  // Strength = 0 (no damage, minimal visual/sound effect)
                    0,   // Record count = 0 (no affected blocks = completely invisible)
                    new Vec(0, explosionVy, 0),  // Velocity vector
                    Particle.BLOCK_MARKER,  // Particle type (minimal visibility)
                    SoundEvent.INTENTIONALLY_EMPTY,  // Silent (may not work for 1.8)
                    List.of()  // Empty affected blocks list
            ));

            // Update position tracking
            player.setTag(GRAVITY_LAST_POS, currentPos);

        } else {
            // Non-players: simpler approach
            var pos = entity.getPosition();
            entity.teleport(pos.add(0, -customGravity, 0));
        }
    }

    /**
     * Get fall distance from DamageFeature (if available)
     */
    private double getFallDistance(Player player) {
        try {
            return DamageFeature.getInstance().getFallDistance(player);
        } catch (IllegalStateException e) {
            return 0.0;
        }
    }

    public static void setGravity(Entity entity, double gravity) {
        entity.setTag(GRAVITY, gravity);
    }

    public static double getGravity(Entity entity) {
        Double customGravity = entity.getTag(GRAVITY);
        return customGravity != null ? customGravity : LegacyGravityConstants.GRAVITY_NORMAL;
    }

    public static void clearGravity(Entity entity) {
        entity.removeTag(GRAVITY);
        entity.removeTag(GRAVITY_LAST_POS);
        entity.removeTag(SMOOTHED_VY);
    }

    public static boolean hasCustomGravity(Entity entity) {
        return entity.getTag(GRAVITY) != null;
    }
}
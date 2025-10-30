package com.minestom.mechanics.systems.util;

import com.minestom.mechanics.systems.damage.DamageFeature;
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

import static java.lang.Math.abs;

/**
 * Gravity system using ExplosionPacket for velocity application.
 * Tracks its own position to avoid conflicts with FallDamageTracker.
 */
public class GravitySystem extends InitializableSystem {

    private static GravitySystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("GravitySystem");

    // ===========================
    // TUNING CONSTANTS - ADJUST THESE!
    // ===========================

    /**
     * Scale factor for explosion velocity.
     * Controls how strongly gravity affects the player.
     * 0.05 = gentle/moon-like, 0.1 = normal-ish, 0.2 = heavy
     */
    private static final double EXPLOSION_VELOCITY_SCALE = 0.22;

    /**
     * Maximum downward velocity (NEGATIVE value).
     * Prevents unlimited acceleration.
     */
    private static final double TERMINAL_VELOCITY = -0.5;

    /**
     * Whether to apply terminal velocity cap.
     */
    private static final boolean USE_TERMINAL_VELOCITY = true;

    /**
     * Smoothing factor for velocity changes (0.0 to 1.0).
     * Higher = more responsive, Lower = smoother (helps with ping)
     * 1.0 = no smoothing, 0.7 = moderate smoothing
     */
    private static final double VELOCITY_SMOOTHING = 0.85;

    // ===========================
    // TAGS
    // ===========================

    public static final Tag<Double> GRAVITY = Tag.Double("gravity");
    // Our own position tracking - separate from FallDamageTracker
    private static final Tag<Pos> GRAVITY_LAST_POS = Tag.Transient("gravity_last_pos");
    // Smoothed velocity for ping compensation
    private static final Tag<Double> SMOOTHED_VY = Tag.Transient("gravity_smoothed_vy");

    // Common gravity values
    public static final double GRAVITY_NORMAL = 0.08;
    public static final double GRAVITY_PROJECTILE = 0.05;
    public static final double GRAVITY_NONE = 0.0;
    public static final double GRAVITY_LOW = 0.02;
    public static final double GRAVITY_HIGH = 0.16;

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
            double smoothedVy = (estimatedVy * VELOCITY_SMOOTHING) + (prevSmoothedVy * (1.0 - VELOCITY_SMOOTHING));

            // Apply gravity to the velocity
            double newVy = smoothedVy - customGravity;

            // Apply terminal velocity cap
            if (USE_TERMINAL_VELOCITY && newVy < TERMINAL_VELOCITY) {
                newVy = TERMINAL_VELOCITY;
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
            double scaledVy = abs(newVy) * EXPLOSION_VELOCITY_SCALE;

            // ✅ FIXED: Use scaledVy instead of constant!
            player.sendPacket(new ExplosionPacket(
                    player.getPosition(),
                    0f,
                    0,
                    new Vec(0, scaledVy, 0),  // ← Use calculated value!
                    Particle.BLOCK_MARKER,
                    SoundEvent.INTENTIONALLY_EMPTY,
                    List.of()
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
        return customGravity != null ? customGravity : GRAVITY_NORMAL;
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
package com.minestom.mechanics.systems.misc.gravity;

import com.minestom.mechanics.config.constants.LegacyGravityConstants;
import com.minestom.mechanics.systems.damage.DamageFeature;
import com.minestom.mechanics.systems.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
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
            if (!(entity instanceof Player player)) return;

            Double customGravity = entity.getTag(GRAVITY);
            if (customGravity == null) {
                if (entity.hasNoGravity()) entity.setNoGravity(false);
                entity.removeTag(GRAVITY_LAST_POS);
                entity.removeTag(SMOOTHED_VY);
                return;
            }

            applyCustomGravity(player, customGravity);
        });
    }

    private void applyCustomGravity(Player player, double customGravity) {
        // Special case: if multiplier is 1.0, use vanilla gravity (don't send explosion packets)
        if (customGravity == 1.0) {
            if (player.hasNoGravity()) player.setNoGravity(false);
            player.removeTag(GRAVITY_LAST_POS);
            player.removeTag(SMOOTHED_VY);
            return;
        }

        // Special case: if multiplier is 0.0, use zero gravity (dynamic counteraction with feedback)
        // For zero gravity, we track actual velocity and adjust counteraction to maintain zero net velocity
        // This prevents slow rising/falling by creating a feedback loop
        if (customGravity == 0.0) {
            // Skip if conditions don't allow gravity
            if (player.isOnGround() || player.isFlying() || player.isFlyingWithElytra()) {
                player.removeTag(GRAVITY_LAST_POS);
                player.setTag(SMOOTHED_VY, 0.0);
                return;
            }
            
            Pos currentPos = player.getPosition();
            Pos lastPos = player.getTag(GRAVITY_LAST_POS);
            
            // Initialize on first tick - don't send explosion packet yet, just track position
            if (lastPos == null) {
                player.setTag(GRAVITY_LAST_POS, currentPos);
                player.setTag(SMOOTHED_VY, 0.0);
                return; // Wait for next tick to start counteraction
            }
            
            // Detect teleports (large position changes) and reset tracking
            double yChange = Math.abs(currentPos.y() - lastPos.y());
            double horizontalChange = Math.sqrt(
                    Math.pow(currentPos.x() - lastPos.x(), 2) + 
                    Math.pow(currentPos.z() - lastPos.z(), 2)
            );
            // If Y changed by more than 2 blocks OR horizontal changed by more than 10 blocks, it's likely a teleport
            if (yChange > 2.0 || horizontalChange > 10.0) {
                player.setTag(GRAVITY_LAST_POS, currentPos);
                player.setTag(SMOOTHED_VY, 0.0);
                return; // Reset and wait for next tick
            }
            
            // Calculate actual Y velocity from position change
            double actualVy = currentPos.y() - lastPos.y();
            
            // Get previous smoothed velocity for feedback
            Double prevSmoothedVy = player.getTag(SMOOTHED_VY);
            if (prevSmoothedVy == null) prevSmoothedVy = 0.0;
            
            // Light smoothing to reduce jitter but keep feedback responsive
            double smoothedVy = (actualVy * 0.9) + (prevSmoothedVy * 0.1);
            player.setTag(SMOOTHED_VY, smoothedVy);
            
            // Base counteraction: GRAVITY_NORMAL upward to counteract vanilla gravity
            double baseCounteraction = LegacyGravityConstants.GRAVITY_NORMAL * LegacyGravityConstants.EXPLOSION_VELOCITY_SCALE;
            
            // Feedback adjustment: if player is rising (positive velocity), reduce counteraction
            // If player is falling (negative velocity), increase counteraction
            // This creates a stabilizing feedback loop
            // Scale factor of 0.5 means we adjust by 50% of the velocity error
            double feedbackAdjustment = -smoothedVy * 0.5;
            double explosionVy = baseCounteraction + feedbackAdjustment;
            
            player.sendPacket(new ExplosionPacket(
                    player.getPosition(),
                    0f,  // Strength = 0 (no damage, minimal visual/sound effect)
                    0,   // Record count = 0 (no affected blocks = completely invisible)
                    new Vec(0, explosionVy, 0),  // Velocity vector (positive = upward to counteract)
                    Particle.BLOCK_MARKER,  // Particle type (minimal visibility)
                    SoundEvent.INTENTIONALLY_EMPTY,  // Silent (may not work for 1.8)
                    List.of()  // Empty affected blocks list
            ));
            
            // Update position tracking
            player.setTag(GRAVITY_LAST_POS, currentPos);
            return;
        }

        // Calculate actual gravity value from multiplier
        // customGravity is a multiplier: 0.0 = zero gravity, 0.2 = 20% of normal, 1.0 = 100% of normal (vanilla), 2.0 = 200% of normal
        double actualGravity = customGravity * LegacyGravityConstants.GRAVITY_NORMAL;
        
        // CRITICAL: Vanilla gravity always applies -GRAVITY_NORMAL per tick
        // We need to counteract it, then apply our custom gravity
        // Net gravity change needed: -actualGravity - (-GRAVITY_NORMAL) = GRAVITY_NORMAL - actualGravity
        // This is the velocity change we need to apply via explosion packet
        double gravityAdjustment = LegacyGravityConstants.GRAVITY_NORMAL - actualGravity;

        // Reset when conditions don't allow gravity
        if (player.isOnGround()) {
            player.setTag(SMOOTHED_VY, 0.0);
            player.setTag(GRAVITY_LAST_POS, player.getPosition());
            return;
        }
        if (player.isFlying()) {
            player.setTag(SMOOTHED_VY, 0.0);
            player.setTag(GRAVITY_LAST_POS, player.getPosition());
            return;
        }
        if (player.isFlyingWithElytra()) {
            player.setTag(SMOOTHED_VY, 0.0);
            player.setTag(GRAVITY_LAST_POS, player.getPosition());
            return;
        }

        // Note: We don't set noGravity(true) because this system emulates gravity for older clients
        // that don't have the gravity attribute. We work alongside vanilla gravity.
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
        // We calculate what the velocity should be after applying custom gravity
        // Since vanilla gravity applies -GRAVITY_NORMAL, and we want -actualGravity,
        // the net change from our explosion packet should be: GRAVITY_NORMAL - actualGravity
        // So: newVy = smoothedVy - GRAVITY_NORMAL + (GRAVITY_NORMAL - actualGravity) = smoothedVy - actualGravity
        double newVy = smoothedVy - actualGravity;

        // Apply terminal velocity cap (downward)
        // Terminal velocity scales directly with actual gravity value
        // TERMINAL_VELOCITY is positive (magnitude), so we negate it for downward direction
        if (LegacyGravityConstants.USE_TERMINAL_VELOCITY) {
            double scaledTerminalVelocity = -LegacyGravityConstants.TERMINAL_VELOCITY * actualGravity;
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
            log.debug("Player: {} | fallDist: {:.4f} | estimatedVy: {:.4f} | smoothedVy: {:.4f} | newVy: {:.4f} | gravityMult: {:.4f} | actualGravity: {:.4f}",
                    player.getUsername(), fallDistance, estimatedVy, smoothedVy, newVy, customGravity, actualGravity);
        }

        // Scale the velocity for explosion packet
        // CRITICAL: Explosion packets ADD velocity, and vanilla gravity applies -GRAVITY_NORMAL per tick
        // We need to counteract vanilla gravity and apply custom gravity
        // Net adjustment needed: GRAVITY_NORMAL - actualGravity
        //   - For 0.16 multiplier: GRAVITY_NORMAL - 0.16*GRAVITY_NORMAL = 0.84*GRAVITY_NORMAL (reduced downward)
        //   - For 2.0 multiplier: GRAVITY_NORMAL - 2.0*GRAVITY_NORMAL = -GRAVITY_NORMAL (extra downward)
        // Scale by EXPLOSION_VELOCITY_SCALE to convert gravity units to explosion velocity units
        double explosionVy = gravityAdjustment * LegacyGravityConstants.EXPLOSION_VELOCITY_SCALE;

        // ✅ FIXED: Apply gravity force directly, make explosion invisible/silent
        // Note: Explosions may make sound for 1.8 clients (protocol limitation)
        player.sendPacket(new ExplosionPacket(
                player.getPosition(),
                0f,  // Strength = 0 (no damage, minimal visual/sound effect)
                0,   // Record count = 0 (no affected blocks = completely invisible)
                new Vec(0, explosionVy, 0),  // Velocity vector (negative = downward)
                Particle.BLOCK_MARKER,  // Particle type (minimal visibility)
                SoundEvent.INTENTIONALLY_EMPTY,  // Silent (may not work for 1.8)
                List.of()  // Empty affected blocks list
        ));

        // Update position tracking
        player.setTag(GRAVITY_LAST_POS, currentPos);
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

    /**
     * Set custom gravity multiplier for an entity.
     * @param entity The entity to set gravity for
     * @param gravityMultiplier Gravity multiplier (0.2 = 20% of normal, 1.0 = 100% normal/vanilla, 2.0 = 200% of normal)
     *                          Special case: 1.0 uses vanilla gravity (no explosion packets)
     */
    public static void setGravity(Entity entity, double gravityMultiplier) {
        entity.setTag(GRAVITY, gravityMultiplier);
    }

    /**
     * Get the gravity multiplier for an entity.
     * @param entity The entity to get gravity for
     * @return The gravity multiplier (1.0 = normal/vanilla if not set)
     */
    public static double getGravity(Entity entity) {
        Double customGravity = entity.getTag(GRAVITY);
        return customGravity != null ? customGravity : 1.0;
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
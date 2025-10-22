package com.minestom.mechanics.util;

import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.minestom.server.network.packet.server.play.EntityTeleportPacket;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Gravity system that works for BOTH 1.8 and modern clients.
 *
 * <h2>How It Works</h2>
 *
 * <h3>Modern Clients (1.21+)</h3>
 * <p>Uses {@code Attribute.GENERIC_GRAVITY} - handled automatically by Minestom.</p>
 *
 * <h3>Legacy 1.8 Clients (ViaVersion)</h3>
 * <p>Manual gravity application via velocity manipulation each tick.
 * The attribute doesn't exist in 1.8, so ViaVersion can't translate it.</p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * // Initialize system
 * GravitySystem.initialize();
 *
 * // Set gravity (works for all client versions)
 * entity.setTag(GravitySystem.GRAVITY, 0.0);  // Floating
 * entity.setTag(GravitySystem.GRAVITY, 0.02); // Low gravity
 *
 * // For projectiles
 * bow.withTag(GravitySystem.GRAVITY, 0.0);  // Item-specific
 * </pre>
 *
 * <h2>Priority for Projectiles</h2>
 * <ol>
 *   <li>Projectile GRAVITY tag</li>
 *   <li>Item GRAVITY tag</li>
 *   <li>Shooter GRAVITY tag</li>
 *   <li>Registry config</li>
 *   <li>Default</li>
 * </ol>
 */
public class GravitySystem extends InitializableSystem {

    private static GravitySystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("GravitySystem");

    /**
     * Gravity tag for entities and items.
     * Works on both living entities and projectiles.
     */
    public static final Tag<Double> GRAVITY = Tag.Double("gravity");

    // Common gravity values
    public static final double GRAVITY_NORMAL = 0.08;      // Vanilla entity gravity
    public static final double GRAVITY_PROJECTILE = 0.05;  // Vanilla projectile
    public static final double GRAVITY_NONE = 0.0;         // Floating
    public static final double GRAVITY_LOW = 0.02;         // Moon
    public static final double GRAVITY_HIGH = 0.16;        // Heavy

    private GravitySystem() {}

    // ===========================
    // INITIALIZATION
    // ===========================

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
                return;
            }

            applyCustomGravity(livingEntity, customGravity);
        });
    }

    private static final Tag<Double> VY = Tag.Double("gravity_vy");

    private void applyCustomGravity(LivingEntity entity, double customGravity) {
        if (entity.isOnGround()) {
            entity.setTag(VY, 0.0);
            return;
        }
        if (entity instanceof Player p && p.isFlying()) {
            entity.setTag(VY, 0.0);
            return;
        }
        if (entity.isFlyingWithElytra()) {
            entity.setTag(VY, 0.0);
            return;
        }

        entity.setNoGravity(true);

        Double vyBoxed = entity.getTag(VY);
        double vy = (vyBoxed != null) ? vyBoxed : 0.0;

        if (vy <= 0) {
            vy -= customGravity;
        }

        entity.setTag(VY, vy);

        if (entity instanceof Player player) {
            var pos = player.getPosition();

            // Send packet with ONLY Y as relative
            player.sendPacket(new EntityTeleportPacket(
                    player.getEntityId(),
                    pos, // Current position (for X/Z reference)
                    new Vec(0, vy, 0), // Only Y delta
                    RelativeFlags.Y, // ONLY Y is relative, X/Z are absolute from pos
                    player.isOnGround()
            ));

            // Update server position
            entity.refreshPosition(pos.withY(pos.y() + vy));
        } else {
            var pos = entity.getPosition();
            entity.teleport(pos.withY(pos.y() + vy));
        }
    }

    // ===========================
    // PUBLIC API
    // ===========================

    /**
     * Set gravity for an entity.
     * Works for both 1.8 and modern clients.
     *
     * @param entity The entity
     * @param gravity Gravity value (0.08 = vanilla, 0.0 = floating)
     */
    public static void setGravity(Entity entity, double gravity) {
        entity.setTag(GRAVITY, gravity);
    }

    /**
     * Get gravity for an entity.
     * Returns custom gravity if set, otherwise vanilla default.
     *
     * @param entity The entity
     * @return gravity value
     */
    public static double getGravity(Entity entity) {
        Double customGravity = entity.getTag(GRAVITY);
        return customGravity != null ? customGravity : GRAVITY_NORMAL;
    }

    /**
     * Remove custom gravity from entity (revert to vanilla).
     *
     * @param entity The entity
     */
    public static void clearGravity(Entity entity) {
        entity.removeTag(GRAVITY);
    }

    /**
     * Check if entity has custom gravity set.
     *
     * @param entity The entity
     * @return true if custom gravity is set
     */
    public static boolean hasCustomGravity(Entity entity) {
        return entity.getTag(GRAVITY) != null;
    }

}
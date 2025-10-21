package com.minestom.mechanics.util;

import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.item.ItemStack;
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

    private static final Tag<Boolean> GRAVITY_CONTROLLED = Tag.Boolean("grav_controlled").defaultValue(false);

    private void registerListener() {
        MinecraftServer.getGlobalEventHandler().addListener(EntityTickEvent.class, event -> {
            Entity entity = event.getEntity();

            // Only handle living entities (not projectiles - they have custom physics)
            if (!(entity instanceof LivingEntity livingEntity)) {
                return;
            }

            // Check for custom gravity tag
            Double customGravity = entity.getTag(GRAVITY);

            if (customGravity == null) {
                // No custom gravity - restore vanilla
                if (livingEntity.getTag(GRAVITY_CONTROLLED)) {
                    livingEntity.setNoGravity(false);
                    livingEntity.setTag(GRAVITY_CONTROLLED, false);
                }
                return;
            }

            // Tell client to stop applying gravity
            if (!livingEntity.getTag(GRAVITY_CONTROLLED)) {
                livingEntity.setNoGravity(true);
                livingEntity.setTag(GRAVITY_CONTROLLED, true);
            }

            Vec velocity = livingEntity.getVelocity();

            // ONLY apply gravity when falling (Y <= 0), NOT when jumping (Y > 0)
            // This prevents cutting off jumps!
            if (!livingEntity.isOnGround() && velocity.y() <= 0) {
                applyCustomGravity(livingEntity, customGravity);
            }
        });
    }

    /**
     * Apply custom gravity ONLY to Y velocity.
     * Should NOT touch X or Z at all.
     */
    private void applyCustomGravity(LivingEntity entity, double customGravity) {
        // Don't apply if flying
        if (entity instanceof Player player && player.isFlying()) {
            return;
        }

        if (entity.isFlyingWithElytra()) {
            return;
        }

        Vec velocity = entity.getVelocity();

        // Explicitly preserve X and Z, only change Y
        double newY = velocity.y() - customGravity;

        // Use Vec constructor to be 100% explicit
        Vec newVelocity = new Vec(velocity.x(), newY, velocity.z());
        entity.setVelocity(newVelocity);

        // Debug X/Z changes
        if (entity.getAliveTicks() % 40 == 0) {
            log.debug("Gravity applied - Before: X={} Y={} Z={}, After: X={} Y={} Z={}",
                    velocity.x(), velocity.y(), velocity.z(),
                    newVelocity.x(), newVelocity.y(), newVelocity.z());
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

    // ===========================
    // PROJECTILE GRAVITY RESOLUTION
    // ===========================

    /**
     * Resolve gravity for a projectile with priority chain.
     *
     * <p><b>Priority:</b></p>
     * <ol>
     *   <li>Projectile GRAVITY tag (highest)</li>
     *   <li>Item GRAVITY tag</li>
     *   <li>Shooter GRAVITY tag</li>
     *   <li>Registry config gravity</li>
     *   <li>Default fallback</li>
     * </ol>
     *
     * @param projectile The projectile entity
     * @param shooter The shooting entity (can be null)
     * @param item The item used (can be null)
     * @param registryGravity Registry config gravity (can be null)
     * @param defaultGravity Default fallback value
     * @return resolved gravity value
     */
    public static double resolveProjectileGravity(Entity projectile,
                                                  @Nullable Entity shooter,
                                                  @Nullable ItemStack item,
                                                  @Nullable Double registryGravity,
                                                  double defaultGravity) {
        // 1. Projectile GRAVITY tag (highest priority)
        Double projectileGravity = projectile.getTag(GRAVITY);
        if (projectileGravity != null) {
            return projectileGravity;
        }

        // 2. Item GRAVITY tag
        if (item != null) {
            Double itemGravity = item.getTag(GRAVITY);
            if (itemGravity != null) {
                return itemGravity;
            }
        }

        // 3. Shooter GRAVITY tag (works for both 1.8 and modern!)
        if (shooter != null) {
            Double shooterGravity = shooter.getTag(GRAVITY);
            if (shooterGravity != null) {
                return shooterGravity;
            }
        }

        // 4. Registry config
        if (registryGravity != null) {
            return registryGravity;
        }

        // 5. Default fallback
        return defaultGravity;
    }

    /**
     * Simple projectile gravity resolution.
     *
     * @param projectile The projectile entity
     * @param registryGravity Registry config gravity (can be null)
     * @param defaultGravity Default fallback
     * @return resolved gravity value
     */
    public static double resolveProjectileGravity(Entity projectile,
                                                  @Nullable Double registryGravity,
                                                  double defaultGravity) {
        return resolveProjectileGravity(projectile, null, null, registryGravity, defaultGravity);
    }
}
package com.minestom.mechanics.systems.projectile.tags;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.Entity;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Central registry for projectile tag systems.
 * Systems register themselves during initialization, then tag copying is automatic.
 *
 * Usage:
 * 1. Systems register during init: ProjectileTagRegistry.register(KnockbackHandler.class)
 * 2. Projectile creation: ProjectileTagRegistry.copyAllProjectileTags(item, projectile)
 */
public class ProjectileTagRegistry {

    private static final Set<Class<?>> registeredSystems = new HashSet<>();
    private static final LogUtil.SystemLogger log = LogUtil.system("ProjectileTagRegistry");

    /**
     * Register a system to have its projectile tags automatically copied.
     * Call this during system initialization.
     *
     * @param systemClass The handler class (e.g., KnockbackHandler.class)
     */
    public static void register(Class<?> systemClass) {
        registeredSystems.add(systemClass);
        log.debug("Registered projectile tags from: {}", systemClass.getSimpleName());
    }

    /**
     * Copy ALL projectile tags from ALL registered systems.
     * This is the only method you need to call when creating projectiles!
     *
     * @param item Source item (bow, egg, snowball, etc.)
     * @param projectile Target projectile entity
     */
    public static void copyAllProjectileTags(ItemStack item, Entity projectile) {
        for (Class<?> systemClass : registeredSystems) {
            copyProjectileTagsFromSystem(item, projectile, systemClass);
        }
    }

    /**
     * Copy projectile tags from a specific system class.
     */
    private static void copyProjectileTagsFromSystem(ItemStack item, Entity projectile, Class<?> systemClass) {
        try {
            for (Field field : systemClass.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && Tag.class.isAssignableFrom(field.getType())
                        && field.getName().contains("PROJECTILE")) {

                    Tag<?> projectileTag = (Tag<?>) field.get(null);
                    Object value = item.getTag(projectileTag);
                    if (value != null) {
                        setTagUnsafe(projectile, projectileTag, value);
                        // ✅ ADD THIS
                        log.info("Copied {} = {} from {} to {}",
                                field.getName(), value, item.material(), projectile.getEntityType());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error copying projectile tags from {}: {}",
                    systemClass.getSimpleName(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void setTagUnsafe(Entity entity, Tag<T> tag, Object value) {
        entity.setTag(tag, (T) value);
    }

    /**
     * Get count of registered systems (for debugging)
     */
    public static int getRegisteredSystemCount() {
        return registeredSystems.size();
    }

    /**
     * Get names of registered systems (for debugging)
     */
    public static Set<String> getRegisteredSystemNames() {
        Set<String> names = new HashSet<>();
        for (Class<?> clazz : registeredSystems) {
            names.add(clazz.getSimpleName());
        }
        return names;
    }

    /**
     * Clear all registrations (mainly for testing)
     */
    public static void clearRegistrations() {
        registeredSystems.clear();
    }
}
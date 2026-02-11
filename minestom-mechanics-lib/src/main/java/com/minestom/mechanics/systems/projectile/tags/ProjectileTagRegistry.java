package com.minestom.mechanics.systems.projectile.tags;

import com.minestom.mechanics.systems.knockback.KnockbackSystem;
import com.minestom.mechanics.systems.knockback.tags.KnockbackTagValue;
import com.minestom.mechanics.systems.projectile.components.ProjectileVelocity;
import net.minestom.server.entity.Entity;
import net.minestom.server.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Copies item tags to projectile entities when projectiles are created.
 * Reads from per-system item tags (Tag.Structure), writes to entity transient tags.
 */
public class ProjectileTagRegistry {

    private static final Set<Class<?>> registeredSystems = new HashSet<>();

    public static void register(Class<?> systemClass) {
        registeredSystems.add(systemClass);
    }

    /**
     * Copy projectile-relevant data from item tags to entity transient tags.
     */
    public static void copyAllProjectileTags(ItemStack item, Entity projectile) {
        if (item == null || item.isAir()) return;

        // Knockback: item's ITEM_PROJECTILE_CUSTOM → entity's PROJECTILE_CUSTOM
        if (registeredSystems.contains(KnockbackSystem.class)) {
            KnockbackTagValue pkb = item.getTag(KnockbackSystem.ITEM_PROJECTILE_CUSTOM);
            if (pkb != null) {
                projectile.setTag(KnockbackSystem.PROJECTILE_CUSTOM, pkb);
            }
        }

        // Velocity: item's ITEM_CUSTOM → entity's CUSTOM
        if (registeredSystems.contains(ProjectileVelocity.class)) {
            VelocityTagValue vel = item.getTag(ProjectileVelocity.ITEM_CUSTOM);
            if (vel != null) {
                projectile.setTag(ProjectileVelocity.CUSTOM, vel);
            }
        }
    }

    public static int getRegisteredSystemCount() { return registeredSystems.size(); }

    public static Set<String> getRegisteredSystemNames() {
        Set<String> names = new HashSet<>();
        for (Class<?> clazz : registeredSystems) names.add(clazz.getSimpleName());
        return names;
    }

    public static void clearRegistrations() { registeredSystems.clear(); }
}

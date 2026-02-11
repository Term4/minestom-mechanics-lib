package com.minestom.mechanics.systems.health.damage.types;

import com.minestom.mechanics.systems.health.damage.DamageTracker;
import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.registry.RegistryKey;

/** Thrown projectile damage type (eggs, snowballs, ender pearls). Event-only. */
public final class miscProjectile extends DamageTracker {
    @Override public String id() { return "thrown"; }
    @Override public DamageTypeProperties defaultProperties() { return DamageTypeProperties.ATTACK_DEFAULT; }
    @Override public RegistryKey<?>[] matchedTypes() { return new RegistryKey<?>[]{ DamageType.THROWN }; }
    @Override public Object defaultConfig() { return null; }
    @Override public boolean isTickable() { return false; }
}

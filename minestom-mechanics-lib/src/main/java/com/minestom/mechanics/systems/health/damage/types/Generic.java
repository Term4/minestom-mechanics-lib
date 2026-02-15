package com.minestom.mechanics.systems.health.damage.types;

import com.minestom.mechanics.systems.health.damage.DamageTracker;
import com.minestom.mechanics.config.health.DamageTypeProperties;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.registry.RegistryKey;

/** Generic damage type (fishing bobber, misc). Event-only. */
public final class Generic extends DamageTracker {
    @Override public String id() { return "generic"; }
    @Override public DamageTypeProperties defaultProperties() { return DamageTypeProperties.ATTACK_DEFAULT; }
    @Override public RegistryKey<?>[] matchedTypes() { return new RegistryKey<?>[]{ DamageType.GENERIC }; }
    @Override public Object defaultConfig() { return null; }
    @Override public boolean isTickable() { return false; }
}

package com.minestom.mechanics.systems.health.damage.types;

import com.minestom.mechanics.systems.health.damage.DamageTracker;
import com.minestom.mechanics.config.health.DamageTypeProperties;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.registry.RegistryKey;

/** Melee (player attack) damage type. Event-only, no tick tracking. */
public final class Melee extends DamageTracker {
    @Override public String id() { return "melee"; }
    @Override public DamageTypeProperties defaultProperties() { return DamageTypeProperties.ATTACK_DEFAULT; }
    @Override public RegistryKey<?>[] matchedTypes() { return new RegistryKey<?>[]{ DamageType.PLAYER_ATTACK }; }
    @Override public Object defaultConfig() { return null; }
    @Override public boolean isTickable() { return false; }
}

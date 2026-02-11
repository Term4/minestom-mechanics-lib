package com.minestom.mechanics.systems.health.damage;

import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.registry.RegistryKey;

/**
 * Base class for damage type definitions with optional tick-based tracking.
 * Each subclass defines a damage type (id, properties, config) and optionally
 * implements {@link #tick} for environmental damage detection.
 *
 * <p>Subclasses get access to their {@link DamageType} via {@code damageType},
 * which is set automatically when registered via {@link DamageType#register}.</p>
 */
public abstract class DamageTracker {

    /** The DamageType this tracker belongs to. Set by {@link DamageType#register}. */
    protected DamageType damageType;

    // ===========================
    // DEFINITION (required)
    // ===========================

    /** Unique identifier (e.g. "fall", "fire", "cactus"). */
    public abstract String id();

    /** Default properties for this damage type. */
    public abstract DamageTypeProperties defaultProperties();

    /** Minecraft registry keys this damage type matches. */
    public abstract RegistryKey<?>[] matchedTypes();

    /** Default configuration for this damage type. */
    public abstract Object defaultConfig();

    /** Whether this tracker needs per-tick processing. Override to return true. */
    public boolean isTickable() { return true; }

    // ===========================
    // TRACKING (optional overrides)
    // ===========================

    /** Called every player tick. Override for environmental damage detection. */
    public void tick(Player player, long currentTick) {}

    /** Called when a player dies. */
    public void onPlayerDeath(Player player) {}

    /** Called when a player spawns. */
    public void onPlayerSpawn(Player player) {}

    /** Called when an entity is cleaned up (e.g. disconnect). */
    public void cleanup(LivingEntity entity) {}

    /** Get fall distance (only meaningful for fall tracker). */
    public double getFallDistance(Player player) { return 0; }

    /** Reset fall distance (only meaningful for fall tracker). */
    public void resetFallDistance(Player player) {}
}

package com.minestom.mechanics.systems.health.damage;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A damage type with configurable properties and runtime tag overrides.
 * Also serves as the static registry for all registered damage types.
 *
 * <p>Each damage type maps to one or more Minecraft {@code DamageType} registry keys
 * and carries a set of {@link DamageTypeProperties} that can be overridden
 * per item/entity/world via {@link DamageOverride} tags.</p>
 *
 * <p>Property resolution priority: Item > Attacker > Victim > World > Defaults</p>
 */
public class DamageType {

    // ===========================
    // REGISTRY (static)
    // ===========================

    private static final Map<String, DamageType> byId = new ConcurrentHashMap<>();
    private static final Map<String, DamageTracker> trackersById = new ConcurrentHashMap<>();
    private static final List<DamageType> types = new ArrayList<>();
    private static final List<DamageTracker> trackers = new ArrayList<>();
    private static final Map<RegistryKey<?>, DamageType> byRegistryKey = new HashMap<>();

    /** Register a damage tracker (defines + optionally tracks a damage type). */
    public static void register(DamageTracker tracker) {
        String id = tracker.id();
        if (byId.containsKey(id)) {
            throw new IllegalArgumentException("Damage type already registered: " + id);
        }
        DamageType type = new DamageType(id, tracker.defaultProperties(), tracker.matchedTypes());
        type.setConfig(tracker.defaultConfig());
        tracker.damageType = type;

        byId.put(id, type);
        types.add(type);
        for (RegistryKey<?> key : tracker.matchedTypes()) {
            byRegistryKey.put(key, type);
        }
        if (tracker.isTickable()) {
            trackers.add(tracker);
        }
        trackersById.put(id, tracker);
    }

    /** Get damage type by id. */
    public static DamageType get(String id) { return byId.get(id); }

    /** Find damage type by Minecraft registry key. */
    public static DamageType find(RegistryKey<?> type) { return byRegistryKey.get(type); }

    /** Get all registered damage types. */
    public static List<DamageType> getAll() { return List.copyOf(types); }

    /** Get all tickable trackers. */
    public static List<DamageTracker> getTrackers() { return List.copyOf(trackers); }

    /** Get tracker by id, or null. */
    public static DamageTracker getTracker(String id) { return trackersById.get(id); }

    /** Get override tag by id. */
    public static Tag<DamageOverride> getTag(String id) {
        DamageType dt = byId.get(id);
        return dt != null ? dt.getTag() : null;
    }

    /** Clear all registered types. Called on shutdown. */
    public static void clearRegistry() {
        byId.clear();
        trackersById.clear();
        types.clear();
        trackers.clear();
        byRegistryKey.clear();
    }

    // ===========================
    // INSTANCE
    // ===========================

    private final String name;
    private DamageTypeProperties defaults;
    private Object config;
    private final Tag<DamageOverride> tag;
    private final Set<RegistryKey<?>> matchedTypes;

    @SafeVarargs
    public DamageType(String name, DamageTypeProperties defaults, RegistryKey<?>... matchedTypes) {
        this.name = name;
        this.defaults = defaults;
        this.tag = Tag.Transient("health_" + name.toLowerCase());
        this.matchedTypes = Set.of(matchedTypes);
    }

    // ===========================
    // MATCHING
    // ===========================

    public boolean matches(RegistryKey<?> type) { return matchedTypes.contains(type); }
    public Set<RegistryKey<?>> getMatchedTypes() { return Collections.unmodifiableSet(matchedTypes); }

    // ===========================
    // PROPERTY RESOLUTION
    // ===========================

    /**
     * Resolve effective properties for a damage context.
     * Priority: Item > Attacker > Victim > World > Defaults.
     */
    public DamageTypeProperties resolveProperties(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        DamageOverride override;

        if (item != null && !item.isAir()) {
            override = item.getTag(tag);
            if (override != null && override.custom() != null) return override.custom();
        }
        if (attacker != null) {
            override = attacker.getTag(tag);
            if (override != null && override.custom() != null) return override.custom();
        }
        override = victim.getTag(tag);
        if (override != null && override.custom() != null) return override.custom();
        if (victim.getInstance() != null) {
            override = victim.getInstance().getTag(tag);
            if (override != null && override.custom() != null) return override.custom();
        }
        return defaults;
    }

    // ===========================
    // CONFIG RESOLUTION
    // ===========================

    /**
     * Resolve effective config for a damage context.
     * Checks tag chain (item > attacker > victim > world) for configOverride,
     * falls back to this type's default config.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolveConfig(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        DamageOverride override;
        if (item != null && !item.isAir()) {
            override = item.getTag(tag);
            if (override != null && override.configOverride() != null) return (T) override.configOverride();
        }
        if (attacker != null) {
            override = attacker.getTag(tag);
            if (override != null && override.configOverride() != null) return (T) override.configOverride();
        }
        override = victim.getTag(tag);
        if (override != null && override.configOverride() != null) return (T) override.configOverride();
        if (victim.getInstance() != null) {
            override = victim.getInstance().getTag(tag);
            if (override != null && override.configOverride() != null) return (T) override.configOverride();
        }
        return (T) config;
    }

    /**
     * Resolve config for environmental damage (no attacker/item context).
     */
    @SuppressWarnings("unchecked")
    public <T> T resolveConfig(LivingEntity victim) {
        return resolveConfig(null, victim, null);
    }

    // ===========================
    // DAMAGE CALCULATION
    // ===========================

    /**
     * Calculate final damage with all modifiers applied.
     * Formula: max(0, (baseDamage * props.multiplier * stackedMultipliers) + stackedModifiers)
     */
    public float calculateDamage(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item, float baseDamage) {
        DamageTypeProperties props = resolveProperties(attacker, victim, item);
        if (!props.enabled()) return 0f;

        double stackedMult = getStackedMultiplier(attacker, victim, item);
        double stackedMod = getStackedModify(attacker, victim, item);

        float damage = (float) ((baseDamage * props.multiplier() * stackedMult) + stackedMod);
        return Math.max(0f, damage);
    }

    public boolean isEnabled(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        return resolveProperties(attacker, victim, item).enabled();
    }

    public boolean isEnabled(LivingEntity victim) {
        return isEnabled(null, victim, null);
    }

    // ===========================
    // EVENT PROCESSING
    // ===========================

    /**
     * Apply damage modifiers (multiplier/modify from tag chain) to an EntityDamageEvent.
     * Called AFTER DamageApplicator has handled i-frames/replacement.
     */
    public void processDamage(EntityDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getDamage().getSource();
        ItemStack item = (attacker instanceof Player p) ? p.getItemInMainHand() : null;

        DamageTypeProperties props = resolveProperties(attacker, victim, item);
        if (!props.enabled()) {
            event.setCancelled(true);
            return;
        }

        float original = event.getDamage().getAmount();
        float modified = calculateDamage(attacker, victim, item, original);
        if (modified != original) {
            event.getDamage().setAmount(modified);
        }
    }

    // ===========================
    // MULTIPLIER / MODIFY STACKING
    // ===========================

    private double getStackedMultiplier(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        double result = 1.0;
        DamageOverride override;
        if (item != null && !item.isAir()) {
            override = item.getTag(tag);
            if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        }
        if (attacker != null) {
            override = attacker.getTag(tag);
            if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        }
        override = victim.getTag(tag);
        if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        if (victim.getInstance() != null) {
            override = victim.getInstance().getTag(tag);
            if (override != null && override.multiplier() != null) for (double m : override.multiplier()) result *= m;
        }
        return result;
    }

    private double getStackedModify(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        double result = 0.0;
        DamageOverride override;
        if (item != null && !item.isAir()) {
            override = item.getTag(tag);
            if (override != null && override.modify() != null) for (double m : override.modify()) result += m;
        }
        if (attacker != null) {
            override = attacker.getTag(tag);
            if (override != null && override.modify() != null) for (double m : override.modify()) result += m;
        }
        override = victim.getTag(tag);
        if (override != null && override.modify() != null) for (double m : override.modify()) result += m;
        if (victim.getInstance() != null) {
            override = victim.getInstance().getTag(tag);
            if (override != null && override.modify() != null) for (double m : override.modify()) result += m;
        }
        return result;
    }

    // ===========================
    // GETTERS / SETTERS
    // ===========================

    public String getName() { return name; }
    public DamageTypeProperties getDefaults() { return defaults; }
    public Tag<DamageOverride> getTag() { return tag; }
    public void setDefaults(DamageTypeProperties defaults) { this.defaults = defaults; }

    @SuppressWarnings("unchecked")
    public <T> T getConfig() { return (T) config; }
    public void setConfig(Object config) { this.config = config; }

    public void cleanup(LivingEntity entity) { entity.removeTag(tag); }
}

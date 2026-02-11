package com.minestom.mechanics.systems.health.damage;

import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.manager.ArmorManager;
import com.minestom.mechanics.manager.MechanicsManager;
import com.minestom.mechanics.systems.health.InvulnerabilityTracker;
import com.minestom.mechanics.systems.health.damage.util.DamageCalculator;
import com.minestom.mechanics.systems.health.damage.util.DamageOverride;
import com.minestom.mechanics.systems.health.damage.util.DamageOverrideSerializer;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
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

    /** Get entity/world override tag by id (transient). */
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
    /** Transient tag for entities/worlds. */
    private final Tag<DamageOverride> entityTag;
    /** Serialized tag for items. */
    private final Tag<DamageOverride> itemTag;
    private final Set<RegistryKey<?>> matchedTypes;

    @SafeVarargs
    public DamageType(String name, DamageTypeProperties defaults, RegistryKey<?>... matchedTypes) {
        this.name = name;
        this.defaults = defaults;
        this.entityTag = Tag.Transient("health_" + name.toLowerCase());
        this.itemTag = Tag.Structure("health_" + name.toLowerCase(), new DamageOverrideSerializer());
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
     * Priority: Item > Source > Shooter > Victim > World > Defaults.
     *
     * @param source  direct damage source (attacker for melee, projectile for ranged)
     * @param shooter indirect attacker (player who shot the projectile, or null for melee)
     * @param victim  the entity being damaged
     * @param item    the item used (weapon/projectile item from shooter's hand)
     */
    public DamageTypeProperties resolveProperties(@Nullable Entity source, @Nullable Entity shooter,
                                                   LivingEntity victim, @Nullable ItemStack item) {
        DamageOverride override;

        // 1. Item (highest priority) — serialized tag
        if (item != null && !item.isAir()) {
            override = item.getTag(itemTag);
            if (override != null && override.custom() != null) return override.custom();
        }
        // 2. Source entity (attacker or projectile) — uses transient tag
        if (source != null) {
            override = source.getTag(entityTag);
            if (override != null && override.custom() != null) return override.custom();
        }
        // 3. Shooter (indirect attacker, e.g. player who shot arrow) — uses transient tag
        if (shooter != null && shooter != source) {
            override = shooter.getTag(entityTag);
            if (override != null && override.custom() != null) return override.custom();
        }
        // 4. Victim
        override = victim.getTag(entityTag);
        if (override != null && override.custom() != null) return override.custom();
        // 5. World
        if (victim.getInstance() != null) {
            override = victim.getInstance().getTag(entityTag);
            if (override != null && override.custom() != null) return override.custom();
        }
        return defaults;
    }

    /** Convenience: resolve with a single attacker (melee / environmental). */
    public DamageTypeProperties resolveProperties(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        return resolveProperties(attacker, null, victim, item);
    }

    // ===========================
    // CONFIG RESOLUTION
    // ===========================

    /**
     * Resolve effective config for a damage context.
     * Checks tag chain for configOverride, falls back to this type's default config.
     * Note: configOverride is NOT serialized on items (only works on entity/world transient tags).
     */
    @SuppressWarnings("unchecked")
    public <T> T resolveConfig(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        DamageOverride override;
        if (attacker != null) {
            override = attacker.getTag(entityTag);
            if (override != null && override.configOverride() != null) return (T) override.configOverride();
        }
        override = victim.getTag(entityTag);
        if (override != null && override.configOverride() != null) return (T) override.configOverride();
        if (victim.getInstance() != null) {
            override = victim.getInstance().getTag(entityTag);
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

    /** Calculate final damage with all modifiers (delegates to {@link DamageCalculator}). */
    public float calculateDamage(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item, float baseDamage) {
        return DamageCalculator.calculate(resolveProperties(attacker, victim, item), itemTag, entityTag, attacker, victim, item, baseDamage);
    }

    public boolean isEnabled(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        return resolveProperties(attacker, victim, item).enabled();
    }

    public boolean isEnabled(LivingEntity victim) {
        return isEnabled(null, victim, null);
    }

    // ===========================
    // DAMAGE PIPELINE
    // ===========================

    private static final LogUtil.SystemLogger log = LogUtil.system("DamageType");

    /**
     * Full damage pipeline. Handles creative check, i-frames, replacement,
     * multiplier/modify stacking, and armor reduction — all in one pass.
     * Returns a {@link DamageResult} with full context for knockback etc.
     */
    public DamageResult processDamage(EntityDamageEvent event, InvulnerabilityTracker invulnerability, HealthConfig config) {
        LivingEntity victim = (LivingEntity) event.getEntity();
        Entity source = event.getDamage().getSource();    // direct: attacker (melee) or projectile (ranged)
        Entity shooter = event.getDamage().getAttacker();  // indirect: player who shot projectile, or null
        float damageAmount = event.getDamage().getAmount();
        RegistryKey<net.minestom.server.entity.damage.DamageType> mcType = event.getDamage().getType();

        // Resolve the player and their held item
        // For melee: source = player, shooter = null → item from source
        // For projectiles: source = projectile, shooter = player → item from shooter
        Player attackerPlayer = null;
        if (shooter instanceof Player p) attackerPlayer = p;
        else if (source instanceof Player p) attackerPlayer = p;
        ItemStack item = attackerPlayer != null ? attackerPlayer.getItemInMainHand() : null;
        Entity attacker = attackerPlayer != null ? attackerPlayer : source;

        DamageTypeProperties props = resolveProperties(source, shooter, victim, item);

        // 1. Disabled check
        if (!props.enabled()) {
            event.setCancelled(true);
            return DamageResult.blocked(props, attacker, victim);
        }

        // 2. Creative mode check
        if (victim instanceof Player player && player.getGameMode() == GameMode.CREATIVE) {
            if (!props.bypassCreative()) {
                event.setCancelled(true);
                return DamageResult.blocked(props, attacker, victim);
            }
        }

        // 3. Apply multiplier/modify stacking to damage amount
        float modified = calculateDamage(attacker, victim, item, damageAmount);
        if (modified != damageAmount) {
            event.getDamage().setAmount(modified);
            damageAmount = modified;
        }

        // 4. Bypass invulnerability: allow damage, skip i-frame check
        if (props.bypassInvulnerability()) {
            invulnerability.markDamaged(victim, damageAmount);
            invulnerability.setLastDamageReplacement(victim, false);
            return new DamageResult(true, false, damageAmount, props, attacker, victim);
        }

        // 5. Not in i-frames: allow damage normally
        if (!invulnerability.isInvulnerable(victim)) {
            invulnerability.markDamaged(victim, damageAmount);
            invulnerability.setLastDamageReplacement(victim, false);
            return new DamageResult(true, false, damageAmount, props, attacker, victim);
        }

        // 6. In i-frames: check if this damage type supports replacement
        if (!props.damageReplacement()) {
            event.setCancelled(true);
            return DamageResult.blocked(props, attacker, victim);
        }

        // 7. Replacement: only if incoming damage > previous damage
        float previousDamage = invulnerability.getLastDamageAmount(victim);
        if (damageAmount <= previousDamage) {
            event.setCancelled(true);
            return DamageResult.blocked(props, attacker, victim);
        }

        // 8. Apply replacement damage (difference only)
        event.setCancelled(true);
        float damageDifference = damageAmount - previousDamage;
        float finalDifference = damageDifference;

        // Apply armor reduction if damage doesn't penetrate
        if (!props.penetratesArmor() && victim instanceof Player player) {
            try {
                ArmorManager armorManager = MechanicsManager.getInstance().getArmorManager();
                if (armorManager != null && armorManager.isInitialized() && armorManager.isEnabled()) {
                    finalDifference = armorManager.calculateReducedDamage(player, damageDifference, mcType);
                }
            } catch (IllegalStateException ignored) {}
        }

        float newHealth = Math.max(0, victim.getHealth() - finalDifference);
        victim.setHealth(newHealth);

        invulnerability.updateDamageAmount(victim, damageAmount);
        invulnerability.setLastDamageReplacement(victim, true);

        return new DamageResult(true, true, finalDifference, props, attacker, victim);
    }

    /**
     * Process damage for an unregistered damage type (falls back to ATTACK_DEFAULT properties).
     */
    public static DamageResult processUnregistered(EntityDamageEvent event, InvulnerabilityTracker invulnerability, HealthConfig config) {
        // Create a temporary DamageType with attack defaults for the pipeline
        DamageType fallback = new DamageType("_unregistered", DamageTypeProperties.ATTACK_DEFAULT);
        return fallback.processDamage(event, invulnerability, config);
    }

    // ===========================
    // GETTERS / SETTERS
    // ===========================

    public String getName() { return name; }
    public DamageTypeProperties getDefaults() { return defaults; }
    /** Transient tag for entities/worlds. */
    public Tag<DamageOverride> getTag() { return entityTag; }
    /** Serialized tag for items. */
    public Tag<DamageOverride> getItemTag() { return itemTag; }
    public void setDefaults(DamageTypeProperties defaults) { this.defaults = defaults; }

    @SuppressWarnings("unchecked")
    public <T> T getConfig() { return (T) config; }
    public void setConfig(Object config) { this.config = config; }

    public void cleanup(LivingEntity entity) {
        entity.removeTag(entityTag);
    }
}

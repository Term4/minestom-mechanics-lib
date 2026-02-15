package com.minestom.mechanics.systems.health.damage;

import com.minestom.mechanics.config.health.DamageTypeProperties;
import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.config.timing.TickScaler;
import com.minestom.mechanics.config.timing.TickScalingConfig;
import com.minestom.mechanics.manager.ArmorManager;
import com.minestom.mechanics.manager.MechanicsManager;
import com.minestom.mechanics.systems.blocking.BlockingSystem;
import com.minestom.mechanics.systems.health.events.BlockingDamageEvent;
import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.systems.health.InvulnerabilityTracker;
import com.minestom.mechanics.systems.health.damage.util.DamageCalculator;
import com.minestom.mechanics.systems.health.damage.util.DamageOverride;
import com.minestom.mechanics.systems.health.damage.util.DamageOverrideSerializer;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
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
        return DamageCalculator.calculate(resolveProperties(attacker, victim, item), name, itemTag, entityTag, attacker, victim, item, baseDamage);
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

        // Extract shooter origin for knockback direction (projectiles store this at launch time)
        net.minestom.server.coordinate.Pos shooterOriginPos = null;
        if (source instanceof com.minestom.mechanics.systems.projectile.entities.CustomEntityProjectile proj) {
            shooterOriginPos = proj.getShooterOriginPos();
        }

        DamageTypeProperties props = resolveProperties(source, shooter, victim, item);

        // 1. Disabled check
        if (!props.enabled()) {
            event.setCancelled(true);
            return DamageResult.blocked(props, source, attacker, victim);
        }

        // 2. Creative mode check
        if (victim instanceof Player player && player.getGameMode() == GameMode.CREATIVE) {
            if (!props.bypassCreative()) {
                event.setCancelled(true);
                return DamageResult.blocked(props, source, attacker, victim);
            }
        }

        // 3. Apply multiplier/modify stacking to damage amount
        float modified = calculateDamage(attacker, victim, item, damageAmount);
        if (modified != damageAmount) {
            event.getDamage().setAmount(modified);
            damageAmount = modified;
        }

        // 3b. Apply blocking reduction (must run after damage is calculated)
        if (victim instanceof Player victimPlayer && props.blockable()) {
            try {
                BlockingSystem blocking = BlockingSystem.getInstance();
                HealthSystem hs = HealthSystem.getInstance();
                if (blocking.isEnabled() && blocking.isBlocking(victimPlayer) && hs.isBlockingApplicable(mcType, victimPlayer)) {
                    float originalAmount = damageAmount;
                    double reduction = blocking.getDamageReduction(victimPlayer);
                    damageAmount = (float) (originalAmount * (1.0 - reduction));
                    event.getDamage().setAmount(damageAmount);
                    if (originalAmount > damageAmount) {
                        MinecraftServer.getGlobalEventHandler().call(new BlockingDamageEvent(victimPlayer, originalAmount, damageAmount, mcType));
                    }
                }
            } catch (IllegalStateException ignored) {}
        }

        // 4. Bypass invulnerability: allow damage, skip i-frame check
        //    hurtEffect only applies to replacement; normal damage always flows through so viewers see red flash.
        //    Global override (VICTIM_HURT_EFFECT_OVERRIDE=false) can force silent for this player.
        if (props.bypassInvulnerability()) {
            if (HealthSystem.hasVictimHurtEffectOverride(victim) && victim instanceof Player player) {
                event.setCancelled(true);
                float newHealth = Math.max(0, player.getHealth() - damageAmount);
                if (newHealth <= 0) {
                    player.kill();
                } else {
                    HealthSystem.setHealthWithoutHurtEffect(player, newHealth);
                }
                invulnerability.markDamaged(victim, damageAmount, item);
                invulnerability.setLastDamageReplacement(victim, false);
                return new DamageResult(true, false, damageAmount, props, source, attacker, victim, shooterOriginPos);
            }
            invulnerability.markDamaged(victim, damageAmount, item);
            invulnerability.setLastDamageReplacement(victim, false);
            return new DamageResult(true, false, damageAmount, props, source, attacker, victim, shooterOriginPos);
        }

        // 5. Not in i-frames: allow damage normally (event flows through → viewers see red flash)
        //    hurtEffect only applies to replacement. Global override can force silent.
        if (!invulnerability.isInvulnerable(victim)) {
            if (HealthSystem.hasVictimHurtEffectOverride(victim) && victim instanceof Player player) {
                event.setCancelled(true);
                float newHealth = Math.max(0, player.getHealth() - damageAmount);
                if (newHealth <= 0) {
                    player.kill();
                } else {
                    HealthSystem.setHealthWithoutHurtEffect(player, newHealth);
                }
                invulnerability.markDamaged(victim, damageAmount, item);
                invulnerability.setLastDamageReplacement(victim, false);
                return new DamageResult(true, false, damageAmount, props, source, attacker, victim, shooterOriginPos);
            }
            invulnerability.markDamaged(victim, damageAmount, item);
            invulnerability.setLastDamageReplacement(victim, false);
            return new DamageResult(true, false, damageAmount, props, source, attacker, victim, shooterOriginPos);
        }

        // 5b. In i-frames: check invulnerability buffer (schedule hit for when invuln ends)
        if (props.invulnerabilityBufferTicks() > 0 && config.isInvulnerabilityEnabled()) {
            long ticksSince = invulnerability.getTicksSinceLastDamage(victim);
            if (ticksSince >= 0) {
                int scaledInvuln = TickScaler.scale(config.invulnerabilityTicks(), TickScalingConfig.getMode());
                int scaledBuffer = TickScaler.scale(props.invulnerabilityBufferTicks(), TickScalingConfig.getMode());
                long ticksRemaining = scaledInvuln - ticksSince;
                if (ticksRemaining <= scaledBuffer) {
                    HealthSystem hs = HealthSystem.getInstance();
                    if (!hs.hasBufferedHit(victim)) {
                        long lastDamageTick = invulnerability.getLastDamageTick(victim);
                        long applyAtTick = lastDamageTick + scaledInvuln;
                        var dmg = event.getDamage();
                        Entity dmgSource = dmg.getSource();
                        net.minestom.server.coordinate.Point pos = dmg.getSourcePosition();
                        if (pos == null && dmgSource != null) pos = dmgSource.getPosition();
                        if (pos == null) pos = victim.getPosition();
                        net.minestom.server.entity.damage.Damage snap = new net.minestom.server.entity.damage.Damage(
                                dmg.getType(), dmg.getSource(), dmg.getAttacker(), pos, dmg.getAmount());
                        boolean wasSprinting = attacker instanceof Player p && p.isSprinting();
                        if (hs.scheduleBufferedDamage(victim, snap, applyAtTick, wasSprinting)) {
                            event.setCancelled(true);
                            return DamageResult.blocked(props, source, attacker, victim);
                        }
                    }
                    event.setCancelled(true);
                    return DamageResult.blocked(props, source, attacker, victim);
                }
            }
        }

        // 6. In i-frames: check if this damage type supports replacement
        if (!props.damageReplacement()) {
            event.setCancelled(true);
            return DamageResult.blocked(props, source, attacker, victim);
        }

        // 6b. Same-item exclusion (Minemen): no replacement if replacement hit uses same item as initial
        if (props.noReplacementSameItem() && DamageTypeProperties.isSameItem(item, invulnerability.getLastMeleeItem(victim))) {
            event.setCancelled(true);
            return DamageResult.blocked(props, source, attacker, victim);
        }

        // 7. Replacement: only if incoming damage >= previous + cutoff
        float previousDamage = invulnerability.getLastDamageAmount(victim);
        float cutoff = props.replacementCutoff();
        if (damageAmount < previousDamage + cutoff) {
            event.setCancelled(true);
            return DamageResult.blocked(props, source, attacker, victim);
        }

        // 8. Apply replacement damage (difference only) — cancel event, update health directly.
        boolean effectiveHurtEffect = HealthSystem.shouldApplyHurtEffect(victim, props);
        event.setAnimation(effectiveHurtEffect);
        event.setCancelled(true);
        float damageDifference = damageAmount - previousDamage;
        float finalDifference = damageDifference;

        if (!props.penetratesArmor() && victim instanceof Player player) {
            try {
                ArmorManager armorManager = MechanicsManager.getInstance().getArmorManager();
                if (armorManager != null && armorManager.isInitialized() && armorManager.isEnabled()) {
                    finalDifference = armorManager.calculateReducedDamage(player, damageDifference, mcType);
                }
            } catch (IllegalStateException ignored) {}
        }

        float newHealth = Math.max(0, victim.getHealth() - finalDifference);
        if (!effectiveHurtEffect && victim instanceof Player player && newHealth > 0) {
            HealthSystem.setHealthWithoutHurtEffect(player, newHealth);
        } else {
            victim.setHealth(newHealth);
        }

        invulnerability.updateDamageAmount(victim, damageAmount);
        invulnerability.setLastDamageReplacement(victim, true);

        // Replacement supersedes any buffered hit — clear it so buffer does not fire later
        HealthSystem.getInstance().clearBufferedHit(victim);

        return new DamageResult(true, true, finalDifference, props, source, attacker, victim, shooterOriginPos);
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

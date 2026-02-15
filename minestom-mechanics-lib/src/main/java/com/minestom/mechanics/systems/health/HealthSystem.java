package com.minestom.mechanics.systems.health;

import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.manager.ArmorManager;
import com.minestom.mechanics.manager.MechanicsManager;
import com.minestom.mechanics.systems.blocking.BlockingSystem;
import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import com.minestom.mechanics.systems.compatibility.legacy_1_8.fix.LegacyHurtSuppression;
import com.minestom.mechanics.config.health.DamageTypeProperties;
import com.minestom.mechanics.config.timing.TickScaler;
import com.minestom.mechanics.config.timing.TickScalingConfig;
import com.minestom.mechanics.systems.health.damage.*;
import com.minestom.mechanics.systems.health.events.BlockingDamageEvent;
import net.minestom.server.item.ItemStack;
import com.minestom.mechanics.systems.health.damage.types.*;
import com.minestom.mechanics.systems.health.damage.util.DamageOverride;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main health system orchestrator.
 * Manages damage types, invulnerability tracking, and environmental damage detection.
 *
 * <p>Usage:</p>
 * <pre>
 * HealthSystem system = HealthSystem.initialize(HealthPresets.MINEMEN);
 * player.setTag(HealthSystem.tag("fire"), DamageOverride.mult(0.5));
 * world.setTag(HealthSystem.tag("fall"), DamageOverride.DISABLED);
 * </pre>
 */
public class HealthSystem extends InitializableSystem {
    private static HealthSystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("HealthSystem");

    private final InvulnerabilityTracker invulnerability;
    private HealthConfig config;
    private long currentTick = 0;
    private final Map<UUID, Long> lastProcessedTick = new ConcurrentHashMap<>();
    private final Map<UUID, BufferedDamageEntry> invulnerabilityBuffer = new ConcurrentHashMap<>();

    /** ThreadLocal: when set, applyKnockbackFromResult uses this for wasSprinting (buffered hits). Cleared after use. */
    private static final ThreadLocal<Boolean> BUFFERED_WAS_SPRINTING = new ThreadLocal<>();

    private record BufferedDamageEntry(long applyAtTick, Damage damage, boolean wasSprinting) {}
    private final List<AttackLandedListener> attackLandedListeners = new CopyOnWriteArrayList<>();
    private EventNode<Event> eventNode;
    private Task tickCounterTask;

    /** Called when attacker (Player) lands damage on victim. Used for swing-window hit tracking. */
    public interface AttackLandedListener {
        void onAttackLanded(Player attacker, LivingEntity victim, long tick);
    }

    public void addAttackLandedListener(AttackLandedListener listener) {
        attackLandedListeners.add(listener);
    }

    private HealthSystem(HealthConfig config) {
        this.config = config;
        this.invulnerability = new InvulnerabilityTracker(config);

        // Register built-in damage types
        // Environmental (tick-based)
        DamageType.register(new Fall());
        DamageType.register(new Fire());
        DamageType.register(new Cactus());
        // Combat (event-only — needed for tag resolution on items/entities)
        DamageType.register(new Melee());
        DamageType.register(new Arrow());
        DamageType.register(new miscProjectile());
        DamageType.register(new Generic());
    }

    /** Get the entity/world override tag for a damage type (transient). For player.setTag / world.setTag. */
    public static Tag<DamageOverride> tag(String id) {
        return DamageType.getTag(id);
    }

    /**
     * When set to {@code Boolean.FALSE} on a player, disables hurt effect (camera shake, red flash)
     * for ALL damage types for that player. Disabled by default.
     * <p>To turn ON (disable hurt for this player): {@code player.setTag(HealthSystem.VICTIM_HURT_EFFECT_OVERRIDE, false);}
     * <p>To turn OFF (normal hurt): {@code player.removeTag(HealthSystem.VICTIM_HURT_EFFECT_OVERRIDE);}
     */
    public static final Tag<Boolean> VICTIM_HURT_EFFECT_OVERRIDE = Tag.Transient("health_victim_no_hurt");

    /** True when victim has global override set (wants no hurt effect). Used for normal damage path. */
    public static boolean hasVictimHurtEffectOverride(LivingEntity victim) {
        return victim instanceof Player p && Boolean.FALSE.equals(p.getTag(VICTIM_HURT_EFFECT_OVERRIDE));
    }

    /** Resolve effective hurtEffect for replacement only: victim override wins over props when set to false. */
    public static boolean shouldApplyHurtEffect(LivingEntity victim, DamageTypeProperties props) {
        if (victim instanceof Player p) {
            Boolean override = p.getTag(VICTIM_HURT_EFFECT_OVERRIDE);
            if (Boolean.FALSE.equals(override)) return false;
        }
        return props.hurtEffect();
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static HealthSystem initialize(HealthConfig config) {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("HealthSystem");
            return instance;
        }
        instance = new HealthSystem(config);
        instance.registerListeners();
        instance.markInitialized();
        LogUtil.logInit("HealthSystem");
        return instance;
    }

    private void registerListeners() {
        eventNode = EventNode.all("health");

        tickCounterTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    currentTick++;
                    invulnerability.updateTick();
                    processBufferedDamage();
                })
                .repeat(TaskSchedule.tick(1))
                .schedule();

        // Damage pipeline
        eventNode.addListener(EntityDamageEvent.class, event -> {
            if (!(event.getEntity() instanceof LivingEntity victim)) return;
            UUID id = victim.getUuid();
            Long last = lastProcessedTick.get(id);
            if (last != null && last == currentTick) { event.setCancelled(true); return; }
            lastProcessedTick.put(id, currentTick);

            DamageType dt = DamageType.find(event.getDamage().getType());
            DamageResult result = (dt != null)
                    ? dt.processDamage(event, invulnerability, config)
                    : DamageType.processUnregistered(event, invulnerability, config);

            // Trigger knockback from damage result
            if (result.applied() || result.wasReplacement()) {
                applyKnockbackFromResult(result);
                if (result.attacker() instanceof Player attacker) {
                    for (AttackLandedListener l : attackLandedListeners) {
                        l.onAttackLanded(attacker, victim, currentTick);
                    }
                }
            }
        });

        // Environmental tick
        eventNode.addListener(PlayerTickEvent.class, e -> {
            Player p = e.getPlayer();
            for (DamageTracker t : DamageType.getTrackers()) t.tick(p, currentTick);
        });

        // Death / spawn notifications
        eventNode.addListener(PlayerDeathEvent.class, e -> {
            for (DamageTracker t : DamageType.getTrackers()) t.onPlayerDeath(e.getPlayer());
        });
        eventNode.addListener(PlayerSpawnEvent.class, e -> {
            for (DamageTracker t : DamageType.getTrackers()) t.onPlayerSpawn(e.getPlayer());
        });

        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
    }

    // ===========================
    // LOOKUP
    // ===========================

    public DamageType findDamageType(RegistryKey<?> type) { return DamageType.find(type); }
    public DamageType getDamageType(String name) { return DamageType.get(name); }
    public List<DamageType> getDamageTypes() { return DamageType.getAll(); }

    public boolean isBlockingApplicable(RegistryKey<?> damageType, Player victim) {
        DamageType dt = DamageType.find(damageType);
        if (dt == null) return DamageTypeProperties.ATTACK_DEFAULT.blockable();
        return dt.resolveProperties(null, victim, null).blockable();
    }

    // ===========================
    // PUBLIC API
    // ===========================

    /**
     * Schedule damage to apply when the victim leaves invulnerability. Only one hit per victim is buffered.
     * @param victim the entity to damage
     * @param damage snapshot of the damage (final amount after modifiers/blocking)
     * @param applyAtTick tick when invulnerability ends for this victim
     * @param wasSprinting whether the attacker was sprinting at hit time (for correct knockback when buffer fires)
     * @return true if scheduled, false if another hit is already buffered for this victim
     */
    public boolean scheduleBufferedDamage(LivingEntity victim, Damage damage, long applyAtTick, boolean wasSprinting) {
        UUID id = victim.getUuid();
        if (invulnerabilityBuffer.containsKey(id)) return false;
        invulnerabilityBuffer.put(id, new BufferedDamageEntry(applyAtTick, damage, wasSprinting));
        return true;
    }

    /** Whether a buffered hit is already scheduled for this victim. */
    public boolean hasBufferedHit(LivingEntity victim) {
        return invulnerabilityBuffer.containsKey(victim.getUuid());
    }

    /** Clear any buffered hit for this victim. Called when replacement supersedes the buffer. */
    public void clearBufferedHit(LivingEntity victim) {
        invulnerabilityBuffer.remove(victim.getUuid());
    }

    /**
     * Update health without triggering hurt/screen tilt.
     * <ul>
     *   <li>1.8 clients: call setHealth (keeps server's this.health in sync), suppress
     *       UpdateHealth/EntityAttributes packets, then send EntityMetaDataPacket to the victim.
     *       The metadata (index 6 = health) updates the bar without tilt.</li>
     *   <li>Modern clients: max-health trick (temp max=new, setHealth, restore max).</li>
     * </ul>
     */
    public static void setHealthWithoutHurtEffect(Player player, float newHealth) {
        ClientVersionDetector detector = ClientVersionDetector.getInstance();
        if (detector.getClientVersion(player) == ClientVersionDetector.ClientVersion.LEGACY) {
            LegacyHurtSuppression.getInstance();
            LegacyHurtSuppression.setSuppressNextHealthPacket(player, true);
            try {
                player.setHealth(newHealth);
                player.sendPacket(player.getMetadataPacket());
            } finally {
                LegacyHurtSuppression.setSuppressNextHealthPacket(player, false);
            }
            return;
        }
        var maxAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double originalBase = maxAttr.getBaseValue();
        maxAttr.setBaseValue(Math.max(0.5, newHealth)); // min 0.5 for attribute validity
        player.setHealth(newHealth);
        maxAttr.setBaseValue(originalBase);
    }

    private void processBufferedDamage() {
        invulnerabilityBuffer.entrySet().removeIf(entry -> {
            if (entry.getValue().applyAtTick > currentTick) return false;
            LivingEntity victim = findLivingByUuid(entry.getKey());
            if (victim == null || victim.isRemoved()) return true;
            BufferedDamageEntry buf = entry.getValue();
            try {
                BUFFERED_WAS_SPRINTING.set(buf.wasSprinting());
                applyDamage(victim, buf.damage());
            } finally {
                BUFFERED_WAS_SPRINTING.remove();
            }
            return true;
        });
    }

    private LivingEntity findLivingByUuid(UUID uuid) {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (player.getUuid().equals(uuid)) return player;
        }
        return null; // non-player living entities: would need instance iteration; players cover the main case
    }

    /**
     * Process a player melee attack. For replacement hits (victim in i-frames, higher damage),
     * updates health directly without firing the damage event — avoiding client-side damage
     * effects like screen tilt. Normal hits and buffered hits still go through the event pipeline.
     */
    public boolean processPlayerMeleeAttack(Player attacker, LivingEntity victim) {
        Damage damage = new Damage(net.minestom.server.entity.damage.DamageType.PLAYER_ATTACK,
                attacker, attacker, attacker.getPosition(), 0);
        com.minestom.mechanics.systems.health.damage.DamageType melee = DamageType.find(damage.getType());
        if (melee == null) melee = DamageType.get("melee");
        if (melee == null) return applyDamage(victim, damage);

        DamageTypeProperties props = melee.resolveProperties(attacker, attacker, victim, attacker.getItemInMainHand());
        if (!props.enabled()) return false;

        if (victim instanceof Player p && p.getGameMode() == GameMode.CREATIVE && !props.bypassCreative())
            return false;

        float damageAmount = melee.calculateDamage(attacker, victim, attacker.getItemInMainHand(), 0);
        if (victim instanceof Player victimPlayer && props.blockable()) {
            try {
                BlockingSystem blocking = BlockingSystem.getInstance();
                if (blocking.isEnabled() && blocking.isBlocking(victimPlayer) && isBlockingApplicable(damage.getType(), victimPlayer)) {
                    float originalAmount = damageAmount;
                    double reduction = blocking.getDamageReduction(victimPlayer);
                    damageAmount = (float) (originalAmount * (1.0 - reduction));
                    if (originalAmount > damageAmount) {
                        MinecraftServer.getGlobalEventHandler().call(new BlockingDamageEvent(victimPlayer, originalAmount, damageAmount, damage.getType()));
                    }
                }
            } catch (IllegalStateException ignored) {}
        }

        // Normal hits: delegate to applyDamage. Do NOT call markDamaged here — it would put
        // the victim in i-frames before the event fires, causing processDamage to block the hit.
        if (props.bypassInvulnerability()) return applyDamage(victim, damage);
        if (!invulnerability.isInvulnerable(victim)) return applyDamage(victim, damage);

        // In i-frames: buffer or replacement
        if (props.invulnerabilityBufferTicks() > 0 && config.isInvulnerabilityEnabled()) {
            long ticksSince = invulnerability.getTicksSinceLastDamage(victim);
            if (ticksSince >= 0) {
                int scaledInvuln = TickScaler.scale(config.invulnerabilityTicks(), TickScalingConfig.getMode());
                int scaledBuffer = TickScaler.scale(props.invulnerabilityBufferTicks(), TickScalingConfig.getMode());
                long ticksRemaining = scaledInvuln - ticksSince;
                if (ticksRemaining <= scaledBuffer) {
                    if (!hasBufferedHit(victim)) {
                        long applyAtTick = invulnerability.getLastDamageTick(victim) + scaledInvuln;
                        net.minestom.server.entity.damage.Damage snap = new net.minestom.server.entity.damage.Damage(
                                damage.getType(), damage.getSource(), damage.getAttacker(),
                                damage.getSourcePosition() != null ? damage.getSourcePosition() : attacker.getPosition(),
                                damageAmount);
                        scheduleBufferedDamage(victim, snap, applyAtTick, attacker.isSprinting());
                    }
                    return false;
                }
            }
        }

        if (!props.damageReplacement()) return false;

        ItemStack currentItem = attacker.getItemInMainHand();
        if (props.noReplacementSameItem() && DamageTypeProperties.isSameItem(currentItem, invulnerability.getLastMeleeItem(victim)))
            return false;

        float previousDamage = invulnerability.getLastDamageAmount(victim);
        if (damageAmount < previousDamage + props.replacementCutoff()) return false;

        // Replacement: update health directly (cancel event path to avoid double-processing).
        float damageDifference = damageAmount - previousDamage;
        float finalDifference = damageDifference;
        if (!props.penetratesArmor() && victim instanceof Player player) {
            try {
                ArmorManager armorManager = MechanicsManager.getInstance().getArmorManager();
                if (armorManager != null && armorManager.isInitialized() && armorManager.isEnabled())
                    finalDifference = armorManager.calculateReducedDamage(player, damageDifference, damage.getType());
            } catch (IllegalStateException ignored) {}
        }

        float newHealth = Math.max(0, victim.getHealth() - finalDifference);
        if (!shouldApplyHurtEffect(victim, props) && victim instanceof Player player && newHealth > 0) {
            setHealthWithoutHurtEffect(player, newHealth);
        } else {
            victim.setHealth(newHealth);
        }

        invulnerability.updateDamageAmount(victim, damageAmount);
        invulnerability.setLastDamageReplacement(victim, true);
        clearBufferedHit(victim);

        DamageResult result = new DamageResult(true, true, finalDifference, props, attacker, attacker, victim, null);
        applyKnockbackFromResult(result);
        for (AttackLandedListener l : attackLandedListeners)
            l.onAttackLanded(attacker, victim, currentTick);

        return true;
    }

    /**
     * Apply damage through the health pipeline. Use this instead of {@code victim.damage()}
     * to ensure creative mode is handled by the damage pipeline rather than Minestom's
     * built-in invulnerable flag (which skips the event entirely).
     */
    public static boolean applyDamage(LivingEntity victim, Damage damage) {
        // Minestom skips the damage event for invulnerable entities (creative mode).
        // Temporarily clear so the event fires; the damage pipeline handles the creative check.
        boolean wasInvulnerable = false;
        if (victim instanceof Player p && p.getGameMode() == GameMode.CREATIVE && victim.isInvulnerable()) {
            victim.setInvulnerable(false);
            wasInvulnerable = true;
        }
        boolean result = victim.damage(damage);
        if (wasInvulnerable) victim.setInvulnerable(true);
        return result;
    }

    /**
     * Apply knockback based on damage result. Called automatically after damage pipeline.
     * Determines melee vs projectile from the source entity and delegates to KnockbackApplicator.
     */
    private void applyKnockbackFromResult(DamageResult result) {
        if (result.attacker() == null && result.source() == null) return; // environmental — no kb source

        if (result.wasReplacement() && !result.props().knockbackOnReplacement()) return;

        try {
            var knockbackApplicator = com.minestom.mechanics.manager.ProjectileManager.getInstance().getKnockbackApplicator();
            boolean isProjectile = result.source() != null && result.source() != result.attacker();

            // Buffered hits: use wasSprinting captured at buffer time (attacker may have stopped by apply time)
            Boolean bufferedSprint = BUFFERED_WAS_SPRINTING.get();
            boolean wasSprinting = bufferedSprint != null
                    ? bufferedSprint
                    : (result.attacker() instanceof Player p && p.isSprinting());
            boolean trustWasSprinting = bufferedSprint != null; // authoritative when from buffer

            knockbackApplicator.applyKnockback(
                    result.victim(),
                    result.attacker(),
                    result.source(),
                    result.shooterOriginPos(),
                    isProjectile
                            ? com.minestom.mechanics.systems.knockback.KnockbackSystem.KnockbackType.PROJECTILE
                            : com.minestom.mechanics.systems.knockback.KnockbackSystem.KnockbackType.ATTACK,
                    wasSprinting,
                    trustWasSprinting,
                    0 // TODO: enchantment level from tags
            );
        } catch (IllegalStateException ignored) {
            // KnockbackSystem or ProjectileManager not initialized
        }
    }

    public boolean canTakeDamage(LivingEntity entity) { return !invulnerability.isInvulnerable(entity); }
    public boolean wasLastDamageReplacement(LivingEntity entity) { return invulnerability.wasLastDamageReplacement(entity); }

    public double getFallDistance(Player player) {
        DamageTracker t = DamageType.getTracker("fall");
        return t != null ? t.getFallDistance(player) : 0;
    }

    public void resetFallDistance(Player player) {
        DamageTracker t = DamageType.getTracker("fall");
        if (t != null) t.resetFallDistance(player);
    }

    public void cleanup(LivingEntity entity) {
        lastProcessedTick.remove(entity.getUuid());
        invulnerabilityBuffer.remove(entity.getUuid());
        invulnerability.clearState(entity);
        for (DamageType dt : DamageType.getAll()) dt.cleanup(entity);
        if (entity instanceof Player player) {
            for (DamageTracker t : DamageType.getTrackers()) t.cleanup(player);
            LogUtil.logCleanup("HealthSystem", player.getUsername());
        }
    }

    // ===========================
    // GETTERS / CONFIG
    // ===========================

    public HealthConfig getConfig() { return config; }
    public long getCurrentTick() { return currentTick; }
    public InvulnerabilityTracker getInvulnerability() { return invulnerability; }

    public void updateConfig(HealthConfig newConfig) {
        this.config = newConfig;
        log.info("HealthSystem configuration updated");
    }

    // ===========================
    // SHUTDOWN
    // ===========================

    @Override
    public void shutdown() {
        if (eventNode != null) { MinecraftServer.getGlobalEventHandler().removeChild(eventNode); eventNode = null; }
        if (tickCounterTask != null) { tickCounterTask.cancel(); tickCounterTask = null; }
        lastProcessedTick.clear();
        invulnerabilityBuffer.clear();
        DamageType.clearRegistry();
        log.info("HealthSystem shutdown complete");
    }

    public static HealthSystem getInstance() {
        if (instance == null) throw new IllegalStateException("HealthSystem not initialized!");
        return instance;
    }
}

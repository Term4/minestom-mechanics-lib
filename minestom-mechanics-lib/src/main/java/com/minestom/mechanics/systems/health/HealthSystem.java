package com.minestom.mechanics.systems.health;

import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.systems.health.damage.*;
import com.minestom.mechanics.systems.health.damage.types.*;
import com.minestom.mechanics.systems.health.damage.util.DamageOverride;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
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

    private record BufferedDamageEntry(long applyAtTick, Damage damage) {}
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
     * @return true if scheduled, false if another hit is already buffered for this victim
     */
    public boolean scheduleBufferedDamage(LivingEntity victim, Damage damage, long applyAtTick) {
        UUID id = victim.getUuid();
        if (invulnerabilityBuffer.containsKey(id)) return false;
        invulnerabilityBuffer.put(id, new BufferedDamageEntry(applyAtTick, damage));
        return true;
    }

    /** Whether a buffered hit is already scheduled for this victim. */
    public boolean hasBufferedHit(LivingEntity victim) {
        return invulnerabilityBuffer.containsKey(victim.getUuid());
    }

    private void processBufferedDamage() {
        invulnerabilityBuffer.entrySet().removeIf(entry -> {
            if (entry.getValue().applyAtTick > currentTick) return false;
            LivingEntity victim = findLivingByUuid(entry.getKey());
            if (victim == null || victim.isRemoved()) return true;
            applyDamage(victim, entry.getValue().damage);
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

            knockbackApplicator.applyKnockback(
                    result.victim(),
                    result.attacker(),
                    result.source(),
                    result.shooterOriginPos(),
                    isProjectile
                            ? com.minestom.mechanics.systems.knockback.KnockbackSystem.KnockbackType.PROJECTILE
                            : com.minestom.mechanics.systems.knockback.KnockbackSystem.KnockbackType.ATTACK,
                    result.attacker() instanceof Player p && p.isSprinting(),
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

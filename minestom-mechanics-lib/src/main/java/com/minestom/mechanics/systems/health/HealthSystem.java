package com.minestom.mechanics.systems.health;

import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.systems.health.damagetypes.Cactus;
import com.minestom.mechanics.systems.health.damagetypes.DamageTypeRegistry;
import com.minestom.mechanics.systems.health.damagetypes.FallDamage;
import com.minestom.mechanics.systems.health.damagetypes.Fire;
import com.minestom.mechanics.systems.health.tags.InvulnerabilityTagSerializer;
import com.minestom.mechanics.systems.health.util.DamageApplicator;
import com.minestom.mechanics.systems.health.util.Invulnerability;
import com.minestom.mechanics.systems.projectile.tags.ProjectileTagRegistry;
import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main health system orchestrator - coordinates all health-related components.
 * Replaces DamageFeature with a more general health system architecture.
 */
public class HealthSystem extends InitializableSystem {
    private static HealthSystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("HealthSystem");

    // Component references
    private final Invulnerability invulnerability;
    private final DamageApplicator damageApplicator;
    private final DamageTypeRegistry damageTypeRegistry;
    private final FallDamage fallDamage;
    private final Fire fire;
    private final Cactus cactus;

    // Configuration
    private HealthConfig config;
    private long currentTick = 0;
    
    // Attack deduplication - prevent same attack from being processed multiple times
    private final Map<UUID, Long> lastProcessedTick = new ConcurrentHashMap<>();

    // Tasks
    private Task tickCounterTask;
    private Task cleanupTask;
    
    // ===========================
    // TAG SYSTEM (using ConfigTagWrapper pattern)
    // ===========================
    
    /** Tag for fall damage configuration */
    public static final Tag<com.minestom.mechanics.systems.health.tags.HealthTagWrapper> FALL_DAMAGE = 
            Tag.Structure("health_fall_damage", new com.minestom.mechanics.systems.health.tags.HealthTagSerializer());
    
    /** Tag for fire damage configuration */
    public static final Tag<com.minestom.mechanics.systems.health.tags.HealthTagWrapper> FIRE_DAMAGE = 
            Tag.Structure("health_fire_damage", new com.minestom.mechanics.systems.health.tags.HealthTagSerializer());
    
    /** Tag for cactus damage configuration */
    public static final Tag<com.minestom.mechanics.systems.health.tags.HealthTagWrapper> CACTUS_DAMAGE = 
            Tag.Structure("health_cactus_damage", new com.minestom.mechanics.systems.health.tags.HealthTagSerializer());
    
    /** Tag for invulnerability configuration (using ConfigTagWrapper pattern) */
    public static final Tag<com.minestom.mechanics.systems.health.tags.InvulnerabilityTagWrapper> INVULNERABILITY = 
            Tag.Structure("health_invulnerability", new InvulnerabilityTagSerializer());

    /** Alias for ProjectileTagRegistry: copy invulnerability (bypass i-frame + bypass creative) from item to projectile. */
    public static final Tag<com.minestom.mechanics.systems.health.tags.InvulnerabilityTagWrapper> PROJECTILE_INVULNERABILITY = INVULNERABILITY;

    private HealthSystem(HealthConfig config) {
        this.config = config;
        
        // Initialize components
        this.invulnerability = new Invulnerability(config);
        this.damageTypeRegistry = new DamageTypeRegistry();
        this.damageApplicator = new DamageApplicator(config, invulnerability, damageTypeRegistry);

        // Initialize damage types
        this.fallDamage = new FallDamage(config);
        this.fire = new Fire(config);
        this.cactus = new Cactus(config);

        // Register damage types
        damageTypeRegistry.register(fallDamage);
        damageTypeRegistry.register(fire);
        damageTypeRegistry.register(cactus);
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

        ProjectileTagRegistry.register(HealthSystem.class);

        // Initialize player death handler
        com.minestom.mechanics.systems.player.PlayerDeathHandler.initialize();

        LogUtil.logInit("HealthSystem");
        return instance;
    }

    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Start tick counter
        this.tickCounterTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    currentTick++;
                    invulnerability.updateTick();
                })
                .repeat(TaskSchedule.tick(1))
                .schedule();

        // Player tick events for fall, fire, and cactus damage tracking
        handler.addListener(PlayerTickEvent.class, event -> {
            var player = event.getPlayer();
            fallDamage.trackFallDamage(player);
            fire.trackAndApplyFireDamage(player, currentTick);
            cactus.trackAndApplyCactusDamage(player, currentTick);
        });
        
        // Player death events - reset fall distance (death handling is in PlayerDeathHandler)
        handler.addListener(PlayerDeathEvent.class, event -> {
            fallDamage.resetFallDistance(event.getPlayer());
        });
        
        // Player spawn events - reset fall distance
        handler.addListener(PlayerSpawnEvent.class, event -> {
            fallDamage.resetFallDistance(event.getPlayer());
        });

        // Entity damage events: duplicate/creative checks, then DamageApplicator, then damage type modifiers
        handler.addListener(EntityDamageEvent.class, event -> {
            if (event.getEntity() instanceof LivingEntity victim) {
                UUID victimId = victim.getUuid();

                // Duplicate processing guard (same tick)
                Long lastProcessed = lastProcessedTick.get(victimId);
                if (lastProcessed != null && lastProcessed == currentTick) {
                    log.debug("{} damage event rejected (duplicate in same tick): {:.1f}",
                            getEntityName(victim), event.getDamage().getAmount());
                    event.setCancelled(true);
                    return;
                }
                lastProcessedTick.put(victimId, currentTick);

                if (victim instanceof Player player && player.getGameMode() == net.minestom.server.entity.GameMode.CREATIVE) {
                    Entity attacker = event.getDamage().getSource();
                    net.minestom.server.item.ItemStack item = (attacker instanceof Player p) ? p.getItemInMainHand() : null;
                    boolean bypass = event.getDamage().getType().equals(DamageType.PLAYER_ATTACK)
                            ? invulnerability.isBypassCreativeInvulnerabilityMelee(attacker, victim, item)
                            : invulnerability.isBypassCreativeInvulnerability(attacker, victim, item);
                    if (!bypass) {
                        event.setCancelled(true);
                        log.debug("Blocked damage for creative player: {}", player.getUsername());
                        return;
                    }
                }

                damageApplicator.handleDamage(event);
            }

            // Process damage types (fire, cactus modifiers)
            damageTypeRegistry.processEntityDamageEvent(event);
        });

        // Start periodic cleanup
        startPeriodicCleanup();
    }

    // ===========================
    // PUBLIC API
    // ===========================

    /**
     * Check if an entity is currently outside the i-frame window (would allow a normal hit).
     * Does not account for damage-type or weapon bypass; for full flow use DamageApplicator.
     */
    public boolean canTakeDamage(LivingEntity entity, float incomingDamage) {
        long ticksSince = invulnerability.getTicksSinceLastDamage(entity);
        if (ticksSince < 0) return true;
        int invulnTicks = invulnerability.getInvulnerabilityTicks(null, entity, null);
        return ticksSince >= invulnTicks;
    }

    /**
     * Check if last damage was a replacement
     */
    public boolean wasLastDamageReplacement(LivingEntity entity) {
        return invulnerability.wasLastDamageReplacement(entity);
    }

    /**
     * Get fall distance for a player
     */
    public double getFallDistance(Player player) {
        return fallDamage.getFallDistance(player);
    }

    /**
     * Reset fall distance for a player
     */
    public void resetFallDistance(Player player) {
        fallDamage.resetFallDistance(player);
    }

    /**
     * Clean up tracking data for an entity
     */
    public void cleanup(LivingEntity entity) {
        UUID entityId = entity.getUuid();
        lastProcessedTick.remove(entityId);
        invulnerability.cleanup(entity);
        damageTypeRegistry.cleanup(entity);
        if (entity instanceof Player player) {
            LogUtil.logCleanup("HealthSystem", player.getUsername());
        }
    }

    /**
     * Get the configured invulnerability duration in ticks
     */
    public int getInvulnerabilityTicks() {
        return config.getInvulnerabilityTicks();
    }

    /**
     * Get current tick
     */
    public long getCurrentTick() {
        return currentTick;
    }

    /**
     * Get number of tracked entities
     */
    public int getTrackedEntities() {
        return invulnerability.getTrackedEntities();
    }

    /**
     * Get configuration
     */
    public HealthConfig getConfig() {
        return config;
    }

    /**
     * Get damage type registry
     */
    public DamageTypeRegistry getDamageTypeRegistry() {
        return damageTypeRegistry;
    }

    // ===========================
    // CLEANUP
    // ===========================

    private void startPeriodicCleanup() {
        this.cleanupTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    // Clean up stale entries
                    int beforeSize = invulnerability.getTrackedEntities();
                    
                    if (beforeSize > 0) {
                        log.debug("Periodic cleanup completed");
                    }
                })
                .repeat(TaskSchedule.seconds(60)) // Every minute
                .schedule();
    }

    public void shutdown() {
        if (tickCounterTask != null) {
            tickCounterTask.cancel();
            tickCounterTask = null;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        lastProcessedTick.clear();
        invulnerability.clearAll(); // Clear all tracking data
        log.info("HealthSystem shutdown complete");
    }

    // ===========================
    // CONFIGURATION
    // ===========================
    
    /**
     * Update health configuration at runtime
     */
    public void updateConfig(HealthConfig newConfig) {
        this.config = newConfig;
        // Note: The components will use the new config on their next operation since they reference this.config
        log.info("HealthSystem configuration updated");
    }

    // ===========================
    // STATIC ACCESS
    // ===========================

    public static HealthSystem getInstance() {
        if (instance == null) {
            throw new IllegalStateException("HealthSystem not initialized!");
        }
        return instance;
    }

    private String getEntityName(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getUsername();
        }
        return entity.getClass().getSimpleName();
    }
    
    /**
     * Centralized damage logging for all damage types.
     * This prevents duplicate logging across different damage systems.
     */
    private void logDamage(LivingEntity victim, net.minestom.server.entity.damage.Damage damage, float amount) {
        String damageType = damage.getType().name();
        String victimName = getEntityName(victim);
        
        // Format damage amount consistently
        String formattedAmount = String.format("%.2f", amount);
        
        // Log with consistent format
        log.debug("{} took {} {} damage", victimName, formattedAmount, damageType);
    }
    
    // ===========================
    // TAG USAGE EXAMPLES
    // ===========================
    
    /**
     * Example usage of health tags:
     * <pre>
     * import static HealthTagWrapper.*;
     * import static InvulnerabilityTagWrapper.*;
     * 
     * // Per-item configuration (weapon that reduces fall damage)
     * ItemStack boots = ItemStack.of(Material.LEATHER_BOOTS);
     * boots.setTag(HealthSystem.FALL_DAMAGE, healthMult(0.5)); // 50% fall damage
     * 
     * // Per-player configuration (player immune to fire)
     * player.setTag(HealthSystem.FIRE_DAMAGE, DISABLED);
     * 
     * // Per-world configuration (world with double fall damage)
     * world.setTag(HealthSystem.FALL_DAMAGE, DOUBLE_DAMAGE);
     * 
     * // Combined multiplier and modify
     * player.setTag(HealthSystem.CACTUS_DAMAGE, healthMult(2.0).thenAdd(5.0));
     * 
     * // Invulnerability configuration
     * player.setTag(HealthSystem.INVULNERABILITY, invulnSet(InvulnerabilityTagValue.invulnTicks(20)));
     * world.setTag(HealthSystem.INVULNERABILITY, REPLACEMENT_ENABLED);
     * 
     * // Bypass creative mode invulnerability (damage from this item/entity/world can hit creative players)
     * sword.setTag(HealthSystem.INVULNERABILITY, invulnSet(InvulnerabilityTagValue.BYPASS_CREATIVE));
     * </pre>
     * 
     * Priority chain for damage types:
     * Item > Attacker > Player > Victim > World > Server Default
     * 
     * Priority chain for invulnerability:
     * Item > Attacker > Player > Victim > World > Server Default
     */
}


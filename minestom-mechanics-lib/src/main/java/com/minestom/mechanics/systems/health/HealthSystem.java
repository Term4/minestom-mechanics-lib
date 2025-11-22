package com.minestom.mechanics.systems.health;

import com.minestom.mechanics.manager.ArmorManager;
import com.minestom.mechanics.manager.MechanicsManager;
import com.minestom.mechanics.systems.health.damagetypes.CactusDamageType;
import com.minestom.mechanics.systems.health.damagetypes.DamageTypeRegistry;
import com.minestom.mechanics.systems.health.damagetypes.FallDamageType;
import com.minestom.mechanics.systems.health.damagetypes.FireDamageType;
import com.minestom.mechanics.systems.health.tags.InvulnerabilityTagSerializer;
import com.minestom.mechanics.systems.health.util.Invulnerability;
import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
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
    private final DamageTypeRegistry damageTypeRegistry;
    private final FallDamageType fallDamageType;
    private final FireDamageType fireDamageType;
    private final CactusDamageType cactusDamageType;

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

    private HealthSystem(HealthConfig config) {
        this.config = config;
        
        // Initialize components
        this.invulnerability = new Invulnerability(config);
        this.damageTypeRegistry = new DamageTypeRegistry();
        
        // Initialize damage types
        this.fallDamageType = new FallDamageType(config);
        this.fireDamageType = new FireDamageType(config);
        this.cactusDamageType = new CactusDamageType(config);
        
        // Register damage types
        damageTypeRegistry.register(fallDamageType);
        damageTypeRegistry.register(fireDamageType);
        damageTypeRegistry.register(cactusDamageType);
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
        var handler = MinecraftServer.getGlobalEventHandler();

        // Start tick counter
        this.tickCounterTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    currentTick++;
                    invulnerability.updateTick();
                })
                .repeat(TaskSchedule.tick(1))
                .schedule();

        // Player tick events for fall damage tracking
        handler.addListener(PlayerTickEvent.class, event -> {
            fallDamageType.trackFallDamage(event.getPlayer());
        });

        // Entity damage events
        handler.addListener(EntityDamageEvent.class, event -> {
            if (event.getEntity() instanceof LivingEntity victim) {
                UUID victimId = victim.getUuid();
                
                // Check for duplicate processing in the same tick
                Long lastProcessed = lastProcessedTick.get(victimId);
                if (lastProcessed != null && lastProcessed == currentTick) {
                    log.debug("{} damage event rejected (duplicate in same tick): {:.1f}",
                            getEntityName(victim), event.getDamage().getAmount());
                    event.setCancelled(true);
                    return;
                }
                
                // Mark this entity as processed in this tick
                lastProcessedTick.put(victimId, currentTick);
                
                // ✅ FIXED: Disable all damage for creative mode players
                if (victim instanceof Player player && player.getGameMode() == net.minestom.server.entity.GameMode.CREATIVE) {
                    event.setCancelled(true);
                    log.debug("Blocked damage for creative player: {}", player.getUsername());
                    return;
                }
                
                float damageAmount = event.getDamage().getAmount();
                
                // Always log damage attempts for debugging
                log.debug("Damage attempt on {}: amount={}, tick={}, invulnTicks={}", 
                    getEntityName(victim), damageAmount, currentTick, config.getInvulnerabilityTicks());

                if (!invulnerability.canTakeDamage(victim, damageAmount)) {
                    event.setCancelled(true);
                    log.debug("Damage blocked by invulnerability for {}", getEntityName(victim));
                    return;
                }

                // Check if this is a replacement hit
                boolean isReplacement = invulnerability.wasLastDamageReplacement(victim);

                if (isReplacement) {
                    // For replacement hits: we need to calculate the damage difference
                    float previousDamage = invulnerability.getLastDamageAmount(victim);
                    float damageDifference = damageAmount - previousDamage;
                    
                    if (damageDifference > 0) {
                        // Cancel the event to prevent knockback/animation
                        event.setCancelled(true);
                        
                        // Apply armor reduction to the damage difference if victim is a player
                        float finalDifference = damageDifference;
                        if (victim instanceof Player player && MechanicsManager.getInstance().getArmorManager() != null) {
                            ArmorManager armorManager = MechanicsManager.getInstance().getArmorManager();
                            if (armorManager.isInitialized() && armorManager.isEnabled()) {
                                finalDifference = armorManager.calculateReducedDamage(player, damageDifference, event.getDamage().getType());
                            }
                        }
                        
                        // Apply only the difference in damage
                        float currentHealth = victim.getHealth();
                        float newHealth = Math.max(0, currentHealth - finalDifference);
                        victim.setHealth(newHealth);
                        
                        if (config.isLogReplacementDamage()) {
                            log.debug("{} took {} additional damage silently (replacement: {} -> {}, armor reduced: {})",
                                    getEntityName(victim), 
                                    String.format("%.1f", finalDifference),
                                    String.format("%.1f", previousDamage),
                                    String.format("%.1f", damageAmount),
                                    String.format("%.1f", damageDifference - finalDifference));
                        }
                    }

                    // Update the tracked damage amount WITHOUT resetting invulnerability timer
                    // Replacement hits should only update damage, not give new i-frames
                    invulnerability.updateDamageAmount(victim, damageAmount);
                    return;
                }

                // Normal hit - let it process naturally
                log.debug("Setting {} as invulnerable for {} ticks after taking {} damage", 
                    getEntityName(victim), config.getInvulnerabilityTicks(), damageAmount);
                invulnerability.setInvulnerable(victim, damageAmount);
                
                // ✅ CENTRALIZED: Log all damage through the base health system
                logDamage(victim, event.getDamage(), damageAmount);
            }
            
            // Process damage types (fire, cactus, etc.)
            damageTypeRegistry.processEntityDamageEvent(event);
        });

        // Player spawn events
        handler.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            fallDamageType.resetFallDistance(player);
            log.debug("Reset fall distance for {} on spawn", player.getUsername());
        });

        // Player death events
        handler.addListener(PlayerDeathEvent.class, event -> {
            Player player = event.getPlayer();
            fallDamageType.resetFallDistance(player);
            
            // Cancel death messages to prevent 1.7 client crashes with incomplete TranslatableComponent
            event.setDeathText(null);
            event.setChatMessage(null);
            
            log.debug("Reset fall distance for {} on death", player.getUsername());
        });

        // Start periodic cleanup
        startPeriodicCleanup();
    }

    // ===========================
    // PUBLIC API
    // ===========================

    /**
     * Check if an entity can take damage
     */
    public boolean canTakeDamage(LivingEntity entity, float incomingDamage) {
        return invulnerability.canTakeDamage(entity, incomingDamage);
    }

    /**
     * Check if knockback should be applied
     */
    public boolean shouldApplyKnockback(LivingEntity entity) {
        return invulnerability.shouldApplyKnockback(entity);
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
        return fallDamageType.getFallDistance(player);
    }

    /**
     * Reset fall distance for a player
     */
    public void resetFallDistance(Player player) {
        fallDamageType.resetFallDistance(player);
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
     * </pre>
     * 
     * Priority chain for damage types:
     * Item > Attacker > Player > Victim > World > Server Default
     * 
     * Priority chain for invulnerability:
     * Item > Attacker > Player > Victim > World > Server Default
     */
}


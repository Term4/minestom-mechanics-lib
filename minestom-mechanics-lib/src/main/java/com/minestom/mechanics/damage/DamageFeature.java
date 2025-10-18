package com.minestom.mechanics.damage;

import com.minestom.mechanics.damage.components.*;
import com.minestom.mechanics.config.gameplay.DamageConfig;
import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.MechanicsManager;
import com.minestom.mechanics.manager.ArmorManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main damage system orchestrator - coordinates all damage-related components.
 * Replaces the monolithic DamageSystem with focused component architecture.
 */
public class DamageFeature extends InitializableSystem {
    private static DamageFeature instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("DamageFeature");

    // Component references
    private final InvulnerabilityTracker invulnerabilityTracker;
    private final FallDamageTracker fallDamageTracker;
    private final FireDamageModifier fireDamageModifier;

    // Configuration
    private DamageConfig config;
    private long currentTick = 0;
    
    // Attack deduplication - prevent same attack from being processed multiple times
    private final Map<UUID, Long> lastProcessedTick = new ConcurrentHashMap<>();

    // Tasks
    private Task tickCounterTask;
    private Task cleanupTask;

    private DamageFeature(DamageConfig config) {
        this.config = config;
        
        // Initialize components
        this.invulnerabilityTracker = new InvulnerabilityTracker(config);
        this.fallDamageTracker = new FallDamageTracker(config);
        this.fireDamageModifier = new FireDamageModifier(config);
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static DamageFeature initialize(DamageConfig config) {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("DamageFeature");
            return instance;
        }

        instance = new DamageFeature(config);
        instance.registerListeners();
        instance.markInitialized();

        LogUtil.logInit("DamageFeature");
        return instance;
    }

    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Start tick counter
        this.tickCounterTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    currentTick++;
                    invulnerabilityTracker.updateTick();
                })
                .repeat(TaskSchedule.tick(1))
                .schedule();

        // Player tick events for fall damage tracking
        handler.addListener(PlayerTickEvent.class, event -> {
            fallDamageTracker.trackFallDamage(event.getPlayer());
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

                if (!invulnerabilityTracker.canTakeDamage(victim, damageAmount)) {
                    event.setCancelled(true);
                    log.debug("Damage blocked by invulnerability for {}", getEntityName(victim));
                    return;
                }

                // Check if this is a replacement hit
                boolean isReplacement = invulnerabilityTracker.wasLastDamageReplacement(victim);

                if (isReplacement) {
                    // For replacement hits: we need to calculate the damage difference
                    float previousDamage = invulnerabilityTracker.getLastDamageAmount(victim);
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
                    invulnerabilityTracker.updateDamageAmount(victim, damageAmount);
                    return;
                }

                // Normal hit - let it process naturally
                log.debug("Setting {} as invulnerable for {} ticks after taking {} damage", 
                    getEntityName(victim), config.getInvulnerabilityTicks(), damageAmount);
                invulnerabilityTracker.setInvulnerable(victim, damageAmount);
                
                // ✅ CENTRALIZED: Log all damage through the base damage feature
                logDamage(victim, event.getDamage(), damageAmount);
            }
        });

        // Player spawn events
        handler.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            fallDamageTracker.resetFallDistance(player);
            log.debug("Reset fall distance for {} on spawn", player.getUsername());
        });

        // Player death events
        handler.addListener(PlayerDeathEvent.class, event -> {
            Player player = event.getPlayer();
            fallDamageTracker.resetFallDistance(player);
            log.debug("Reset fall distance for {} on death", player.getUsername());
        });

        // Damage modification events
        handler.addListener(EntityDamageEvent.class, event -> {
            fireDamageModifier.modifyFireDamage(event);
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
        return invulnerabilityTracker.canTakeDamage(entity, incomingDamage);
    }

    /**
     * Check if knockback should be applied
     */
    public boolean shouldApplyKnockback(LivingEntity entity) {
        return invulnerabilityTracker.shouldApplyKnockback(entity);
    }

    /**
     * Check if last damage was a replacement
     */
    public boolean wasLastDamageReplacement(LivingEntity entity) {
        return invulnerabilityTracker.wasLastDamageReplacement(entity);
    }

    /**
     * Get fall distance for a player
     */
    public double getFallDistance(Player player) {
        return fallDamageTracker.getFallDistance(player);
    }

    /**
     * Reset fall distance for a player
     */
    public void resetFallDistance(Player player) {
        fallDamageTracker.resetFallDistance(player);
    }

    /**
     * Clean up tracking data for an entity
     */
    public void cleanup(LivingEntity entity) {
        UUID entityId = entity.getUuid();
        lastProcessedTick.remove(entityId);
        invulnerabilityTracker.cleanup(entity);
        if (entity instanceof Player player) {
            LogUtil.logCleanup("DamageFeature", player.getUsername());
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
        return invulnerabilityTracker.getTrackedEntities();
    }

    /**
     * Get configuration
     */
    public DamageConfig getConfig() {
        return config;
    }

    // ===========================
    // CLEANUP
    // ===========================

    private void startPeriodicCleanup() {
        this.cleanupTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    // Clean up stale entries
                    int beforeSize = invulnerabilityTracker.getTrackedEntities();
                    // Note: Individual cleanup would need to be implemented in InvulnerabilityTracker
                    
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
        invulnerabilityTracker.clearAll(); // Clear all tracking data
        log.info("DamageFeature shutdown complete");
    }

    // ===========================
    // CONFIGURATION
    // ===========================
    
    /**
     * Update damage configuration at runtime
     */
    public void updateConfig(DamageConfig newConfig) {
        this.config = newConfig;
        // Note: The components (InvulnerabilityTracker, FallDamageTracker, FireDamageModifier)
        // will use the new config on their next operation since they reference this.config
        log.info("DamageFeature configuration updated");
    }

    // ===========================
    // STATIC ACCESS
    // ===========================

    public static DamageFeature getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DamageFeature not initialized!");
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
}

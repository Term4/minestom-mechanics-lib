package com.minestom.mechanics.systems.attack;

import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.systems.knockback.KnockbackApplicator;
import com.minestom.mechanics.systems.knockback.KnockbackSystem;
import com.minestom.mechanics.systems.validation.hits.HitDetection;
import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.systems.misc.GameplayUtils;
import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.config.constants.MechanicsConstants;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerHandAnimationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.TaskSchedule;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: MAJOR todo here, we need to add a configurable method of detecting if hitbox / bounding box is obstructed.
//  Right now due to the swing animation method of detecting hits, players can be hit through walls. This issue extends to projectiles
//  Could be good to generalize this to be used for general checking if a bounding box / hitbox is obstructed.
//  We already have the block raycast feature, but players can be

// TODO: Rename to system, move to systems...

/**
 * Main attack feature that orchestrates all attack-related components.
 * Replaces the monolithic CombatSystem with focused responsibilities.
 */
public class AttackFeature extends InitializableSystem {
    private static AttackFeature instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("AttackFeature");

    // TODO: Consider moving all tick away from attackfeature to be handled by invulnerabilitytracker and damagefeature
    //  Alternatively, could leave this here for future updates with 1.9 hit cooldown tracking, but would probably want to
    //  make tracking conditional for efficiency
    private final CombatConfig config;
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAttackTick = new ConcurrentHashMap<>();
    private long currentTick = 0;
    
    // Attack components
    private final AttackCalculator attackCalculator;
    private final SprintBonusCalculator sprintBonusCalculator;
    
    private static final long ATTACK_DEDUPE_MS = MechanicsConstants.ATTACK_DEDUPE_MS;

    // Knockback components
    private KnockbackApplicator knockbackApplicator;

    private AttackFeature(CombatConfig config) {
        this.config = config;
        this.attackCalculator = new AttackCalculator(config);
        this.sprintBonusCalculator = new SprintBonusCalculator(config);
        this.knockbackApplicator = new KnockbackApplicator(config.knockbackConfig());
    }

    public static AttackFeature initialize(CombatConfig config) {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("AttackFeature");
            return instance;
        }

        instance = new AttackFeature(config);
        instance.registerListeners();
        instance.markInitialized();

        LogUtil.logInit("AttackFeature");
        return instance;
    }
    
    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();
        
        // Start tick counter for deduplication
        MinecraftServer.getSchedulerManager()
                .buildTask(() -> currentTick++)
                .repeat(TaskSchedule.tick(1))
                .schedule();
        
        // Start sprint tracking
        sprintBonusCalculator.startSprintTracking();
        
        // Remove attack cooldown on spawn if configured
        if (config.removeAttackCooldown()) {
            handler.addListener(PlayerSpawnEvent.class, event -> {
                removeAttackCooldown(event.getPlayer());
            });
        }
        
        // Client-side hit detection (primarily 1.8 clients)
        handler.addListener(EntityAttackEvent.class, this::handleAttackPacket);
        
        // Server-side hit detection (for 1.21+ clients with strict raycasting)
        handler.addListener(PlayerHandAnimationEvent.class, this::handleSwing);
        
    }
    
    /**
     * Handles attack packets from clients (primarily 1.8).
     * Client already decided this was a hit, just validate and process.
     */
    private void handleAttackPacket(EntityAttackEvent event) {
        if (!(event.getEntity() instanceof Player attacker)) return;
        if (!(event.getTarget() instanceof LivingEntity victim)) return;
        
        UUID attackerId = attacker.getUuid();
        
        // Validate reach
        HitDetection hitDetection = HitDetection.getInstance();
        if (!hitDetection.isReachValid(attacker, victim)) return;
        
        // Mark this attack to prevent duplicate from swing
        lastAttackTime.put(attackerId, System.currentTimeMillis());
        this.lastAttackTick.put(attackerId, currentTick);
        
        // Process the attack
        processAttack(attacker, victim);
    }
    
    /**
     * Handles swings for server-side hit detection.
     * This catches attacks that 1.21 clients don't send packets for.
     */
    private void handleSwing(PlayerHandAnimationEvent event) {
        Player attacker = event.getPlayer();
        UUID attackerId = attacker.getUuid();
        
        // Skip if we just processed an attack packet (dedupe for 1.8 clients)
        Long lastAttack = lastAttackTime.get(attackerId);
        if (lastAttack != null && System.currentTimeMillis() - lastAttack < ATTACK_DEDUPE_MS) {
            return;
        }
        
        // ✅ SIMPLE FIX: Block all swings with projectile items
        // Players can still get hits from EntityAttackEvent, just no expanded hitbox
        if (isProjectileItem(attacker.getItemInMainHand())) {
            return; // Skip swings with projectiles
        }
        
        // Do server-side raycasting (returns null if aiming at blocks)
        HitDetection hitDetection = HitDetection.getInstance();
        LivingEntity target = hitDetection.findTargetFromSwing(attacker);
        if (target == null) return;
        
        // Validate reach (should always pass if findTargetFromSwing succeeded)
        if (!hitDetection.isReachValid(attacker, target)) return;
        
        // Mark this attack
        lastAttackTime.put(attackerId, System.currentTimeMillis());
        this.lastAttackTick.put(attackerId, currentTick);
        
        // Process the attack
        processAttack(attacker, target);
    }
    
    
    
    /**
     * Check if an item is a projectile item that causes false hits
     * Focus on the main culprits: eggs, snowballs, ender pearls, fishing rods
     */
    private boolean isProjectileItem(ItemStack stack) {
        if (stack == null || stack.isAir()) return false;
        Material mat = stack.material();
        return mat == Material.SNOWBALL ||
                mat == Material.EGG ||
                mat == Material.ENDER_PEARL ||
                mat == Material.FISHING_ROD;
                // Removed potions and XP bottles - they're less likely to cause false hits
    }

    /**
     * Core attack processing logic (used by both packet and swing handlers).
     */
    private void processAttack(Player attacker, LivingEntity victim) {
        // Calculate attack result
        AttackResult result = calculateAttack(attacker);
        
        // Apply damage
        Damage damage = new Damage(
                DamageType.PLAYER_ATTACK,
                attacker,
                attacker,
                attacker.getPosition(),
                result.damage()
        );
        
        if (!victim.damage(damage)) {
            return; // Damage was cancelled or victim is invulnerable
        }
        
        // Apply knockback if allowed
        if (shouldApplyKnockback(victim)) {
            knockbackApplicator.applyKnockback(
                    victim,
                    attacker,
                    KnockbackSystem.KnockbackType.ATTACK,
                    result.hadSprintBonus(),
                    0   // Placeholder for enchantment level
            );
        }
        
        // Debug logging
        logAttack(attacker, victim, result);
    }
    
    /**
     * Calculate damage and combat modifiers for this attack.
     */
    private AttackResult calculateAttack(Player attacker) {
        float baseDamage = attackCalculator.calculateBaseDamage(attacker);
        boolean isCritical = attackCalculator.isCriticalHit(attacker);
        boolean hadSprintBonus = attacker.isSprinting() || sprintBonusCalculator.wasRecentlySprinting(attacker);
        
        float finalDamage = attackCalculator.calculateFinalDamage(baseDamage, isCritical);
        
        return AttackResult.of(finalDamage, isCritical, hadSprintBonus);
    }

    private boolean shouldApplyKnockback(LivingEntity victim) {
        HealthSystem healthSystem = HealthSystem.getInstance();
        return healthSystem.shouldApplyKnockback(victim);
    }
    
    /**
     * Log attack details with precise ray distance from snapshot.
     */
    private void logAttack(Player attacker, LivingEntity victim, AttackResult result) {
        HitDetection hitDetection = HitDetection.getInstance();
        
        // Get the snapshot captured at moment of attack
        var snapshot = hitDetection.getLastHitSnapshot(victim);
        
        if (snapshot != null) {
            // We have precise data from the snapshot
            String tierLabel = switch (snapshot.tier) {
                case PRIMARY -> "✓";   // High confidence
                case LIMIT -> "~";     // Medium confidence
                case FALLBACK -> "≈";  // Low confidence (approximate)
            };
            
            log.debug("{} attacked {} for {} at {:.2f}b{} {}",
                    attacker.getUsername(),
                    GameplayUtils.getEntityName(victim),
                    result.getDescription(),
                    snapshot.rayDistance,
                    result.hadSprintBonus() ? " (SPRINT)" : "",
                    tierLabel);
        } else {
            // Fallback (shouldn't happen)
            double distance = hitDetection.getLastHitDistance(victim);
            log.debug("{} attacked {} for {} at {:.2f}b{}",
                    attacker.getUsername(),
                    GameplayUtils.getEntityName(victim),
                    result.getDescription(),
                    distance,
                    result.hadSprintBonus() ? " (SPRINT)" : "");
        }
    }
    
    
    private void removeAttackCooldown(Player player) {
        player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(1024.0);
    }
    
    // ===========================
    // CLEANUP
    // ===========================
    
    public void cleanup(Player player) {
        UUID playerId = player.getUuid();
        sprintBonusCalculator.cleanup(player);
        lastAttackTime.remove(playerId);
        lastAttackTick.remove(playerId);
    }
    
    public void shutdown() {
        sprintBonusCalculator.shutdown();
        lastAttackTime.clear();
        lastAttackTick.clear();
        log.info("AttackFeature shutdown complete");
    }
    
    // ===========================
    // PUBLIC API
    // ===========================
    
    public static AttackFeature getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AttackFeature not initialized!");
        }
        return instance;
    }

    public CombatConfig getConfig() {
        return config;
    }
}

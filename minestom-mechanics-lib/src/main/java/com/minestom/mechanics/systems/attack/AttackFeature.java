package com.minestom.mechanics.systems.attack;

import com.minestom.mechanics.systems.blocking.BlockingSystem;
import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;
import com.minestom.mechanics.systems.knockback.KnockbackApplicator;
import com.minestom.mechanics.systems.knockback.KnockbackSystem;
import com.minestom.mechanics.systems.validation.hits.HitDetection;
import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.systems.misc.GameplayUtils;
import com.minestom.mechanics.systems.projectile.utils.ProjectileMaterials;
import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
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

// TODO: Literally all this has to do is hit detection + pass an "Attack Event" to damage system

// Structure: Remove attack system entirely, replace with hit detection.

// Hit detection passes player attacker and player victim to damage system
// Damage system calculates damage to victim (and to attacker if thorns?)
// Damage system passes player attacker and player victim to knockback system
// Knockback system determines knockback (handles sprinting buffer etc)

/**
 * Main attack feature that orchestrates all attack-related components.
 * Replaces the monolithic CombatSystem with focused responsibilities.
 */
public class AttackFeature extends InitializableSystem {
    private static AttackFeature instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("AttackFeature");

    private final CombatConfig config;
    private final AttackCalculator attackCalculator;
    private final SprintBonusCalculator sprintBonusCalculator;
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

    // Begin hit detection
    /**
     * Handles attack packets from clients (primarily 1.8).
     * Client already decided this was a hit, just validate and process.
     */
    private void handleAttackPacket(EntityAttackEvent event) {
        if (!(event.getEntity() instanceof Player attacker)) return;
        if (!(event.getTarget() instanceof LivingEntity victim)) return;

        // I think this is unnecessary
        if (isBlocking(attacker)) {
            BlockingSystem.getInstance().stopBlocking(attacker);
            return;
        }

        HitDetection hitDetection = HitDetection.getInstance();
        if (!hitDetection.isReachValid(attacker, victim)) return;

        processAttack(attacker, victim);
    }
    
    /**
     * Handles swings for server-side hit detection.
     * Only modern clients use this path; legacy clients use attack packets only.
     */
    private void handleSwing(PlayerHandAnimationEvent event) {
        Player attacker = event.getPlayer();
        if (ClientVersionDetector.getInstance().getClientVersion(attacker) != ClientVersionDetector.ClientVersion.MODERN) {
            return; // Legacy/unknown: hits only via attack packets
        }

        // I think this is unnecessary
        if (isBlocking(attacker)) {
            BlockingSystem.getInstance().stopBlocking(attacker);
            return;
        }

        ItemStack mainHand = attacker.getItemInMainHand();
        if (mainHand != null && !mainHand.isAir()) {
            var mat = mainHand.material();
            if (ProjectileMaterials.isFishingRod(mat) || ProjectileMaterials.isThrowable(mat)) {
                return;
            }
        }

        HitDetection hitDetection = HitDetection.getInstance();
        LivingEntity target = hitDetection.findTargetFromSwing(attacker);
        if (target == null) return;

        if (!hitDetection.isReachValid(attacker, target)) return;

        processAttack(attacker, target);
    }

    private boolean isBlocking(Player player) {
        try {
            return BlockingSystem.getInstance().isBlocking(player);
        } catch (IllegalStateException e) {
            return false;
        }
    }
    // End hit detection

    // Begin damage system
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

        if (!HealthSystem.applyDamage(victim, damage)) {
            return; // Damage was cancelled or victim is invulnerable
        }
        
        // Apply knockback if allowed
        if (shouldApplyKnockback(victim)) {
            knockbackApplicator.applyKnockback(
                    victim,
                    attacker,
                    KnockbackSystem.KnockbackType.ATTACK,
                    result.hadSprintBonus(),
                    0   // Placeholder for enchantment level TODO: Update this?? Aren't we going to use tags?
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
        try {
            HealthSystem hs = HealthSystem.getInstance();
            if (!hs.wasLastDamageReplacement(victim)) return true;
            // Was a replacement hit — check if knockback applies for this type
            var dt = com.minestom.mechanics.systems.health.damage.DamageType.find(DamageType.PLAYER_ATTACK);
            DamageTypeProperties props = dt != null
                    ? dt.resolveProperties(null, victim, null)
                    : DamageTypeProperties.ATTACK_DEFAULT;
            return props.knockbackOnReplacement();
        } catch (IllegalStateException ignored) {
            return true; // HealthSystem not initialized, allow knockback
        }
    }
    // End damage system

    // Hit detection / validation
    /**
     * Log attack details with precise ray distance from snapshot.
     */
    // TODO: Clean up this logging, it's ugly right now
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
    
    // TODO: Move to compatibility package
    private void removeAttackCooldown(Player player) {
        player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(1024.0);
    }
    
    // ===========================
    // CLEANUP
    // ===========================
    
    public void cleanup(Player player) {
        sprintBonusCalculator.cleanup(player);
    }

    public void shutdown() {
        sprintBonusCalculator.shutdown();
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

package com.minestom.mechanics.systems.attack;

import com.minestom.mechanics.systems.blocking.BlockingSystem;
import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import com.minestom.mechanics.systems.health.HealthSystem;
// HitDetection is now in this package
import com.minestom.mechanics.config.combat.CombatConfig;
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
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.item.ItemStack;

/**
 * Hit detection system. Handles attack packets and server-side swing detection,
 * then passes attacker + victim to the damage system.
 *
 * <p>Does NOT calculate damage, apply knockback, or manage combat state.
 * All of that flows through {@link HealthSystem#applyDamage}.</p>
 */
public class AttackFeature extends InitializableSystem {
    private static AttackFeature instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("AttackFeature");

    private final CombatConfig config;

    private AttackFeature(CombatConfig config) {
        this.config = config;
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

        if (config.removeAttackCooldown()) {
            handler.addListener(PlayerSpawnEvent.class, event ->
                    event.getPlayer().getAttribute(Attribute.ATTACK_SPEED).setBaseValue(1024.0));
        }

        // Client-side hit detection (primarily 1.8 clients)
        handler.addListener(EntityAttackEvent.class, this::handleAttackPacket);

        // Server-side hit detection (swing animations)
        handler.addListener(PlayerHandAnimationEvent.class, this::handleSwing);

        // Swing window: poll look direction after each swing (hit lands when crosshair passes over victim)
        if (config.swingHitWindowTicks() > 0 && config.swingLookCheckTicks() > 0) {
            handler.addListener(PlayerTickEvent.class, this::handleSwingLookCheck);
        }

        // Record attacker-victim for swing window when damage lands
        HealthSystem.getInstance().addAttackLandedListener(this::onAttackLanded);
    }

    /** Set before processAttack when the hit is from swing window — prevents recording (only initial melee/projectile hits count). */
    private static final ThreadLocal<Boolean> FROM_SWING_WINDOW = ThreadLocal.withInitial(() -> false);

    private void onAttackLanded(Player attacker, LivingEntity victim, long tick) {
        if (Boolean.TRUE.equals(FROM_SWING_WINDOW.get())) return;
        if (config.swingHitWindowTicks() > 0) {
            SwingWindowTracker.recordHit(attacker, victim, tick);
            log.debug("Swing window: recorded hit {} -> {} at tick {}", attacker.getUsername(), victim.getEntityType(), tick);
        }
    }

    // ===========================
    // HIT DETECTION
    // ===========================

    private void handleAttackPacket(EntityAttackEvent event) {
        if (!(event.getEntity() instanceof Player attacker)) return;
        if (!(event.getTarget() instanceof LivingEntity victim)) return;

        if (isBlocking(attacker)) {
            BlockingSystem.getInstance().stopBlocking(attacker);
            return;
        }

        HitDetection hitDetection = HitDetection.getInstance();
        if (!hitDetection.isReachValid(attacker, victim)) return;

        processAttack(attacker, victim);
    }

    private void handleSwing(PlayerHandAnimationEvent event) {
        Player attacker = event.getPlayer();

        if (isBlocking(attacker)) {
            BlockingSystem.getInstance().stopBlocking(attacker);
            return;
        }

        ItemStack mainHand = attacker.getItemInMainHand();
        if (mainHand != null && !mainHand.isAir()) {
            var mat = mainHand.material();
            if (ProjectileMaterials.isFishingRod(mat) || ProjectileMaterials.isThrowable(mat)) return;
        }

        HitDetection hitDetection = HitDetection.getInstance();
        long tick = HealthSystem.getInstance().getCurrentTick();

        // Swing window: record swing for look-check; if swingLookCheckTicks==0, check immediately at swing moment
        if (config.swingHitWindowTicks() > 0) {
            SwingWindowTracker.recordSwing(attacker, tick);
            var recentVictims = SwingWindowTracker.getRecentVictims(attacker, tick, config.swingHitWindowTicks());
            if (config.swingLookCheckTicks() == 0) {
                // Check ray only at swing moment
                for (LivingEntity victim : recentVictims) {
                    if (hitDetection.isLookHittingVictimInSwingWindow(attacker, victim) && hitDetection.isReachValid(attacker, victim)) {
                        try {
                            FROM_SWING_WINDOW.set(true);
                            processAttack(attacker, victim);
                        } finally {
                            FROM_SWING_WINDOW.remove();
                        }
                        return;
                    }
                }
            } else if (!recentVictims.isEmpty()) {
                // Tick handler will poll look each tick — don't run findTargetFromSwing (avoids double-hit)
                return;
            }
        }

        // Modern clients: raycast to find target (extended hitbox)
        if (ClientVersionDetector.getInstance().getClientVersion(attacker) == ClientVersionDetector.ClientVersion.MODERN) {
            LivingEntity target = hitDetection.findTargetFromSwing(attacker);
            if (target != null && hitDetection.isReachValid(attacker, target)) {
                processAttack(attacker, target);
            }
        }
    }

    /**
     * Each tick: for attackers with recent victims and an unconsumed swing, check if look ray hits victim.
     * Hit lands when crosshair passes over victim during the look-check window (not just at swing moment).
     */
    private void handleSwingLookCheck(PlayerTickEvent event) {
        Player attacker = event.getPlayer();
        if (isBlocking(attacker)) return;

        long tick = HealthSystem.getInstance().getCurrentTick();
        if (!SwingWindowTracker.hasUnconsumedSwing(attacker, tick, config.swingLookCheckTicks())) return;

        var recentVictims = SwingWindowTracker.getRecentVictims(attacker, tick, config.swingHitWindowTicks());
        if (recentVictims.isEmpty()) return;

        HitDetection hitDetection = HitDetection.getInstance();
        for (LivingEntity victim : recentVictims) {
            if (hitDetection.isLookHittingVictimInSwingWindow(attacker, victim) && hitDetection.isReachValid(attacker, victim)) {
                SwingWindowTracker.consumeSwing(attacker);
                try {
                    FROM_SWING_WINDOW.set(true);
                    processAttack(attacker, victim);
                } finally {
                    FROM_SWING_WINDOW.remove();
                }
                return;
            }
        }
    }

    // ===========================
    // DAMAGE HANDOFF
    // ===========================

    /**
     * Pass attacker + victim to the damage system. The damage pipeline handles:
     * weapon damage calculation, crits, multipliers, i-frames, replacement, and knockback.
     */
    private void processAttack(Player attacker, LivingEntity victim) {
        Damage damage = new Damage(DamageType.PLAYER_ATTACK, attacker, attacker,
                attacker.getPosition(), 0); // base = 0, DamageCalculator resolves weapon damage
        HealthSystem.applyDamage(victim, damage);
    }

    // ===========================
    // HELPERS
    // ===========================

    private boolean isBlocking(Player player) {
        try {
            return BlockingSystem.getInstance().isBlocking(player);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    // ===========================
    // LIFECYCLE
    // ===========================

    public void cleanup(Player player) {
        // SprintBonusCalculator cleanup will move to knockback system
    }

    public void shutdown() {
        log.info("AttackFeature shutdown complete");
    }

    public static AttackFeature getInstance() {
        if (instance == null) throw new IllegalStateException("AttackFeature not initialized!");
        return instance;
    }

    public CombatConfig getConfig() { return config; }
}

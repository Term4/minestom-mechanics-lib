package com.minestom.mechanics.manager;

import com.minestom.mechanics.systems.attack.AttackFeature;
import com.minestom.mechanics.config.combat.*;
import com.minestom.mechanics.systems.blocking.BlockingSystem;

/**
 * Combat Manager - Manages player vs player combat mechanics
 *
 * Manages:
 * - AttackFeature (player attacks and damage calculations)
 * - BlockingSystem (shield blocking mechanics)
 */
public class CombatManager extends AbstractManager<CombatManager> {
    private static CombatManager instance;

    private CombatConfig currentConfig;
    private AttackFeature attackFeature;
    private BlockingSystem blockingSystem;

    private CombatManager() {
        super("CombatManager");
    }

    public static CombatManager getInstance() {
        if (instance == null) {
            instance = new CombatManager();
        }
        return instance;
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public CombatManager initialize(CombatConfig config) {
        this.currentConfig = config;

        return initializeWithWrapper(() -> {
            // Initialize and register systems
            blockingSystem = BlockingSystem.initialize(config);
            registerSystem(blockingSystem, "BlockingSystem");

            attackFeature = AttackFeature.initialize(config);
            registerSystem(attackFeature, "AttackFeature");
        });
    }

    public boolean initialize() {
        return initialize(CombatPresets.MINEMEN).isInitialized();
    }

    @Override
    protected String getSystemName() {
        return "CombatManager";
    }

    @Override
    protected void logMinimalConfig() {
        log.debug("Attack Cooldown: {}", currentConfig.removeAttackCooldown() ? "removed" : "1.9+");
        log.debug("Critical Multiplier: {}x", currentConfig.criticalMultiplier());
        log.debug("Sprint Crits: {}", currentConfig.allowSprintCrits() ? "enabled" : "disabled");
        log.debug("Knockback: H={}, V={}",
                currentConfig.knockbackConfig().horizontal(),
                currentConfig.knockbackConfig().vertical());
        log.debug("Sprint Window: {} ticks ({})",
                currentConfig.sprintWindowTicks(),
                currentConfig.sprintWindowTicks() == 0 ? "disabled" : "enabled");
        log.debug("Blocking: {} ({}% reduction)",
                currentConfig.blockDamageReduction() > 0 ? "enabled" : "disabled",
                (int)(currentConfig.blockDamageReduction() * 100));
        log.debug("Fix 1.8 Hit Slowdown: {}", currentConfig.fix18HitSlowdown() ? "enabled" : "disabled");
    }

    // cleanup(), shutdown(), getStatus() - ALL AUTO-HANDLED by AbstractManager!

    // ===========================
    // RUNTIME OPERATIONS
    // ===========================

    /**
     * Get the current combat configuration.
     * Used by other systems that need access to combat config (like ProjectileManager).
     */
    public CombatConfig getCombatConfig() {
        requireInitialized();
        return currentConfig;
    }

    public void setBlockingEnabled(boolean enabled) {
        requireInitialized();
        blockingSystem.setRuntimeEnabled(enabled);
        log.info("Blocking {}", enabled ? "enabled" : "disabled");
    }

    public AttackFeature getAttackFeature() {
        requireInitialized();
        return attackFeature;
    }

    public BlockingSystem getBlockingSystem() {
        requireInitialized();
        return blockingSystem;
    }

    public CombatConfig getCurrentConfig() {
        return currentConfig;
    }
}
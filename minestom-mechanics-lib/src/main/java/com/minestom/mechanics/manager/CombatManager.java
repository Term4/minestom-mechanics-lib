package com.minestom.mechanics.manager;

import com.minestom.mechanics.attack.AttackFeature;
import com.minestom.mechanics.config.combat.*;
import com.minestom.mechanics.features.blocking.BlockingSystem;
import com.minestom.mechanics.config.blocking.BlockingConfig;

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
            // Create blocking config from CombatConfig fields
            BlockingConfig blockingConfig = BlockingConfig.builder()
                    .enabled(true)
                    .damageReduction(config.blockDamageReduction())
                    .knockbackHorizontalMultiplier(config.blockKnockbackHorizontal())
                    .knockbackVerticalMultiplier(config.blockKnockbackVertical())
                    .showDamageMessages(config.showBlockDamageMessages())
                    .showBlockEffects(config.showBlockEffects())
                    .build();

            // Initialize and register systems
            blockingSystem = BlockingSystem.initialize(blockingConfig);
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
        log.debug("Sprint Window: {}{}",
                currentConfig.dynamicSprintWindow() ? "dynamic" : "static",
                currentConfig.sprintWindowDouble() ? " (double hit)" : "");
        log.debug("Blocking: {} ({}% reduction)",
                currentConfig.blockDamageReduction() > 0 ? "enabled" : "disabled",
                (int)(currentConfig.blockDamageReduction() * 100));
    }

    // cleanup(), shutdown(), getStatus() - ALL AUTO-HANDLED by AbstractManager!

    // ===========================
    // RUNTIME OPERATIONS
    // ===========================

    public void setBlockingEnabled(boolean enabled) {
        requireInitialized();
        blockingSystem.setEnabled(enabled);
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
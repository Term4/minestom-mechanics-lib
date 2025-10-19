package com.minestom.mechanics.features.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.features.knockback.components.*;
import com.minestom.mechanics.features.knockback.sync.KnockbackSyncHandler;
import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;

import java.util.UUID;

import static com.minestom.mechanics.config.combat.CombatConstants.*;

/**
 * Main knockback system orchestrator - coordinates all knockback components.
 * ✅ REFACTORED: Now uses focused component architecture instead of monolithic design.
 */
public class KnockbackHandler extends InitializableSystem {

    private static KnockbackHandler instance;

    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackHandler");

    public enum KnockbackType {
        ATTACK, DAMAGE, SWEEPING, EXPLOSION, PROJECTILE
    }

    // Component references
    private final KnockbackCalculator calculator;
    private final KnockbackModifier modifier;
    private final KnockbackStateManager stateManager;
    private final KnockbackVelocityHandler velocityHandler;

    // Core configuration
    private KnockbackConfig currentConfig;
    private boolean knockbackSyncEnabled = false;

    // Custom overrides
    private Double customAirHorizontalMultiplier = null;
    private Double customAirVerticalMultiplier = null;
    private Double customLookWeight = null;

    private KnockbackHandler(KnockbackConfig config) {
        this.currentConfig = config;

        // Initialize components
        this.stateManager = new KnockbackStateManager();
        this.calculator = new KnockbackCalculator(config);
        this.modifier = new KnockbackModifier();
        this.velocityHandler = new KnockbackVelocityHandler(stateManager, knockbackSyncEnabled);
    }

    // ===========================
    // INITIALIZATION (STANDARDIZED!)
    // ===========================

    public static KnockbackHandler initialize(KnockbackConfig config) {
        if (instance != null && instance.initialized) {
            LogUtil.logAlreadyInitialized("KnockbackHandler");
            return instance;
        }

        instance = new KnockbackHandler(config);
        instance.registerEventHandlers();
        instance.markInitialized();

        log.debug("Initialized with config: modern={}, sync={}",
                config.modern(), config.knockbackSyncSupported());

        return instance;
    }

    // TODO: Remember what I said earlier about memory leaks from enabling wayyy too many listeners

    private void registerEventHandlers() {
        MinestomVelocityFix.initialize();

        var eventHandler = MinecraftServer.getGlobalEventHandler();

        eventHandler.addListener(PlayerTickEvent.class, event -> {
            stateManager.trackGroundState(event.getPlayer());
            if (knockbackSyncEnabled) {
                KnockbackSyncHandler.getInstance().updatePlayerState(event.getPlayer());
            }
        });

        eventHandler.addListener(PlayerDisconnectEvent.class, event -> {
            stateManager.removePlayerData(event.getPlayer());
            if (knockbackSyncEnabled) {
                KnockbackSyncHandler.getInstance().removePlayer(event.getPlayer());
            }
        });

        eventHandler.addListener(ServerTickMonitorEvent.class, _ -> {
            // Clear knockback tracking every tick
            stateManager.clearKnockbackTracking();
        });

        if (knockbackSyncEnabled) {
            KnockbackSyncHandler.getInstance().initialize();
        }
    }

    public static KnockbackHandler getInstance() {
        if (instance == null || !instance.initialized) {
            throw new IllegalStateException("KnockbackHandler not initialized!");
        }
        return instance;
    }

    // ===========================
    // KNOCKBACK APPLICATION (FULL LOGIC!)
    // ===========================

    public void applyKnockback(LivingEntity victim, Entity attacker, KnockbackType type, boolean wasSprinting) {
        requireInitialized();

        // Prevent double knockback in same tick
        if (!stateManager.canReceiveKnockback((Player) victim)) {
            return;
        }

        // Update combat state
        if (victim instanceof Player player) {
            stateManager.updateCombatState(player);
        }

        // Calculate knockback components using focused components
        Vec knockbackDirection = calculator.calculateKnockbackDirection(victim, attacker);
        KnockbackCalculator.KnockbackStrength strength = calculator.calculateKnockbackStrength(victim, attacker, type, wasSprinting);
        
        // Convert to KnockbackHandler.KnockbackStrength for compatibility
        KnockbackStrength handlerStrength = new KnockbackStrength(strength.horizontal, strength.vertical);
        
        // Apply modifiers
        KnockbackStrength modifiedStrength = modifier.applyBlockingReduction(victim, handlerStrength);
        modifiedStrength = modifier.applyAirMultipliers(victim, modifiedStrength, 
                getAirHorizontalMultiplier(), getAirVerticalMultiplier());
        modifiedStrength = modifier.applyFallingModifiers(victim, modifiedStrength);

        // Apply velocity using velocity handler
        velocityHandler.applyKnockbackVelocity(victim, attacker, knockbackDirection, modifiedStrength, type);
    }


    // THIS IS ENTIRELY UNTESTED, also could probably be consolidated into one applyKnockback class...
    /**
     * Apply knockback with knockback enchantment level.
     * This is a convenience method for the KnockbackManager.
     * 
     * @param victim The player receiving knockback
     * @param attacker The entity causing knockback (can be null for non-entity sources)
     * @param sprintBonus Whether the attacker was sprinting (adds extra knockback)
     * @param kbEnchantLevel Knockback enchantment level (0 = no enchantment)
     */
    public void applyKnockback(Player victim, Entity attacker, boolean sprintBonus, int kbEnchantLevel) {
        requireInitialized();
        
        // Determine knockback type based on enchantment level
        KnockbackType type = KnockbackType.ATTACK;
        if (kbEnchantLevel > 0) {
            // For now, treat all knockback enchantments as attack type
            // Could be extended to have different types based on level
            type = KnockbackType.ATTACK;
        }
        
        // Apply knockback with the determined type
        applyKnockback(victim, attacker, type, sprintBonus);
        
        // Apply additional knockback for enchantment
        if (kbEnchantLevel > 0 && attacker != null) {
            // Calculate additional knockback from enchantment
            Vec direction = calculator.calculateKnockbackDirection(victim, attacker);
            double enchantmentBonus = kbEnchantLevel * 0.5; // Each level adds 0.5 blocks of knockback
            
            Vec additionalVelocity = direction.normalize().mul(enchantmentBonus, 0, enchantmentBonus);
            victim.setVelocity(victim.getVelocity().add(additionalVelocity));
        }
    }

    // TODO: Projectile knockback SHOULD be in the knnockback feature, just not this class

    /**
     * Apply projectile knockback with full system integration.
     * Includes iframe checking, blocking support, and all knockback modifiers.
     * 
     * @param victim The entity being hit
     * @param projectile The projectile that hit
     * @param projectileOrigin The position where the projectile was shot from
     * @param horizontalKnockback Horizontal knockback strength
     * @param verticalKnockback Vertical knockback strength
     * @param punchLevel Punch enchantment level (0 = no punch)
     */
    public void applyProjectileKnockback(LivingEntity victim, Entity projectile, 
                                       net.minestom.server.coordinate.Pos projectileOrigin,
                                       double horizontalKnockback, double verticalKnockback, int punchLevel) {
        requireInitialized();

        // Prevent double knockback in same tick
        if (victim instanceof Player player && !stateManager.canReceiveKnockback(player)) {
            return;
        }

        // Update combat state
        if (victim instanceof Player player) {
            stateManager.updateCombatState(player);
        }

        // Calculate knockback direction from projectile origin to victim
        Vec knockbackDirection = calculateProjectileKnockbackDirection(victim, projectileOrigin);

        // Create knockback strength with projectile values
        KnockbackStrength strength = new KnockbackStrength(horizontalKnockback, verticalKnockback);

        // Apply Punch enchantment bonus if present
        if (punchLevel > 0) {
            double horizontalMagnitude = Math.sqrt(knockbackDirection.x() * knockbackDirection.x() + 
                                                  knockbackDirection.z() * knockbackDirection.z());
            if (horizontalMagnitude > 0.0001) {
                double punchBonus = punchLevel * 0.6;
                strength = new KnockbackStrength(
                    strength.horizontal + punchBonus,
                    strength.vertical + 0.1
                );
            }
        }

        // Apply modifiers (blocking, air, falling)
        KnockbackStrength modifiedStrength = modifier.applyBlockingReduction(victim, strength);
        modifiedStrength = modifier.applyAirMultipliers(victim, modifiedStrength, 
                getAirHorizontalMultiplier(), getAirVerticalMultiplier());
        modifiedStrength = modifier.applyFallingModifiers(victim, modifiedStrength);

        // Apply velocity using velocity handler
        velocityHandler.applyKnockbackVelocity(victim, projectile, knockbackDirection, modifiedStrength, KnockbackType.PROJECTILE);
    }

    // TODO: Move out to a knockback math specific class.

    /**
     * Calculate knockback direction for projectiles (horizontal only).
     */
    private Vec calculateProjectileKnockbackDirection(LivingEntity victim, net.minestom.server.coordinate.Pos projectileOrigin) {
        double dx = victim.getPosition().x() - projectileOrigin.x();
        double dz = victim.getPosition().z() - projectileOrigin.z();
        
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        // If too close, use random direction
        if (distance < 0.0001) {
            dx = Math.random() * 0.02 - 0.01;
            dz = Math.random() * 0.02 - 0.01;
            distance = Math.sqrt(dx * dx + dz * dz);
        }
        
        dx /= distance;
        dz /= distance;
        
        return new Vec(dx, 0, dz);
    }

    // ===========================
    // DELEGATED TO COMPONENTS
    // ===========================
    // All calculation logic moved to focused components:
    // - KnockbackCalculator: Direction and strength calculations
    // - KnockbackModifier: Blocking, air, and falling modifiers  
    // - KnockbackStateManager: Player state tracking
    // - KnockbackVelocityHandler: Velocity application

    // ===========================
    // CONFIGURATION
    // ===========================

    public void setConfig(KnockbackConfig config) {
        requireInitialized();
        this.currentConfig = config;
        this.knockbackSyncEnabled = config.knockbackSyncSupported() && knockbackSyncEnabled;
        log.debug("Config updated: modern={}, sync={}", config.modern(), config.knockbackSyncSupported());
    }

    /**
     * Set knockback config (alias for setConfig for KnockbackManager compatibility)
     */
    public void setKnockbackConfig(KnockbackConfig config) {
        setConfig(config);
    }

    public KnockbackConfig getCurrentConfig() {
        requireInitialized();
        return currentConfig;
    }

    public void setKnockbackSyncEnabled(boolean enabled) {
        requireInitialized();
        if (enabled && !currentConfig.knockbackSyncSupported()) {
            log.warn("Cannot enable sync - not supported by current config (modern={}, syncSupported={})",
                    currentConfig.modern(), currentConfig.knockbackSyncSupported());
            return;
        }

        this.knockbackSyncEnabled = enabled;

        if (enabled) {
            KnockbackSyncHandler.getInstance().initialize();
            KnockbackSyncHandler.getInstance().setEnabled(true);
            log.debug("Knockback sync enabled");
        } else {
            KnockbackSyncHandler.getInstance().setEnabled(false);
            log.debug("Knockback sync disabled");
        }
    }

    public boolean isKnockbackSyncEnabled() {
        return knockbackSyncEnabled && currentConfig.knockbackSyncSupported();
    }

    public boolean hasCustomAirMultipliers() {
        return customAirHorizontalMultiplier != null || customAirVerticalMultiplier != null;
    }

    public void resetAirMultipliers() {
        customAirHorizontalMultiplier = null;
        customAirVerticalMultiplier = null;
        log.debug("Reset custom air multipliers");
    }

    public void setCustomAirMultipliers(double horizontal, double vertical) {
        customAirHorizontalMultiplier = horizontal;
        customAirVerticalMultiplier = vertical;
        log.debug("Set custom air multipliers: h={}, v={}", horizontal, vertical);
    }

    public double getAirHorizontalMultiplier() {
        return customAirHorizontalMultiplier != null ?
                customAirHorizontalMultiplier : currentConfig.airMultiplierHorizontal();
    }

    public double getAirVerticalMultiplier() {
        return customAirVerticalMultiplier != null ?
                customAirVerticalMultiplier : currentConfig.airMultiplierVertical();
    }

    public double getLookWeight() {
        return customLookWeight != null ?
                customLookWeight : currentConfig.lookWeight();
    }

    public int getTrackedPlayers() {
        return stateManager.getTrackedPlayers();
    }

    // ===========================
    // PLAYER DATA
    // ===========================

    public PlayerKnockbackData getOrCreatePlayerData(Player player) {
        return stateManager.getOrCreatePlayerData(player);
    }

    public PlayerKnockbackData getPlayerData(Player player) {
        return stateManager.getPlayerData(player);
    }

    public void removePlayerData(Player player) {
        stateManager.removePlayerData(player);
    }
    
    /**
     * Sync knockback for a player (used by KnockbackManager)
     */
    public void syncKnockback(Player player) {
        if (knockbackSyncEnabled && KnockbackSyncHandler.getInstance().isEnabled()) {
            // Get the last knockback applied to this player
            PlayerKnockbackData data = getPlayerData(player);
            if (data != null && data.lastKnockback != null) {
                KnockbackSyncHandler.getInstance().recordKnockback(player, data.lastKnockback);
            }
        }
    }

    public void cleanup(Player player) {
        stateManager.removePlayerData(player);
    }

    public void shutdown() {
        log.debug("Shutting down KnockbackHandler");

        // Cleanup all players
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            stateManager.removePlayerData(player);
        }

        // Shutdown components
        stateManager.shutdown();

        log.debug("KnockbackHandler shutdown complete");
    }

    // ===========================
    // INNER CLASSES
    // ===========================

    public static class PlayerKnockbackData {
        public final UUID uuid;
        public Vec lastKnockback = Vec.ZERO;
        public long lastKnockbackTime = 0;
        public long lastCombatTime = 0;
        public double lastPing = 0;

        public PlayerKnockbackData(UUID uuid) {
            this.uuid = uuid;
        }

        /**
         * Check if player is in combat.
         * ✅ REFACTORED: Using COMBAT_TIMEOUT_MS constant
         */
        public boolean isInCombat() {
            return System.currentTimeMillis() - lastCombatTime < COMBAT_TIMEOUT_MS;
        }
    }

    // TODO: this is duplicate code inn KnockbackCalculator.

    public static class KnockbackStrength {
        public final double horizontal;
        public final double vertical;

        public KnockbackStrength(double horizontal, double vertical) {
            this.horizontal = horizontal;
            this.vertical = vertical;
        }
    }
}

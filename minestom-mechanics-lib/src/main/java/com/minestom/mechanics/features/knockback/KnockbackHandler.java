package com.minestom.mechanics.features.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.features.knockback.components.*;
import com.minestom.mechanics.features.knockback.sync.KnockbackSyncHandler;
import com.minestom.mechanics.util.ConfigurableSystem;
import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.ProjectileTagRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.tag.Tag;

import java.util.List;
import java.util.UUID;

import static com.minestom.mechanics.config.combat.CombatConstants.*;

/**
 * Main knockback system orchestrator - coordinates all knockback components.
 * ✅ IMPLEMENTS: Universal Mechanics Configuration System
 *
 * <p><b>MULTIPLIER Array Indices:</b></p>
 * <ul>
 *   <li>0: horizontal</li>
 *   <li>1: vertical</li>
 *   <li>2: sprint bonus horizontal</li>
 *   <li>3: sprint bonus vertical</li>
 *   <li>4: air multiplier horizontal</li>
 *   <li>5: air multiplier vertical</li>
 * </ul>
 */
public class KnockbackHandler extends ConfigurableSystem<KnockbackConfig> {

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
        super(config); // Pass to ConfigurableSystem

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
        instance.registerEventHandlers();  // ← Make sure this is being called!
        instance.markInitialized();

        // ✅ Register for projectile tags
        ProjectileTagRegistry.register(KnockbackHandler.class);

        LogUtil.logInit("KnockbackHandler");
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
    // UNIVERSAL CONFIG SYSTEM TAGS
    // ===========================

    /**
     * Multiply all knockback components by this value.
     * Can be set on: World (Instance), Player, ItemStack
     */
    public static final Tag<List<Double>> MULTIPLIER = Tag.Double("knockback_multiplier").list();

    /**
     * Modify specific knockback components.
     * Format: [horizontal, vertical, sprintBonusH, sprintBonusV, airMultH, airMultV]
     * Use 0.0 for components you don't want to change.
     * Can be set on: World (Instance), Player, ItemStack
     */
    public static final Tag<List<Double>> MODIFY = Tag.Double("knockback_modify").list();

    /**
     * Override with a custom knockback config entirely.
     * Can be set on: World (Instance), Player, ItemStack
     */
    public static final Tag<KnockbackConfig> CUSTOM = Tag.Transient("knockback_custom");

    // Projectile tags
    public static final Tag<List<Double>> PROJECTILE_MULTIPLIER = Tag.Double("knockback_projectile_multiplier").list();
    public static final Tag<List<Double>> PROJECTILE_MODIFY = Tag.Double("knockback_projectile_modify").list();
    public static final Tag<KnockbackConfig> PROJECTILE_CUSTOM = Tag.Transient("knockback_projectile_custom");

    /** Number of components in MODIFY array */
    private static final int COMPONENT_COUNT = 6;

    // ===========================
    // CONFIGURABLE SYSTEM IMPLEMENTATION
    // ===========================

    @Override
    protected Tag<List<Double>> getMultiplierTag() {
        return MULTIPLIER;
    }

    @Override
    protected Tag<List<Double>> getModifyTag() {
        return MODIFY;
    }

    @Override
    protected Tag<KnockbackConfig> getCustomTag() {
        return CUSTOM;
    }

    // NEW: Projectile tag getters
    @Override
    protected Tag<List<Double>> getProjectileMultiplierTag() {
        return PROJECTILE_MULTIPLIER;
    }

    @Override
    protected Tag<List<Double>> getProjectileModifyTag() {
        return PROJECTILE_MODIFY;
    }

    @Override
    protected Tag<KnockbackConfig> getProjectileCustomTag() {
        return PROJECTILE_CUSTOM;
    }

    @Override
    protected int getModifyComponentCount() {
        return COMPONENT_COUNT;
    }

    // ===========================
    // KNOCKBACK APPLICATION (FULL LOGIC!)
    // ===========================

    public void applyKnockback(LivingEntity victim, Entity attacker, KnockbackType type, boolean wasSprinting) {
        requireInitialized();

        // Prevent double knockback in same tick
        if (victim instanceof Player player && !stateManager.canReceiveKnockback(player)) {
            return;
        }

        // Update combat state
        if (victim instanceof Player player) {
            stateManager.updateCombatState(player);
        }

        // ✅ NEW: Resolve knockback using Universal Config System
        // For melee: attacker is Player, handUsed is MAIN_HAND
        // For projectiles: attacker is Projectile, handUsed is null
        EquipmentSlot handUsed = (attacker instanceof Player && type != KnockbackType.PROJECTILE)
                ? EquipmentSlot.MAIN_HAND
                : null;

        KnockbackResult resolved = resolveKnockback(attacker, victim, handUsed);

        // Calculate knockback direction
        Vec knockbackDirection = calculator.calculateKnockbackDirection(victim, attacker);

        // Calculate base strength using resolved values
        double horizontal = resolved.horizontal;
        double vertical = resolved.vertical;

        // Apply sprint bonus if applicable (only for player melee)
        if (attacker instanceof Player player && wasSprinting &&
                (type == KnockbackType.ATTACK || type == KnockbackType.DAMAGE)) {
            horizontal += resolved.sprintBonusH;
            vertical += resolved.sprintBonusV;
            player.setSprinting(false);
            log.debug("Sprint bonus applied for: " + player.getUsername());
        }

        // Apply sweeping reduction
        if (type == KnockbackType.SWEEPING) {
            horizontal *= 0.5;
            vertical *= 0.5;
        }

        KnockbackStrength strength = new KnockbackStrength(horizontal, vertical);

        // Apply modifiers (blocking, air multipliers using resolved values, falling)
        KnockbackStrength modifiedStrength = modifier.applyBlockingReduction(victim, strength);
        modifiedStrength = modifier.applyAirMultipliers(
                victim, modifiedStrength, resolved.airMultH, resolved.airMultV);
        modifiedStrength = modifier.applyFallingModifiers(victim, modifiedStrength);

        // Apply velocity using velocity handler
        velocityHandler.applyKnockbackVelocity(victim, attacker, knockbackDirection,
                modifiedStrength, type);
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

    public KnockbackConfig getserverDefaultConfig() {
        requireInitialized();
        return serverDefaultConfig;
    }

    public void setKnockbackSyncEnabled(boolean enabled) {
        requireInitialized();
        if (enabled && !serverDefaultConfig.knockbackSyncSupported()) {
            log.warn("Cannot enable sync - not supported by current config (modern={}, syncSupported={})",
                    serverDefaultConfig.modern(), serverDefaultConfig.knockbackSyncSupported());
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
        return knockbackSyncEnabled && serverDefaultConfig.knockbackSyncSupported();
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
                customAirHorizontalMultiplier : serverDefaultConfig.airMultiplierHorizontal();
    }

    public double getAirVerticalMultiplier() {
        return customAirVerticalMultiplier != null ?
                customAirVerticalMultiplier : serverDefaultConfig.airMultiplierVertical();
    }

    public double getLookWeight() {
        return customLookWeight != null ?
                customLookWeight : serverDefaultConfig.lookWeight();
    }

    public int getTrackedPlayers() {
        return stateManager.getTrackedPlayers();
    }

    /**
     * Resolve effective knockback values with Universal Config System.
     *
     * @param attacker The attacking entity
     * @param victim The victim being knocked back
     * @param handUsed The hand that was used (MAIN_HAND for melee, may vary for projectiles)
     * @return Resolved knockback values
     */
    private KnockbackResult resolveKnockback(Entity attacker, LivingEntity victim, EquipmentSlot handUsed) {
        // Use inherited resolution with a lambda to extract components
        double[] components = resolveComponents(attacker, victim, handUsed, config -> new double[] {
                config.horizontal(),
                config.vertical(),
                config.sprintBonusHorizontal(),
                config.sprintBonusVertical(),
                config.airMultiplierHorizontal(),
                config.airMultiplierVertical()
        });

        // Also get the base config for other properties
        KnockbackConfig config = resolveBaseConfig(attacker, victim, handUsed);

        return new KnockbackResult(
                components[0], // horizontal
                components[1], // vertical
                components[2], // sprintBonusH
                components[3], // sprintBonusV
                components[4], // airMultH
                components[5], // airMultV
                config
        );
    }

    /**
     * Result record holding resolved knockback values and config.
     */
    private record KnockbackResult(
            double horizontal,
            double vertical,
            double sprintBonusH,
            double sprintBonusV,
            double airMultH,
            double airMultV,
            KnockbackConfig config
    ) {}

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

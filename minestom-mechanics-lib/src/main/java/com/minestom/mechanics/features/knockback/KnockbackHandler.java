package com.minestom.mechanics.features.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.features.knockback.components.*;
import com.minestom.mechanics.features.knockback.sync.KnockbackSyncHandler;
import com.minestom.mechanics.util.ConfigurableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.ProjectileTagRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
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

    /**
     * Apply melee knockback with optional enchantment support.
     *
     * @param victim The entity being hit
     * @param attacker The attacking entity
     * @param type Type of knockback (ATTACK, DAMAGE, SWEEPING, EXPLOSION)
     * @param wasSprinting Whether the attacker was sprinting
     * @param kbEnchantLevel Knockback enchantment level (0 = none, 1 = Knockback I, 2 = Knockback II)
     */
    public void applyKnockback(LivingEntity victim, Entity attacker,
                               KnockbackType type, boolean wasSprinting,
                               int kbEnchantLevel) {
        applyKnockbackInternal(victim, attacker, attacker.getPosition(),
                type, wasSprinting, kbEnchantLevel);
    }

    /**
     * Apply melee knockback without enchantments (convenience method).
     */
    public void applyKnockback(LivingEntity victim, Entity attacker,
                               KnockbackType type, boolean wasSprinting) {
        applyKnockback(victim, attacker, type, wasSprinting, 0);
    }

    /**
     * Legacy compatibility method for KnockbackManager.
     */
    @Deprecated
    public void applyKnockback(Player victim, Entity attacker,
                               boolean sprintBonus, int kbEnchantLevel) {
        applyKnockback(victim, attacker, KnockbackType.ATTACK, sprintBonus, kbEnchantLevel);
    }

    /**
     * Internal unified knockback logic for both melee and projectiles.
     *
     * @param victim The entity receiving knockback
     * @param attacker The attacking entity (Player for melee, Projectile for projectiles)
     * @param sourcePosition Position to calculate direction from (current pos for melee, origin for projectiles)
     * @param type Type of knockback
     * @param wasSprinting Whether attacker was sprinting (only relevant for melee)
     * @param kbEnchantLevel Knockback enchantment level (only relevant for melee)
     */
    private void applyKnockbackInternal(LivingEntity victim, Entity attacker,
                                        Pos sourcePosition, KnockbackType type,
                                        boolean wasSprinting, int kbEnchantLevel) {
        requireInitialized();

        // DEBUG: Log thread info
        log.debug("Applying knockback on thread: {}", Thread.currentThread().getName());

        // 2. Update combat state
        if (victim instanceof Player player) {
            stateManager.updateCombatState(player);
        }

        // 3. Resolve from Universal Config System
        EquipmentSlot handUsed = (attacker instanceof Player && type != KnockbackType.PROJECTILE)
                ? EquipmentSlot.MAIN_HAND
                : null;

        KnockbackResult resolved = resolveKnockback(attacker, victim, handUsed);

        // 4. Calculate direction
        Vec direction;
        if (type == KnockbackType.PROJECTILE) {
            direction = calculator.calculateProjectileKnockbackDirection(victim, sourcePosition);
        } else {
            direction = calculator.calculateKnockbackDirection(victim, attacker);
        }

        // 5. Build base strength from resolved values
        double horizontal = resolved.horizontal;
        double vertical = resolved.vertical;

        // Apply sprint bonus (melee only)
        if (attacker instanceof Player player && wasSprinting &&
                (type == KnockbackType.ATTACK || type == KnockbackType.DAMAGE)) {
            horizontal += resolved.sprintBonusH;
            vertical += resolved.sprintBonusV;
            player.setSprinting(false);
            log.debug("Sprint bonus applied for: {}", player.getUsername());
        }

        // Apply enchantment bonus (melee only, AFTER tags)
        if (kbEnchantLevel > 0 && type != KnockbackType.PROJECTILE) {
            horizontal += kbEnchantLevel * 0.6;
            vertical += 0.1;
            log.debug("Knockback {} applied: +{}h", kbEnchantLevel, kbEnchantLevel * 0.6);
        }

        // Sweeping reduction
        if (type == KnockbackType.SWEEPING) {
            horizontal *= 0.5;
            vertical *= 0.5;
        }

        KnockbackStrength strength = new KnockbackStrength(horizontal, vertical);

        // 6. Apply context modifiers
        strength = modifier.applyBlockingReduction(victim, strength);
        strength = modifier.applyAirMultipliers(victim, strength, resolved.airMultH, resolved.airMultV);
        strength = modifier.applyFallingModifiers(victim, strength);

        // Apply knockback resistance attribute
        double resistance = victim.getAttributeValue(Attribute.KNOCKBACK_RESISTANCE);
        strength = new KnockbackStrength(
                strength.horizontal * (1 - resistance),
                strength.vertical * (1 - resistance)
        );

        // 7. Calculate final velocity (delegate to calculator)
        Vec finalVelocity = calculator.calculateFinalVelocity(victim, direction, strength, type);

        // 8. Apply sync compensation if enabled
        // DISABLED: Sync handler needs rework
        /*
        if (victim instanceof Player player && knockbackSyncEnabled) {
            finalVelocity = KnockbackSyncHandler.getInstance()
                    .compensateKnockback(player, finalVelocity, attacker, player.isOnGround());
        }
        */

        // 9. Apply velocity
        victim.setVelocity(finalVelocity);

        // 10. Send packet to client
        if (victim instanceof Player player) {
            player.sendPacket(new EntityVelocityPacket(player.getEntityId(), finalVelocity));
        }
    }

    /**
     * Apply projectile knockback with Punch enchantment support.
     *
     * @param victim The entity being hit
     * @param projectile The projectile entity (tags resolved from this)
     * @param projectileOrigin Where projectile was shot from
     * @param punchLevel Punch enchantment level (0 = none)
     */
    public void applyProjectileKnockback(LivingEntity victim, Entity projectile,
                                         Pos projectileOrigin, int punchLevel) {
        applyKnockbackInternal(victim, projectile, projectileOrigin,
                KnockbackType.PROJECTILE, false, punchLevel);
    }

    /**
     * Apply projectile knockback without enchantments (convenience method).
     */
    public void applyProjectileKnockback(LivingEntity victim, Entity projectile,
                                         Pos projectileOrigin) {
        applyProjectileKnockback(victim, projectile, projectileOrigin, 0);
    }

    // TODO: Projectile knockback SHOULD be in the knockback feature, just not this class

    // TODO: Move out to a knockback math specific class.

    // ===========================
    // CONFIGURATION
    // ===========================

    public void setConfig(KnockbackConfig config) {
        requireInitialized();
        this.currentConfig = config;
        this.knockbackSyncEnabled = config.knockbackSyncSupported() && knockbackSyncEnabled;
        log.debug("Config updated: modern={}, sync={}", config.modern(), config.knockbackSyncSupported());
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
     * Override base config resolution to support projectile-specific configs.
     * Projectiles use their type-specific config, melee uses serverDefaultConfig.
     */
    @Override
    protected KnockbackConfig resolveBaseConfig(Entity attacker, LivingEntity victim, EquipmentSlot handUsed) {
        // If attacker is a projectile, use projectile-specific base config
        if (isProjectileAttacker(attacker)) {
            return getProjectileBaseConfig(attacker);
        }

        // Otherwise use the normal resolution (which checks tags then falls back to serverDefaultConfig)
        return super.resolveBaseConfig(attacker, victim, handUsed);
    }

    /**
     * Get the base knockback config for a specific projectile type.
     * This is the "preset" that tags modify on top of.
     */
    private KnockbackConfig getProjectileBaseConfig(Entity projectile) {
        try {
            var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
            var projectileConfig = projectileManager.getProjectileConfig();

            EntityType type = projectile.getEntityType();
            com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig pkbConfig;

            if (type == EntityType.ARROW || type == EntityType.SPECTRAL_ARROW || type == EntityType.TRIDENT) {
                pkbConfig = projectileConfig.getArrowKnockbackConfig();
            } else if (type == EntityType.SNOWBALL) {
                pkbConfig = projectileConfig.getSnowballKnockbackConfig();
            } else if (type == EntityType.EGG) {
                pkbConfig = projectileConfig.getEggKnockbackConfig();
            } else if (type == EntityType.ENDER_PEARL) {
                pkbConfig = projectileConfig.getEnderPearlKnockbackConfig();
            } else if (type == EntityType.FISHING_BOBBER) {
                pkbConfig = projectileConfig.getFishingRodKnockbackConfig();
            } else {
                log.warn("Unknown projectile type: {}, using melee config", type);
                pkbConfig = null;
            }

            if (pkbConfig == null) {
                return serverDefaultConfig;
            }

            return convertProjectileConfig(pkbConfig);

        } catch (IllegalStateException e) {
            log.warn("ProjectileManager not initialized, using melee config for projectile");
            return serverDefaultConfig;
        }
    }

    /**
     * Convert ProjectileKnockbackConfig to KnockbackConfig format.
     * This allows projectile configs to participate in the tag resolution system.
     */
    private KnockbackConfig convertProjectileConfig(com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig pkbConfig) {
        if (!pkbConfig.enabled()) {
            // Return a "no knockback" config
            return new KnockbackConfig(
                    0.0, 0.0, 0.0,  // horizontal, vertical, verticalLimit
                    0.0, 0.0,        // sprint bonus (not used for projectiles)
                    1.0, 1.0,        // air multipliers (can be overridden by tags)
                    0.0,             // look weight (not used for projectiles)
                    false,           // legacy mode
                    false            // no sync for projectiles
            );
        }

        return new KnockbackConfig(
                pkbConfig.horizontalKnockback(),
                pkbConfig.verticalKnockback(),
                pkbConfig.verticalLimit(),
                0.0, 0.0,  // No sprint bonus for projectiles
                1.0, 1.0,  // Default air multipliers (tags can override)
                0.0,       // No look weight for projectiles
                false,     // Legacy mode
                false      // No sync for projectiles
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

    public static class KnockbackStrength {
        public final double horizontal;
        public final double vertical;

        public KnockbackStrength(double horizontal, double vertical) {
            this.horizontal = horizontal;
            this.vertical = vertical;
        }
    }
}

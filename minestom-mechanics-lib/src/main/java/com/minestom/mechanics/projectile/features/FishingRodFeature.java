package com.minestom.mechanics.projectile.features;

import com.minestom.mechanics.config.projectiles.ProjectileConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackPresets;
import com.minestom.mechanics.projectile.components.ProjectileSoundHandler;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityPresets;
import com.minestom.mechanics.projectile.entities.FishingBobber;
import com.minestom.mechanics.projectile.utils.VelocityCalculator;
import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

import java.util.Objects;

// TODO: Fix players not being able to hit themselves with fishing rod

/**
 * Manages fishing rod functionality - casting, retrieving, and bobber lifecycle.
 * Consolidated design: All fishing rod logic in one place.
 */
public class FishingRodFeature extends InitializableSystem implements ProjectileFeature {

    private static FishingRodFeature instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("FishingRodFeature");

    // Bobber tracking
    private static final Tag<FishingBobber> FISHING_BOBBER = Tag.Transient("fishingBobber");

    // Configuration and components
    private ProjectileVelocityConfig config;
    private final ProjectileSoundHandler soundHandler;

    private FishingRodFeature() {
        this.config = ProjectileVelocityPresets.FISHING_ROD;
        this.soundHandler = new ProjectileSoundHandler();
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static FishingRodFeature initialize() {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("FishingRodFeature");
            return instance;
        }

        instance = new FishingRodFeature();
        instance.registerListeners();
        instance.markInitialized();

        LogUtil.logInit("FishingRodFeature");
        return instance;
    }

    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Handle fishing rod usage (cast/retrieve)
        handler.addListener(PlayerUseItemEvent.class, event -> {
            if (event.getItemStack().material() != Material.FISHING_ROD) return;
            handleFishingRod(event.getPlayer(), event.getHand());
        });

        // Handle hotbar switching - cleanup bobber if player switches away from rod
        handler.addListener(PlayerChangeHeldSlotEvent.class, event -> {
            // Delayed check to ensure item has actually changed
            MinecraftServer.getSchedulerManager().buildTask(() -> {
                handleSlotChange(event.getPlayer());
            }).delay(TaskSchedule.tick(1)).schedule();
        });

        // Note: PlayerDeathEvent and PlayerDisconnectEvent cleanup handled by ProjectileCleanupHandler
    }

    // ===========================
    // CORE LOGIC
    // ===========================

    private void handleFishingRod(Player player, PlayerHand hand) {
        if (hasActiveBobber(player)) {
            // Retrieve existing bobber
            retrieveBobber(player);
            soundHandler.playFishingRetrieveSound(player);
        } else {
            // Cast new bobber
            castBobber(player);
            soundHandler.playFishingCastSound(player);
        }
    }

    private void castBobber(Player player) {
        FishingBobber bobber = new FishingBobber(player, true); // Always legacy mode

        // Set knockback configuration from ProjectileManager
        try {
            var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
            var projectileConfig = projectileManager.getProjectileConfig();
            bobber.setKnockbackConfig(projectileConfig.getFishingRodKnockbackConfig());
            bobber.setKnockbackMode(projectileConfig.getFishingRodKnockbackMode());
        } catch (IllegalStateException e) {
            // ProjectileManager not initialized, use defaults
            bobber.setKnockbackConfig(ProjectileKnockbackPresets.FISHING_ROD);
            bobber.setKnockbackMode(ProjectileConfig.FishingRodKnockbackMode.BOBBER_RELATIVE);
        }

        // Store bobber on player
        player.setTag(FISHING_BOBBER, bobber);

        // Calculate spawn position and velocity
        Pos playerPos = player.getPosition();
        Pos spawnPos = VelocityCalculator.calculateSpawnOffset(player);
        Vec velocity = calculateFishingVelocity(playerPos, playerPos.pitch(), playerPos.yaw());

        // Spawn bobber in world
        bobber.setInstance(Objects.requireNonNull(player.getInstance()), spawnPos);
        bobber.setVelocity(velocity);

        log.debug("Cast fishing bobber for {} at {}", player.getUsername(), spawnPos);
    }

    private void handleSlotChange(Player player) {
        if (!hasActiveBobber(player)) return;

        // Check if player still has a fishing rod in either hand
        var mainHand = player.getItemInMainHand();
        var offHand = player.getItemInOffHand();

        if (mainHand.material() != Material.FISHING_ROD && offHand.material() != Material.FISHING_ROD) {
            // Player switched away from fishing rod - cleanup bobber
            removeActiveBobber(player);
            log.debug("Cleaned up fishing bobber for {} due to hotbar switch", player.getUsername());
        }
    }

    // ===========================
    // BOBBER MANAGEMENT
    // ===========================

    private void retrieveBobber(Player player) {
        FishingBobber bobber = player.getTag(FISHING_BOBBER);
        if (bobber != null) {
            bobber.retrieve();
            bobber.remove();
        }
        player.removeTag(FISHING_BOBBER);
    }

    private void removeActiveBobber(Player player) {
        FishingBobber bobber = player.getTag(FISHING_BOBBER);
        if (bobber != null) {
            bobber.remove();
        }
        player.removeTag(FISHING_BOBBER);
    }

    // ===========================
    // VELOCITY CALCULATION
    // ===========================

    private Vec calculateFishingVelocity(Pos playerPos, float playerPitch, float playerYaw) {
        // Base velocity from player's aim
        double maxVelocity = 0.4F;
        Vec velocity = VelocityCalculator.calculateDirectionalVelocity(playerPitch, playerYaw, maxVelocity);

        // Apply horizontal and vertical multipliers
        velocity = new Vec(
                velocity.x() * config.horizontalMultiplier(),
                velocity.y() * config.verticalMultiplier(),
                velocity.z() * config.horizontalMultiplier()
        );

        // Apply spread with multiplier
        double spread = 0.0075 * config.spreadMultiplier();
        velocity = VelocityCalculator.applySpread(velocity, spread);

        // Convert to per-tick velocity
        return VelocityCalculator.toPerTickVelocity(velocity, 0.75);
    }

    // ===========================
    // PUBLIC API
    // ===========================

    public boolean hasActiveBobber(Player player) {
        return player.hasTag(FISHING_BOBBER);
    }

    public FishingBobber getActiveBobber(Player player) {
        return player.getTag(FISHING_BOBBER);
    }

    public void cleanup(Player player) {
        if (hasActiveBobber(player)) {
            removeActiveBobber(player);
        }
    }

    public static FishingRodFeature getInstance() {
        return requireInstance(instance, "FishingRodFeature");
    }

    // ===========================
    // CONFIGURATION
    // ===========================

    public ProjectileVelocityConfig getConfig() {
        return config;
    }

    public void setConfig(ProjectileVelocityConfig config) {
        this.config = config;
        log.debug("FishingRodVelocityConfig updated: h={}, v={}, s={}, g={}",
                config.horizontalMultiplier(), config.verticalMultiplier(),
                config.spreadMultiplier(), config.gravity());
    }
}
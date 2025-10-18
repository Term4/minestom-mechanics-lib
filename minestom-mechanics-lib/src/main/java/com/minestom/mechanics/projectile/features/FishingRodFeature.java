package com.minestom.mechanics.projectile.features;

import com.minestom.mechanics.projectile.components.*;
import com.minestom.mechanics.projectile.config.FishingRodConfig;
import com.minestom.mechanics.projectile.config.FishingRodKnockbackConfig;
import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.item.Material;
import com.minestom.mechanics.projectile.entities.FishingBobber;

import java.util.Objects;

/**
 * Refactored FishingRodFeature using focused components.
 * Orchestrates fishing rod functionality through specialized components.
 */
public class FishingRodFeature extends InitializableSystem implements ProjectileFeature {
    
    private static FishingRodFeature instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("FishingRodFeature");
    
    // Component references
    private final FishingBobberManager bobberManager;
    private FishingVelocityCalculator velocityCalculator;
    private final ProjectileSoundHandler soundHandler;
    
    // Configuration
    private FishingRodConfig config;
    
    private FishingRodFeature() {
        // Initialize with default configuration
        this.config = FishingRodConfig.defaultConfig();
        
        // Initialize components
        this.bobberManager = new FishingBobberManager();
        this.velocityCalculator = new FishingVelocityCalculator(config);
        this.soundHandler = new ProjectileSoundHandler();
    }
    
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
        
        // Handle fishing rod usage
        handler.addListener(PlayerUseItemEvent.class, event -> {
            if (event.getItemStack().material() != Material.FISHING_ROD) return;
            handleFishingRod(event.getPlayer(), event.getHand());
        });
        
        // ✅ FIXED: Handle hotbar switching - cleanup fishing bobber
        // Use delayed check to ensure the item has actually changed
        handler.addListener(PlayerChangeHeldSlotEvent.class, event -> {
            // Schedule a delayed check to ensure the item has actually changed
            MinecraftServer.getSchedulerManager().buildTask(() -> {
                handleSlotChange(event.getPlayer());
            }).delay(TaskSchedule.tick(1)).schedule();
        });
        
        // Note: PlayerDeathEvent and PlayerDisconnectEvent cleanup is now handled by ProjectileCleanupHandler
    }
    
    private void handleFishingRod(Player player, PlayerHand hand) {
        if (bobberManager.hasActiveBobber(player)) {
            // Retrieve existing bobber
            bobberManager.retrieveBobber(player);
            
            // Durability damage disabled for now
            
            soundHandler.playFishingRetrieveSound(player);
        } else {
            // Cast new bobber
            soundHandler.playFishingCastSound(player);
            
            FishingBobber bobber = new FishingBobber(player, true); // Always legacy mode
            // Set knockback configuration
            bobber.setKnockbackConfig(getFishingRodKnockbackConfig());
            bobberManager.setActiveBobber(player, bobber);
            
            // Calculate spawn position and velocity using FishingVelocityCalculator
            Pos playerPos = player.getPosition();
            float playerPitch = playerPos.pitch();
            float playerYaw = playerPos.yaw();

            // Use velocity calculator for consistent velocity calculation
            Pos spawnPos = velocityCalculator.calculateSpawnPosition(player);
            Vec velocity = velocityCalculator.calculateVelocity(playerPos, playerPitch, playerYaw);

            bobber.setInstance(Objects.requireNonNull(player.getInstance()), spawnPos);
            bobber.setVelocity(velocity);
            
            log.debug("Cast fishing bobber for {} at {}", player.getUsername(), spawnPos);
        }
    }
    
    /**
     * Handle hotbar slot changes - cleanup fishing bobber if player switches away from fishing rod
     */
    private void handleSlotChange(Player player) {
        if (bobberManager.hasActiveBobber(player)) {
            // Check if the new held item is still a fishing rod
            var currentItem = player.getItemInMainHand();
            var offhandItem = player.getItemInOffHand();
            
            // Only cleanup if neither hand has a fishing rod
            if (currentItem.material() != Material.FISHING_ROD && offhandItem.material() != Material.FISHING_ROD) {
                // Player switched away from fishing rod - cleanup bobber
                bobberManager.removeActiveBobber(player);
                log.debug("Cleaned up fishing bobber for {} due to hotbar switch", player.getUsername());
            }
        }
    }
    
    
    // ===========================
    // PUBLIC API
    // ===========================
    
    public boolean hasActiveBobber(Player player) {
        return bobberManager.hasActiveBobber(player);
    }
    
    public FishingBobber getActiveBobber(Player player) {
        return bobberManager.getActiveBobber(player);
    }
    
    public void cleanup(Player player) {
        if (bobberManager.hasActiveBobber(player)) {
            bobberManager.removeActiveBobber(player);
        }
    }
    
    public static FishingRodFeature getInstance() {
        return requireInstance(instance, "FishingRodFeature");
    }
    
    // ===========================
    // CONFIGURATION
    // ===========================
    
    public FishingRodConfig getConfig() {
        return config;
    }
    
    public void setConfig(FishingRodConfig config) {
        this.config = config;
        // Update velocity calculator with new config
        this.velocityCalculator = new FishingVelocityCalculator(config);
        log.debug("FishingRodConfig updated: h={}, v={}, s={}, p={}", 
                config.horizontalMultiplier(), config.verticalMultiplier(),
                config.spreadMultiplier(), config.powerMultiplier());
    }
    
    private FishingRodKnockbackConfig getFishingRodKnockbackConfig() {
        // Get from ProjectileManager
        try {
            return com.minestom.mechanics.manager.ProjectileManager.getInstance().getFishingRodKnockbackConfig();
        } catch (IllegalStateException e) {
            // ProjectileManager not initialized, use default
            return FishingRodKnockbackConfig.defaultConfig();
        }
    }
}

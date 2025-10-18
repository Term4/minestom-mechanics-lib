package com.minestom.mechanics.projectile.features;

import com.minestom.mechanics.projectile.components.*;
import com.minestom.mechanics.projectile.entities.AbstractArrow;
import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.ViewerBasedAnimationHandler;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.item.PlayerBeginItemUseEvent;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.item.ItemAnimation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.component.DataComponents;

/**
 * Refactored BowFeature using focused components.
 * Orchestrates bow shooting functionality through specialized components.
 */
public class BowFeature extends InitializableSystem implements ProjectileFeature {
    
    private static BowFeature instance;
    
    // Component references
    private final BowStateManager stateManager;
    private final BowArrowManager arrowManager;
    private final BowPowerCalculator powerCalculator;
    private final BowArrowCreator arrowCreator;
    private final ProjectileSoundHandler soundHandler;
    
    private BowFeature() {
        // Initialize components
        this.stateManager = new BowStateManager();
        this.arrowManager = new BowArrowManager();
        this.powerCalculator = new BowPowerCalculator();
        this.arrowCreator = new BowArrowCreator();
        this.soundHandler = new ProjectileSoundHandler();
    }
    
    public static BowFeature initialize() {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("BowFeature");
            return instance;
        }
        
        instance = new BowFeature();
        instance.registerListeners();
        instance.markInitialized();
        
        LogUtil.logInit("BowFeature");
        return instance;
    }
    
    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();
        
        // Start tracking when bow is drawn
        handler.addListener(PlayerBeginItemUseEvent.class, event -> {
            if (event.getAnimation() == ItemAnimation.BOW) {
                if (event.getPlayer().getGameMode() != GameMode.CREATIVE
                        && arrowManager.getArrowFromInventory(event.getPlayer()) == null) {
                    event.setCancelled(true);
                } else {
                    startDrawing(event.getPlayer());
                }
            }
        });
        
        // Stop tracking when bow is released
        handler.addListener(PlayerCancelItemUseEvent.class, event -> {
            if (event.getItemStack().material() == Material.BOW) {
                stopDrawing(event.getPlayer());
                handleBowRelease(event);
            }
        });
        
        // Stop on slot change
        handler.addListener(PlayerChangeHeldSlotEvent.class, event -> {
            if (stateManager.isDrawingBow(event.getPlayer())) {
                stopDrawing(event.getPlayer());
            }
        });
        
        // Note: PlayerDisconnectEvent and PlayerDeathEvent cleanup is now handled by ProjectileCleanupHandler
    }
    
    // ===========================
    // BOW DRAWING STATE
    // ===========================
    
    private void startDrawing(Player player) {
        stateManager.startDrawing(player);
        
        // Register with version-based animation handler
        ViewerBasedAnimationHandler.getInstance().registerAnimation(
                player,
                "bow_drawing",
                stateManager::isDrawingBow
        );
    }
    
    private void stopDrawing(Player player) {
        stateManager.stopDrawing(player);
        
        // Unregister from animation handler
        ViewerBasedAnimationHandler.getInstance().unregisterAnimation(player);
    }
    
    // ===========================
    // BOW RELEASE (SHOOTING)
    // ===========================
    
    private void handleBowRelease(PlayerCancelItemUseEvent event) {
        Player player = event.getPlayer();
        ItemStack bowStack = event.getItemStack();
        if (bowStack.material() != Material.BOW) return;
        
        // Check for infinity enchantment
        EnchantmentList enchantments = bowStack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) enchantments = EnchantmentList.EMPTY;
        
        boolean infinite = player.getGameMode() == GameMode.CREATIVE
                || enchantments.level(Enchantment.INFINITY) > 0;
        
        // Get arrow from inventory
        BowArrowManager.ArrowResult arrowResult = arrowManager.getArrowFromInventory(player);
        if (!infinite && arrowResult == null) return;
        
        if (arrowResult == null) {
            arrowResult = new BowArrowManager.ArrowResult(com.minestom.mechanics.projectile.entities.Arrow.DEFAULT_ARROW, -1);
        }
        
        // Calculate power
        long useTicks = player.getCurrentItemUseTime();
        double power = powerCalculator.calculatePower(useTicks);
        if (!powerCalculator.isPowerSufficient(power)) return;
        
        // Create and configure arrow
        AbstractArrow arrow = arrowCreator.createArrow(arrowResult.stack(), player, bowStack, power);
        
        // Spawn arrow
        arrowCreator.spawnArrow(arrow, player, power);
        
        // Play sound
        soundHandler.playBowShootSound(player, power);
        
        // Consume arrow
        arrowManager.consumeArrow(player, arrowResult, infinite);
    }
    
    // ===========================
    // PUBLIC API
    // ===========================
    
    public boolean isDrawingBow(Player player) {
        return stateManager.isDrawingBow(player);
    }
    
    public void cleanup(Player player) {
        stateManager.cleanup(player);
    }
    
    public int getDrawingPlayersCount() {
        return stateManager.getDrawingPlayersCount();
    }
    
    public static BowFeature getInstance() {
        return requireInstance(instance, "BowFeature");
    }
    
}

package com.minestom.mechanics.systems.projectile.features;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackPresets;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityPresets;
import com.minestom.mechanics.systems.projectile.components.ProjectileCreator;
import com.minestom.mechanics.systems.projectile.entities.AbstractArrow;
import com.minestom.mechanics.systems.projectile.entities.Arrow;
import com.minestom.mechanics.systems.projectile.components.ProjectileSoundHandler;
import com.minestom.mechanics.systems.projectile.utils.ProjectileMaterials;
import com.minestom.mechanics.systems.util.InitializableSystem;
import com.minestom.mechanics.systems.util.LogUtil;
import com.minestom.mechanics.systems.util.ViewerBasedAnimationHandler;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.item.PlayerBeginItemUseEvent;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.item.ItemAnimation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.component.DataComponents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified bow feature handling all bow mechanics.
 * Manages drawing state, arrow inventory, power calculation, and shooting.
 */
public class BowFeature extends InitializableSystem implements ProjectileFeature {
    private static final LogUtil.SystemLogger log = LogUtil.system("BowFeature");
    private static BowFeature instance;

    // State management
    private final Map<UUID, Boolean> drawingBows = new ConcurrentHashMap<>();

    // Components
    private final ProjectileCreator creator;
    private final ProjectileSoundHandler soundHandler;

    private BowFeature() {
        this.creator = new ProjectileCreator();
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

        // Start drawing
        handler.addListener(PlayerBeginItemUseEvent.class, event -> {
            if (event.getAnimation() == ItemAnimation.BOW) {
                if (event.getPlayer().getGameMode() != GameMode.CREATIVE
                        && getArrowFromInventory(event.getPlayer()) == null) {
                    event.setCancelled(true);
                } else {
                    startDrawing(event.getPlayer());
                }
            }
        });

        // Release bow
        handler.addListener(PlayerCancelItemUseEvent.class, event -> {
            if (ProjectileMaterials.isBow(event.getItemStack().material())) {
                stopDrawing(event.getPlayer());
                handleBowRelease(event);
            }
        });

        // Stop on slot change
        handler.addListener(PlayerChangeHeldSlotEvent.class, event -> {
            if (isDrawingBow(event.getPlayer())) {
                stopDrawing(event.getPlayer());
            }
        });
    }

    // ===========================
    // BOW RELEASE LOGIC
    // ===========================

    private void handleBowRelease(PlayerCancelItemUseEvent event) {
        Player player = event.getPlayer();
        ItemStack bowStack = event.getItemStack();

        // Check infinity
        EnchantmentList enchantments = bowStack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) enchantments = EnchantmentList.EMPTY;

        boolean infinite = player.getGameMode() == GameMode.CREATIVE
                || enchantments.level(Enchantment.INFINITY) > 0;

        // Get arrow
        ArrowResult arrowResult = getArrowFromInventory(player);
        if (!infinite && arrowResult == null) return;

        if (arrowResult == null) {
            arrowResult = new ArrowResult(Arrow.DEFAULT_ARROW, -1);
        }

        // Calculate power
        long useTicks = player.getCurrentItemUseTime();
        double power = calculatePower(useTicks);
        if (power < 0.1) return;

        // Create arrow entity
        AbstractArrow arrow = new Arrow(player);

        // Configure arrow
        if (power >= 1.0) arrow.setCritical(true);
        applyEnchantments(arrow, bowStack);
        setPickupMode(arrow, arrowResult.stack(), player, bowStack);
        arrow.setUseKnockbackHandler(true);
        arrow.setKnockbackConfig(getArrowKnockbackConfig());

        // Spawn using unified creator
        creator.spawnArrow(arrow, player, bowStack, getArrowVelocityConfig(), power);

        // Play sound & consume
        soundHandler.playBowShootSound(player, power);
        consumeArrow(player, arrowResult, infinite);
    }

    // ===========================
    // ARROW CONFIGURATION
    // ===========================

    private void applyEnchantments(AbstractArrow arrow, ItemStack bowStack) {
        EnchantmentList enchantments = bowStack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) enchantments = EnchantmentList.EMPTY;

        // Power enchantment
        int powerLevel = enchantments.level(Enchantment.POWER);
        if (powerLevel > 0) {
            arrow.setBaseDamage(arrow.getBaseDamage() + powerLevel * 0.5 + 0.5);
        }

        // Punch enchantment
        int punchLevel = enchantments.level(Enchantment.PUNCH);
        if (punchLevel > 0) {
            arrow.setKnockback(punchLevel);
        }

        // Flame enchantment
        if (enchantments.level(Enchantment.FLAME) > 0) {
            arrow.setFireTicksLeft(100 * ServerFlag.SERVER_TICKS_PER_SECOND);
        }
    }

    private void setPickupMode(AbstractArrow arrow, ItemStack arrowStack, Player player, ItemStack bowStack) {
        EnchantmentList enchantments = bowStack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) enchantments = EnchantmentList.EMPTY;

        boolean infinite = player.getGameMode() == GameMode.CREATIVE
                || enchantments.level(Enchantment.INFINITY) > 0;

        boolean reallyInfinite = infinite && ProjectileMaterials.isArrow(arrowStack.material());

        if (reallyInfinite || player.getGameMode() == GameMode.CREATIVE) {
            arrow.setPickupMode(AbstractArrow.PickupMode.CREATIVE_ONLY);
        }
    }

    private ProjectileKnockbackConfig getArrowKnockbackConfig() {
        try {
            return com.minestom.mechanics.manager.ProjectileManager.getInstance()
                    .getProjectileConfig().getArrowKnockbackConfig();
        } catch (IllegalStateException e) {
            return ProjectileKnockbackPresets.ARROW;
        }
    }

    private ProjectileVelocityConfig getArrowVelocityConfig() {
        try {
            return com.minestom.mechanics.manager.ProjectileManager.getInstance()
                    .getProjectileConfig().getArrowVelocityConfig();
        } catch (IllegalStateException e) {
            return ProjectileVelocityPresets.ARROW;
        }
    }

    // ===========================
    // DRAWING STATE MANAGEMENT
    // ===========================

    private void startDrawing(Player player) {
        drawingBows.put(player.getUuid(), true);
        ViewerBasedAnimationHandler.getInstance().registerAnimation(
                player, "bow_drawing", this::isDrawingBow
        );
        log.debug("{} started drawing bow", player.getUsername());
    }

    private void stopDrawing(Player player) {
        drawingBows.remove(player.getUuid());
        ViewerBasedAnimationHandler.getInstance().unregisterAnimation(player);
        log.debug("{} stopped drawing bow", player.getUsername());
    }

    public boolean isDrawingBow(Player player) {
        return Boolean.TRUE.equals(drawingBows.get(player.getUuid()));
    }

    // ===========================
    // POWER CALCULATION
    // ===========================

    private double calculatePower(long ticks) {
        double seconds = ticks / (double) ServerFlag.SERVER_TICKS_PER_SECOND;
        double power = (seconds * seconds + seconds * 2.0) / 3.0;
        return Math.min(power, 1.0);
    }

    // ===========================
    // ARROW INVENTORY MANAGEMENT
    // ===========================

    private ArrowResult getArrowFromInventory(Player player) {
        // Check offhand first
        ItemStack offhand = player.getItemInOffHand();
        if (ProjectileMaterials.isArrow(offhand.material())) {
            return new ArrowResult(offhand, 40);
        }

        // Check main hand
        ItemStack mainHand = player.getItemInMainHand();
        if (ProjectileMaterials.isArrow(mainHand.material())) {
            return new ArrowResult(mainHand, player.getHeldSlot());
        }

        // Search inventory
        ItemStack[] items = player.getInventory().getItemStacks();
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item != null && !item.isAir() && ProjectileMaterials.isArrow(item.material())) {
                return new ArrowResult(item, i);
            }
        }

        // Creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return new ArrowResult(Arrow.DEFAULT_ARROW, -1);
        }

        return null;
    }

    private void consumeArrow(Player player, ArrowResult arrowResult, boolean isInfinite) {
        if (isInfinite || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (arrowResult.slot >= 0) {
            ItemStack newStack = arrowResult.stack.withAmount(arrowResult.stack.amount() - 1);
            player.getInventory().setItemStack(arrowResult.slot, newStack);
            log.debug("Consumed arrow from slot {} for {}", arrowResult.slot, player.getUsername());
        }
    }

    private record ArrowResult(ItemStack stack, int slot) {}

    // ===========================
    // PUBLIC API
    // ===========================

    public void cleanup(Player player) {
        drawingBows.remove(player.getUuid());
    }

    public int getDrawingPlayersCount() {
        return drawingBows.size();
    }

    public static BowFeature getInstance() {
        return requireInstance(instance, "BowFeature");
    }
}
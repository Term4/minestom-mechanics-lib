package com.test.minestom;

import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.config.gameplay.DamageConfig;
import com.minestom.mechanics.config.gameplay.DamagePresets;
import com.minestom.mechanics.config.gameplay.GameplayPresets;
import com.minestom.mechanics.manager.MechanicsManager;
import com.minestom.mechanics.systems.blocking.BlockableItem;
import com.minestom.mechanics.systems.blocking.BlockingState;
import com.minestom.mechanics.systems.blocking.tags.BlockableTagValue;
import com.minestom.mechanics.config.projectiles.ProjectilePresets;
import com.minestom.mechanics.systems.compatibility.ClientVersionDetector;
import com.minestom.mechanics.systems.compatibility.legacy_1_8.fix.LegacyInventoryUtil;
import com.minestom.mechanics.systems.health.damage.types.Fire;
import com.minestom.mechanics.systems.misc.ModernStutterFix;
import com.minestom.mechanics.systems.compatibility.legacy_1_8.fix.LegacyAnimationFix;
import com.minestom.mechanics.systems.compatibility.legacy_1_8.fix.LegacySprintMetadataSuppression;
import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.systems.health.damage.util.DamageOverride;
import com.minestom.mechanics.systems.compatibility.legacy_1_8.feature.GravitySystem;
import com.minestom.mechanics.systems.player.PlayerCleanupManager;
import com.test.minestom.commands.CommandRegistry;
import com.minestom.mechanics.manager.CombatManager;
import com.test.minestom.commands.debug.EntityVisibilityTest;
import com.minestom.mechanics.config.combat.CombatPresets;
import com.minestom.mechanics.config.world.WorldInteractionConfig;
import com.test.minestom.config.server.ServerConfig;
import com.test.minestom.gui.GuiManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.timer.TaskSchedule;

// DamageOverride replaces the old HealthTagWrapper/HealthTagValue system

// TODO: Fix velocity 'Already connected to proxy' issue?? Possible? idk

/**
 * 1.8 PvP Test Server - WITH PROJECTILES
 * Full combat + projectile system
 */
public class Main {

    private static InstanceContainer gameWorld;

    // ===========================
    // CONFIGURATION
    // ===========================
    
    // NEW API - Multiple usage patterns demonstrated:
    
    // Pattern 1: Simple preset usage (recommended for most users)
    private static final CombatConfig COMBAT_CONFIG = CombatPresets.MINEMEN;
    private static final DamageConfig DAMAGE_CONFIG = DamagePresets.MINEMEN;
    
    private static final ServerConfig SERVER_CONFIG = ServerConfig.production("TkJ8OBuWtDYC");

    // ===========================
    // MAIN
    // ===========================

    public static void main(String[] args) {
        System.setProperty("minestom.new-socket-write-lock", "true");
        System.setProperty( "minestom.enforce-entity-interaction-range", "false");
        System.setProperty("minestom.chunk-view-distance", "12");
        System.setProperty("minestom.entity-view-distance", "5");
        System.setProperty("minestom.tps", "20");

        // Initialize server
        MinecraftServer server;
        if (SERVER_CONFIG.isVelocityEnabled()) {
            server = MinecraftServer.init(
                    new net.minestom.server.Auth.Bungee()
            );
            System.setProperty("minestom.velocity-support", "false");
        } else {
            server = MinecraftServer.init();
        }

        // CRITICAL: Initialize cleanup manager FIRST
        PlayerCleanupManager.initialize();

        // Setup
        createWorld();
        initializeWorldInteraction();
        initializePvP();
        initializeCompatibility();
        registerEvents();
        // startMsptLogger();

        // Start server
        server.start(SERVER_CONFIG.getServerIp(), SERVER_CONFIG.getServerPort());
    }

    // ===========================
    // WORLD SETUP
    // ===========================

    private static void createWorld() {
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        gameWorld = instanceManager.createInstanceContainer();
        gameWorld.setChunkSupplier(LightingChunk::new);

        // Generate flat world
        if (SERVER_CONFIG.isFlatWorld()) {
            int height = SERVER_CONFIG.getWorldHeight();
            gameWorld.setGenerator(unit -> {
                unit.modifier().fillHeight(0, height - 2, Block.STONE);
                unit.modifier().fillHeight(height - 2, height - 1, Block.GRASS_BLOCK);
            });
        }

        MinecraftServer.LOGGER.info("[World] Flat world created");
    }

    // ===========================
    // WORLD INTERACTION INITIALIZATION
    // ===========================

    private static void initializeWorldInteraction() {
        // Set up server-wide world interaction configuration
        WorldInteractionConfig worldConfig = WorldInteractionConfig.builder()
                .blockReach(6.0, 4.5)  // Creative, Survival
                .blockRaycastStep(0.2)
                .build();
        
        com.minestom.mechanics.config.ServerConfig.setWorldInteraction(worldConfig);
        
        MinecraftServer.LOGGER.info("[Main] World interaction configuration set");
    }

    // ===========================
    // PVP INITIALIZATION
    // ===========================

    private static void initializePvP() {
        MechanicsManager.getInstance()
                .configure()
                .withCombat(COMBAT_CONFIG)
                .withGameplay(GameplayPresets.MINEMEN)
                .withDamage(DAMAGE_CONFIG)
                .withHitbox(com.minestom.mechanics.config.combat.HitDetectionConfig.standard())
                .withArmor(true)
                .withKnockback(COMBAT_CONFIG.knockbackConfig())
                .initialize();

        DAMAGE_CONFIG.applyInvulnerabilityBuffersTo(gameWorld, COMBAT_CONFIG);
        // To disable melee damage but keep knockback: set NO_DAMAGE.thenOverride(propsWithBuffer) on world melee tag. See DAMAGE_KNOCKBACK_ANALYSIS.md.

        // Initialize projectiles separately (not handled by MechanicsManager YET)
        // Using default projectile config
        com.minestom.mechanics.manager.ProjectileManager.getInstance()
                .initialize(ProjectilePresets.VANILLA18);

        // Initialize once
        GravitySystem.initialize();

        ClientVersionDetector.getInstance();
        LegacyAnimationFix.getInstance();
        LegacySprintMetadataSuppression.getInstance();
        EntityVisibilityTest.register();

        ModernStutterFix.getInstance();

        // Initialize commands (depends on all systems being initialized first!)
        CommandRegistry.initialize();

        MinecraftServer.LOGGER.info("[Main] All PvP systems initialized successfully!");
    }

    // ===========================
    // LEGACY MECHANICS INIT
    // ===========================

    private static void initializeCompatibility() {
        MinecraftServer.LOGGER.info("╔═══════════════════════════════════════╗");
        MinecraftServer.LOGGER.info("║  1.8 Mechanics - Initializing         ║");
        MinecraftServer.LOGGER.info("╚═══════════════════════════════════════╝");

        // Gameplay mechanics are now handled by MechanicsManager in initializePvP()
        MinecraftServer.LOGGER.info("[Compat] 1.8 mechanics enforced");
    }

    // ===========================
    // MSPT LOGGER
    // ===========================

    /*
    private static void startMsptLogger() {
        long[] lastRun = { System.currentTimeMillis() };
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRun[0];
            lastRun[0] = now;
            double mspt = elapsed / 20.0;  // 20 ticks per second
            MinecraftServer.LOGGER.info("[MSPT] %.2f ms/tick".formatted(mspt));
        }).repeat(TaskSchedule.tick(20)).schedule();
    }
    */

    // ===========================
    // EVENT HANDLERS
    // ===========================

    private static void registerEvents() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Player configuration
        handler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(gameWorld);
            event.getPlayer().setRespawnPoint(SERVER_CONFIG.getSpawnPosition());
        });

        // Player spawn
        handler.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();

            if (event.isFirstSpawn()) {
                player.setGameMode(GameMode.SURVIVAL);

                giveStarterKit(player);

                LegacyInventoryUtil.forceInventorySyncDelayed(player, 5);
                // TODO: make this scheduler based on ping? / Replace with inventory packets (also look into command tablist)

                sendWelcomeMessage(player);

                // Add permanent Levitation
                player.addEffect(new Potion(
                        PotionEffect.RESISTANCE,
                        (byte) 2,
                        Potion.INFINITE_DURATION
                ));

                // Default movement speed is 0.1, Speed II increases by 40% (0.1 * 1.4 = 0.14)

                player.getAttribute(net.minestom.server.entity.attribute.Attribute.MOVEMENT_SPEED)
                        .setBaseValue(0.1 * (1 + (0.2 * 2))); // Speed II

                // Set gravity to 25% of normal (0.02 / 0.08 = 0.25)
                // GravitySystem.setGravity(player, 0.167);
                // Enable fire and cactus damage via player tags (overrides server config)
                var fireTag = HealthSystem.tag("fire");
                var cactusTag = HealthSystem.tag("cactus");
                if (fireTag != null) player.setTag(fireTag, DamageOverride.config(
                        Fire.Config.DEFAULT.withIgnitionDelayTicks(0).withIgnitionMode(Fire.IgnitionMode.DELAYED)
                ));
                if (cactusTag != null) player.setTag(cactusTag, DamageOverride.mult(1.0));
            }
        });

        // Player disconnect - automatic cleanup!
        handler.addListener(PlayerDisconnectEvent.class, event -> {
            Player player = event.getPlayer();

            // Clean up combat systems
            CombatManager.getInstance().cleanupPlayer(player);

            // Clean up GUI
            GuiManager.getInstance().cleanup(player);

            // Force aggressive cleanup
            player.removeTag(BlockingState.BLOCKING);
            player.removeTag(BlockingState.PREFERENCES);

            MinecraftServer.LOGGER.info("Cleaned up all data for: {}", player.getUsername());
        });
    }

    // ===========================
    // PLAYER SETUP
    // ===========================

    private static void giveStarterKit(Player player) {
        var inventory = player.getInventory();

        // Weapons
        inventory.setItemStack(0, ItemStack.of(Material.DIAMOND_SWORD));
        inventory.setItemStack(1, BlockableItem.withBlockable(
                ItemStack.of(Material.BOW),
                BlockableTagValue.blockable(false, 0.5, 0.5, 0.5)  // 50% damage reduction, 50% knockback reduction
        ));
        inventory.setItemStack(2, TestItems.bypassFishingRod());
        inventory.setItemStack(3, ItemStack.of(Material.SNOWBALL, 64));
        inventory.setItemStack(4, ItemStack.of(Material.EGG, 64));
        inventory.setItemStack(5, ItemStack.of(Material.WHITE_WOOL, 64));
        inventory.setItemStack(6, TestItems.bypassSword());
        inventory.setItemStack(9, ItemStack.of(Material.ARROW, 64));
        inventory.setItemStack(8, ItemStack.of(Material.COOKED_BEEF, 64));
        inventory.setItemStack(10, TestItems.slowGrappleEgg().withAmount(64));

        // Test Items (addItemStack fills remaining slots 7, 11, 12, ... - don't overwrite after)
        player.getInventory().addItemStack(BlockableItem.withBlockable(
                TestItems.knockbackStick(),
                BlockableTagValue.blockable(true, 0.25, 0.25, 0.25)  // 25% damage reduction, 75% knockback reduction
        ));
        player.getInventory().addItemStack(TestItems.sprintLookSword());
        player.getInventory().addItemStack(TestItems.vectorAddSword());
        player.getInventory().addItemStack(TestItems.knockbackEgg().withAmount(16));
        player.getInventory().addItemStack(TestItems.skySnowball().withAmount(16));
        player.getInventory().addItemStack(TestItems.grappleKnockbackSnowball().withAmount(16));
        player.getInventory().addItemStack(TestItems.gravityEgg().withAmount(16));
        player.getInventory().addItemStack(TestItems.laserSnowball().withAmount(16));
        player.getInventory().addItemStack(TestItems.heavyRock().withAmount(16));
        player.getInventory().addItemStack(TestItems.comboEgg().withAmount(16));
        player.getInventory().addItemStack(TestItems.knockbackMine().withAmount(16));
        player.getInventory().addItemStack(TestItems.cannonBow());
        player.getInventory().addItemStack(TestItems.bypassBow());
        player.getInventory().addItemStack(TestItems.bypassSnowballs());

        // Diamond Armor
        player.setHelmet(ItemStack.of(Material.DIAMOND_HELMET));
        player.setChestplate(ItemStack.of(Material.DIAMOND_CHESTPLATE));
        player.setLeggings(ItemStack.of(Material.DIAMOND_LEGGINGS));
        player.setBoots(ItemStack.of(Material.DIAMOND_BOOTS));
    }


    private static void sendWelcomeMessage(Player player) {
        // Use new API for configuration display
        CombatConfig combatConfig = COMBAT_CONFIG;
        DamageConfig damageConfig = DAMAGE_CONFIG;

        player.sendMessage(Component.empty());

        player.sendMessage(Component.text()
                .append(Component.text("⚔ ", NamedTextColor.GOLD))
                .append(Component.text("1.8 PvP Test Server", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .build());

        player.sendMessage(Component.text()
                .append(Component.text("   Mode: ", NamedTextColor.GRAY))
                .append(Component.text("MINEMEN", NamedTextColor.AQUA))
                .build());

        player.sendMessage(Component.text()
                .append(Component.text("   ", NamedTextColor.GRAY))
                .append(Component.text("Fast-paced competitive PvP", NamedTextColor.DARK_GRAY))
                .build());

        player.sendMessage(Component.empty());

        player.sendMessage(Component.text()
                .append(Component.text("   Knockback: ", NamedTextColor.GRAY))
                .append(Component.text(
                        COMBAT_CONFIG.knockbackConfig().modern() ? "MODERN" : "LEGACY",
                        NamedTextColor.AQUA))
                .build());

        player.sendMessage(Component.text()
                .append(Component.text("   Invulnerability: ", NamedTextColor.GRAY))
                .append(Component.text(DAMAGE_CONFIG.invulnerabilityTicks() + " ticks", NamedTextColor.AQUA))
                .build());

        player.sendMessage(Component.text()
                .append(Component.text("   Reach: ", NamedTextColor.GRAY))
                .append(Component.text("3.0 blocks", NamedTextColor.AQUA))
                .build());

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text()
                .append(Component.text("   Projectiles: ", NamedTextColor.GRAY))
                .append(Component.text("Bow, Fishing Rod, Snowballs, Eggs", NamedTextColor.AQUA))
                .build());

        player.sendMessage(Component.empty());

        player.sendMessage(Component.text()
                .append(Component.text("   Commands: ", NamedTextColor.GRAY))
                .append(Component.text("/kb", NamedTextColor.AQUA))
                .append(Component.text(" - Knockback settings", NamedTextColor.GRAY))
                .build());

        player.sendMessage(Component.empty());
    }
}
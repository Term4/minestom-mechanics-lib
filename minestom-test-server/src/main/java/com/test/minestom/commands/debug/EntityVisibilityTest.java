package com.test.minestom.commands.debug;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Test if the entity invisibility issue is in Minestom itself
 */
public class EntityVisibilityTest {

    public static void register() {
        CommandManager commandManager = MinecraftServer.getCommandManager();
        // GlobalEventHandler handler = MinecraftServer.getGlobalEventHandler(); // Unused variable

        System.out.println("===========================================");
        System.out.println("ENTITY VISIBILITY TEST REGISTERED!");
        System.out.println("Commands: /test1, /test2, /test3, /testvisible");
        System.out.println("===========================================");

        // Test 1: Spawn at current location
        Command test1 = new Command("test1");
        test1.setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            Instance instance = player.getInstance();
            if (instance == null) return;

            Pos playerPos = player.getPosition();

            Entity testEntity = new Entity(EntityType.ARMOR_STAND);
            // ArmorStandMeta meta = (ArmorStandMeta) testEntity.getEntityMeta(); // Unused variable
            // Set custom name on the ENTITY, not metadata
            testEntity.set(DataComponents.CUSTOM_NAME, Component.text("TEST at " +
                            String.format("%.1f, %.1f", playerPos.x(), playerPos.z()),
                    NamedTextColor.YELLOW));
            testEntity.setCustomNameVisible(true);  // This can be on entity or meta

            testEntity.setInstance(instance, playerPos);

            player.sendMessage(Component.text(
                    "Spawned armor stand at your location: " +
                            String.format("%.1f, %.1f, %.1f", playerPos.x(), playerPos.y(), playerPos.z()),
                    NamedTextColor.GREEN
            ));

            // Check visibility after delay
            MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                boolean canSee = testEntity.getViewers().contains(player);
                player.sendMessage(Component.text(
                        "Can you see the armor stand? (Server thinks: " + canSee + ")",
                        NamedTextColor.YELLOW
                ));
                return TaskSchedule.stop();
            }, TaskSchedule.tick(10));
        });

        // Test 2: Spawn at problem coordinates
        Command test2 = new Command("test2");
        test2.setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            Instance instance = player.getInstance();
            if (instance == null) return;

            Pos problemPos = new Pos(170, 70, 170);

            Entity testEntity = new Entity(EntityType.ARMOR_STAND);
            ArmorStandMeta meta = (ArmorStandMeta) testEntity.getEntityMeta();
            testEntity.set(DataComponents.CUSTOM_NAME,
                    Component.text("PROBLEM COORDS 170,170", NamedTextColor.RED));
            meta.setCustomNameVisible(true);
            meta.setHasGlowingEffect(true);

            testEntity.setInstance(instance, problemPos);

            player.sendMessage(Component.text(
                    "Spawned armor stand at PROBLEM coordinates 170, 70, 170",
                    NamedTextColor.RED
            ));
            player.sendMessage(Component.text(
                    "Teleport there with: /tp 170 70 170",
                    NamedTextColor.YELLOW
            ));
        });

        // Test 3: Debug info
        Command test3 = new Command("test3");
        test3.setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            Instance instance = player.getInstance();
            if (instance == null) return;

            Pos pos = player.getPosition();
            int chunkX = pos.chunkX();
            int chunkZ = pos.chunkZ();

            player.sendMessage(Component.text("=== DEBUG INFO ===", NamedTextColor.GOLD));
            player.sendMessage(Component.text(String.format(
                    "Your position: %.1f, %.1f, %.1f",
                    pos.x(), pos.y(), pos.z()
            ), NamedTextColor.YELLOW));
            player.sendMessage(Component.text(String.format(
                    "Your chunk: %d, %d",
                    chunkX, chunkZ
            ), NamedTextColor.YELLOW));

            // Count nearby entities
            int entityCount = 0;
            for (Entity entity : instance.getEntities()) {
                if (entity == player) continue;
                double distance = entity.getPosition().distance(pos);
                if (distance < 50) {
                    entityCount++;
                    player.sendMessage(Component.text(String.format(
                            "- %s at %.1f blocks away (%.1f, %.1f, %.1f)",
                            entity.getEntityType().name(),
                            distance,
                            entity.getPosition().x(),
                            entity.getPosition().y(),
                            entity.getPosition().z()
                    ), NamedTextColor.GRAY));
                }
            }

            player.sendMessage(Component.text(
                    "Found " + entityCount + " entities within 50 blocks",
                    NamedTextColor.GREEN
            ));

            // Check viewer status
            player.sendMessage(Component.text(
                    "You have " + player.getViewers().size() + " viewers",
                    NamedTextColor.AQUA
            ));
        });

        // Test visibility directly
        Command testVisible = new Command("testvisible");
        testVisible.setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            player.sendMessage(Component.text("=== VISIBILITY TEST ===", NamedTextColor.GOLD));

            // Test if player can see themselves (F5 mode)
            player.sendMessage(Component.text(
                    "Press F5 - Can you see yourself in third person?",
                    NamedTextColor.YELLOW
            ));

            // Report current coordinates
            Pos pos = player.getPosition();
            boolean inProblemRange = (pos.x() >= 160 && pos.x() <= 224) ||
                    (pos.z() >= 160 && pos.z() <= 224);

            player.sendMessage(Component.text(String.format(
                    "Current coords: %.1f, %.1f (Problem range: %s)",
                    pos.x(), pos.z(),
                    inProblemRange ? "YES" : "NO"
            ), inProblemRange ? NamedTextColor.RED : NamedTextColor.GREEN));

            // Get entity tracking info
            player.sendMessage(Component.text(
                    "Viewers: " + player.getViewers().size() +
                            " | Chunk: " + pos.chunkX() + "," + pos.chunkZ(),
                    NamedTextColor.AQUA
            ));
        });

        // Simple TP command
        Command tp = new Command("tp");
        tp.addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            double x = context.get("x");
            double y = context.get("y");
            double z = context.get("z");

            player.teleport(new Pos(x, y, z));
            player.sendMessage(Component.text(
                    String.format("Teleported to %.1f, %.1f, %.1f", x, y, z),
                    NamedTextColor.GREEN
            ));
        }, ArgumentType.Double("x"), ArgumentType.Double("y"), ArgumentType.Double("z"));

        // Register all commands
        commandManager.register(test1);
        commandManager.register(test2);
        commandManager.register(test3);
        commandManager.register(testVisible);
        commandManager.register(tp);
    }
}
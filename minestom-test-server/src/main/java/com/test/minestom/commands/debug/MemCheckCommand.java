package com.test.minestom.commands.debug;

import com.minestom.mechanics.damage.DamageFeature;
import com.minestom.mechanics.systems.blocking.BlockingSystem;
import com.test.minestom.gui.GuiManager;
import com.minestom.mechanics.util.PlayerCleanupManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/**
 * Debug command to check for memory leaks.
 * Shows how many entries are in each system's maps.
 * If numbers are higher than online players = LEAK!
 */
public class MemCheckCommand extends Command {

    public MemCheckCommand() {
        super("memcheck", "memorycheck", "leakcheck");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players!",
                        NamedTextColor.RED));
                return;
            }

            int onlinePlayers = MinecraftServer.getConnectionManager()
                    .getOnlinePlayerCount();

            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("═══ Memory Leak Check ═══",
                    NamedTextColor.GOLD, TextDecoration.BOLD));
            player.sendMessage(Component.empty());

            // Check each system
            checkSystem(player, "DamageFeature",
                    DamageFeature.getInstance().getTrackedEntities(), onlinePlayers);
            checkSystem(player, "BlockingSystem",
                    BlockingSystem.getInstance().getActiveCount(), 0); // Blocking can be 0
            checkSystem(player, "GuiManager",
                    GuiManager.getInstance().getOpenGuisCount(), 0); // GUIs can be 0

            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("Online Players: ", NamedTextColor.GRAY)
                    .append(Component.text(onlinePlayers, NamedTextColor.YELLOW)));

            // Cleanup stats
            var stats = PlayerCleanupManager.getStats();
            player.sendMessage(Component.text(
                    String.format("Cleanup Stats: %d total, %d failed (%.1f%%)",
                            stats.totalCleanups(),
                            stats.totalFailures(),
                            stats.failureRate() * 100),
                    NamedTextColor.GRAY));

            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("═════════════════════════",
                    NamedTextColor.GOLD, TextDecoration.BOLD));
        });
    }

    private void checkSystem(Player player, String name, int entries, int expected) {
        boolean leak = entries > expected;

        Component message = Component.text("  " + name + ": ", NamedTextColor.GRAY)
                .append(Component.text(entries, leak ? NamedTextColor.RED : NamedTextColor.GREEN));

        if (leak) {
            message = message.append(Component.text(" ⚠ LEAK!",
                    NamedTextColor.RED, TextDecoration.BOLD));
        } else {
            message = message.append(Component.text(" ✓", NamedTextColor.GREEN));
        }

        player.sendMessage(message);
    }
}
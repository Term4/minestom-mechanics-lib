package com.test.minestom.commands.combat;

import com.test.minestom.gui.views.BlockingSettingsGui;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Player command to open blocking preferences GUI
 * Usage: /block or /blocking
 */
public class BlockingCommand extends Command {

    public BlockingCommand() {
        super("block", "blocking");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
                return;
            }

            BlockingSettingsGui.open(player);
        });
    }
}
package com.test.minestom.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

/**
 * Sets the sender to creative mode. Usage: /gmc
 */
public class GmcCommand extends Command {

    public GmcCommand() {
        super("gmc");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
                return;
            }
            player.setGameMode(GameMode.CREATIVE);
            sender.sendMessage(Component.text("Set game mode to Creative.", NamedTextColor.GREEN));
        });
    }
}

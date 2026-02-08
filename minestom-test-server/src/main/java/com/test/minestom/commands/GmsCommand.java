package com.test.minestom.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

/**
 * Sets the sender to survival mode. Usage: /gms
 */
public class GmsCommand extends Command {

    public GmsCommand() {
        super("gms");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
                return;
            }
            player.setGameMode(GameMode.SURVIVAL);
            sender.sendMessage(Component.text("Set game mode to Survival.", NamedTextColor.GREEN));
        });
    }
}

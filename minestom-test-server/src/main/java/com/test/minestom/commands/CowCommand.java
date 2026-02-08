package com.test.minestom.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;

/**
 * Spawns a cow at the sender's location. Usage: /cow
 */
public class CowCommand extends Command {

    public CowCommand() {
        super("cow");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
                return;
            }
            if (player.getInstance() == null) {
                sender.sendMessage(Component.text("You must be in a world to spawn a cow.", NamedTextColor.RED));
                return;
            }
            Entity cow = new Entity(EntityType.COW);
            cow.setInstance(player.getInstance(), player.getPosition());
            sender.sendMessage(Component.text("Spawned a cow.", NamedTextColor.GREEN));
        });
    }
}

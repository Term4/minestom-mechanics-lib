package com.test.minestom.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

/**
 * /setfire — places fire on the block the sender is standing on.
 */
public class SetFireCommand extends Command {

    public SetFireCommand() {
        super("setfire");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
                return;
            }

            Instance instance = player.getInstance();
            if (instance == null) return;

            Pos pos = player.getPosition();
            int x = pos.blockX();
            int y = pos.blockY();
            int z = pos.blockZ();

            // Place fire at the player's feet position
            instance.setBlock(x, y, z, Block.FIRE);
            player.sendMessage(Component.text("Fire set at " + x + ", " + y + ", " + z, NamedTextColor.GOLD));
        });
    }
}

package com.test.minestom.commands.combat;

import com.minestom.mechanics.systems.health.events.BlockingDamageEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;

import static com.test.minestom.misc.MessageBuilder.*;

/**
 * /dr on|off - Toggle blocking damage reduction display above the hotbar (action bar).
 * When on, successful blocks show "Blocked! X → Y" above the hotbar.
 */
public class DrCommand extends net.minestom.server.command.builder.Command {

    private static final Tag<Boolean> DR_DISPLAY = Tag.Boolean("dr_display").defaultValue(false);

    private static boolean listenerRegistered = false;

    public DrCommand() {
        super("dr");
        setupCommands();
    }

    private void setupCommands() {
        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            boolean currentlyOn = Boolean.TRUE.equals(player.getTag(DR_DISPLAY));
            player.sendMessage(Component.text("Damage reduction display: ", LABEL)
                    .append(toggle(currentlyOn))
                    .append(Component.text(" (use /dr on or /dr off)", MUTED)));
        });

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            player.setTag(DR_DISPLAY, true);
            player.sendMessage(Component.text("Damage reduction display ", LABEL)
                    .append(Component.text("ON", SUCCESS))
                    .append(Component.text(" - blocked hits will show above hotbar", MUTED)));
        }, net.minestom.server.command.builder.arguments.ArgumentType.Literal("on"));

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            player.setTag(DR_DISPLAY, false);
            player.sendMessage(Component.text("Damage reduction display ", LABEL)
                    .append(Component.text("OFF", ERROR)));
        }, net.minestom.server.command.builder.arguments.ArgumentType.Literal("off"));
    }

    /**
     * Register the BlockingDamageEvent listener. Call once at server init.
     */
    public static void registerListener() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        MinecraftServer.getGlobalEventHandler().addListener(BlockingDamageEvent.class, event -> {
            if (!Boolean.TRUE.equals(event.victim().getTag(DR_DISPLAY))) return;
            event.victim().sendActionBar(Component.text(PREFIX_BLOCKING + " Blocked! ", SUCCESS)
                    .append(Component.text(String.format("%.1f", event.originalAmount()), VALUE))
                    .append(Component.text(" → ", MUTED))
                    .append(Component.text(String.format("%.1f", event.reducedAmount()), SUCCESS)));
        });
    }
}

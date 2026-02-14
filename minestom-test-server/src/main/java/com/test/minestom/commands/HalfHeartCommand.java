package com.test.minestom.commands;

import com.minestom.mechanics.systems.health.HealthSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.entity.Player;

/**
 * Debug command: reduces the sender's health by half a heart.
 * Does NOT use damage() — for testing hurt/tilt behavior on 1.8 clients.
 * <p>
 * Modes: normal, metadata, give_absorb, absorption
 */
public class HalfHeartCommand extends Command {

    public enum Mode {
        /** Baseline: setHealth(newHealth) directly */
        normal,
        /** Silent path: setHealthWithoutHurtEffect (metadata for 1.8, max trick for modern) */
        metadata,
        /** Give 2 absorption hearts so you can test absorption mode */
        give_absorb,
        /** Reduce absorption by 1 only (no setHealth). Need absorption first - use give_absorb. */
        absorption
    }

    public HalfHeartCommand() {
        super("halfheart");

        var modeArg = new ArgumentEnum<>("mode", Mode.class);
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                return;
            }
            Mode mode = context.get(modeArg);
            applyHalfHeart(player, mode);
        }, modeArg);

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                return;
            }
            sender.sendMessage(Component.text("Usage: /halfheart <mode>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Modes: normal, metadata, give_absorb, absorption", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Defaulting to 'normal' for this run.", NamedTextColor.DARK_GRAY));
            applyHalfHeart(player, Mode.normal);
        });
    }

    private void applyHalfHeart(Player player, Mode mode) {
        float current = player.getHealth();
        float newHealth = Math.max(0.5f, current - 1f);
        String desc = "";

        switch (mode) {
            case normal -> {
                player.setHealth(newHealth);
                desc = "setHealth direct";
            }
            case metadata -> {
                HealthSystem.setHealthWithoutHurtEffect(player, newHealth);
                desc = "silent (metadata / max trick)";
            }
            case give_absorb -> {
                player.setAdditionalHearts(player.getAdditionalHearts() + 2f);
                desc = "+2 absorption (run 'absorption' to test reduce)";
                player.sendMessage(Component.text()
                        .append(Component.text("+2 absorption. ", NamedTextColor.GREEN))
                        .append(Component.text("Run /halfheart absorption to test if reducing absorption triggers tilt.", NamedTextColor.GRAY))
                        .build());
                return;
            }
            case absorption -> {
                float absorb = player.getAdditionalHearts();
                if (absorb >= 1f) {
                    player.setAdditionalHearts(Math.max(0, absorb - 1f));
                    desc = "absorption -1 only (no setHealth)";
                } else {
                    player.sendMessage(Component.text("No absorption - use /halfheart give_absorb first.", NamedTextColor.RED));
                    return;
                }
            }
            default -> desc = "?";
        }

        player.sendMessage(Component.text()
                .append(Component.text("Health: ", NamedTextColor.GRAY))
                .append(Component.text(current + " → " + newHealth, NamedTextColor.YELLOW))
                .append(Component.text(" [", NamedTextColor.DARK_GRAY))
                .append(Component.text(desc, NamedTextColor.AQUA))
                .append(Component.text("]", NamedTextColor.DARK_GRAY))
                .build());
    }
}

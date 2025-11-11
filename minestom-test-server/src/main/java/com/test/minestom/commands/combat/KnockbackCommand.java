package com.test.minestom.commands.combat;

import com.minestom.mechanics.systems.knockback.KnockbackSystem;
import com.test.minestom.misc.CommandHelpBuilder;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

import static com.test.minestom.misc.MessageBuilder.*;

/**
 * Knockback command - Shows current knockback configuration.
 * For runtime modifications, use the Universal Tag System.
 */
public class KnockbackCommand extends Command {

    private final KnockbackSystem system;

    public KnockbackCommand() {
        super("kb", "knockback");
        this.system = KnockbackSystem.getInstance();
        setupCommands();
    }

    private void setupCommands() {
        // Default - show current settings
        setDefaultExecutor(this::showCurrent);

        // /kb help
        addSyntax((sender, context) -> showHelp(sender),
                ArgumentType.Literal("help"));

        // /kb info - Show detailed info
        addSyntax((sender, context) -> showDetailedInfo(sender),
                ArgumentType.Literal("info"));
    }

    private void showHelp(CommandSender sender) {
        CommandHelpBuilder.create("knockback")
                .description("View knockback configuration")
                .addUsage("/kb", "Show current knockback configuration")
                .addUsage("/kb info", "Show detailed configuration")
                .addNote("This shows the BASE configuration set at server startup")
                .addNote("For runtime changes, use the Universal Tag System:")
                .addNote("  - KnockbackSystem.MULTIPLIER")
                .addNote("  - KnockbackSystem.MODIFY")
                .addNote("  - KnockbackSystem.CUSTOM")
                .send(sender);
    }

    private void showCurrent(CommandSender sender, Object context) {
        var config = system.getConfig();

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text()
                .append(Component.text("Base Knockback Configuration", PRIMARY))
                .build());

        sender.sendMessage(Component.text()
                .append(Component.text("  Modern: ", LABEL))
                .append(Component.text(config.modern() ? "Yes" : "No",
                        config.modern() ? SUCCESS : VALUE))
                .build());

        sender.sendMessage(Component.text()
                .append(Component.text("  Base: ", LABEL))
                .append(Component.text(
                        String.format("H=%.3f, V=%.3f", config.horizontal(), config.vertical()),
                        VALUE))
                .build());

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text()
                .append(Component.text("💡 ", INFO))
                .append(Component.text("Use tags for runtime modifications", LABEL))
                .build());

        sender.sendMessage(Component.text()
                .append(Component.text("Use ", LABEL))
                .append(Component.text("/kb help", ACCENT))
                .append(Component.text(" for more info", LABEL))
                .build());
    }

    private void showDetailedInfo(CommandSender sender) {
        var config = system.getConfig();

        sender.sendMessage(Component.empty());
        sender.sendMessage(header("Knockback Configuration"));

        // Config type
        sender.sendMessage(Component.empty());
        sender.sendMessage(labelValue("Type", config.modern() ? "Modern" : "Legacy",
                config.modern() ? SUCCESS : VALUE));

        // Base values
        sender.sendMessage(Component.empty());
        sender.sendMessage(headerSimple("Base Values"));
        sender.sendMessage(labelValue("  Horizontal", String.format("%.3f", config.horizontal())));
        sender.sendMessage(labelValue("  Vertical", String.format("%.3f", config.vertical())));
        sender.sendMessage(labelValue("  Vertical Limit", String.format("%.3f", config.verticalLimit())));

        // Sprint bonus
        sender.sendMessage(Component.empty());
        sender.sendMessage(headerSimple("Sprint Bonus"));
        sender.sendMessage(labelValue("  Horizontal", String.format("%.3f", config.sprintBonusHorizontal())));
        sender.sendMessage(labelValue("  Vertical", String.format("%.3f", config.sprintBonusVertical())));

        // Air multipliers
        sender.sendMessage(Component.empty());
        sender.sendMessage(headerSimple("Air Multipliers"));
        sender.sendMessage(labelValue("  Horizontal", String.format("%.2fx", config.airMultiplierHorizontal())));
        sender.sendMessage(labelValue("  Vertical", String.format("%.2fx", config.airMultiplierVertical())));

        // Other settings
        sender.sendMessage(Component.empty());
        sender.sendMessage(headerSimple("Other Settings"));
        sender.sendMessage(labelValue("  Look Weight", String.format("%.0f%%", config.lookWeight() * 100)));

        // Tag system info
        sender.sendMessage(Component.empty());
        sender.sendMessage(headerSimple("Runtime Modifications"));
        sender.sendMessage(Component.text("  Use the Universal Tag System for runtime changes:", LABEL));
        sender.sendMessage(Component.text("  • KnockbackSystem.MULTIPLIER", INFO));
        sender.sendMessage(Component.text("  • KnockbackSystem.MODIFY", INFO));
        sender.sendMessage(Component.text("  • KnockbackSystem.CUSTOM", INFO));

        sender.sendMessage(Component.empty());
        sender.sendMessage(separator());
    }
}
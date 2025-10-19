package com.minestom.mechanics.util;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

import static com.minestom.mechanics.util.MessageBuilder.*;

// TODO: Move to a different library..? Seems unnecessary for a mechanics library
//  quick fix would be to just move it to my test server for the time being

/**
 * Builder for creating consistent command help pages.
 * Compatible with all Adventure API versions.
 *
 * Usage:
 * <pre>
 * CommandHelpBuilder.create("knockback")
 *     .description("Configure knockback settings")
 *     .addUsage("/kb", "Show current settings")
 *     .addUsage("/kb set &lt;profile&gt;", "Change knockback profile")
 *     .addUsage("/kb info", "Show detailed information")
 *     .send(player);
 * </pre>
 */
public class CommandHelpBuilder {

    private final String commandName;
    private String description;
    private final List<CommandUsage> usages = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    private CommandHelpBuilder(String commandName) {
        this.commandName = commandName;
    }

    public static CommandHelpBuilder create(String commandName) {
        return new CommandHelpBuilder(commandName);
    }

    public CommandHelpBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CommandHelpBuilder addUsage(String usage, String description) {
        this.usages.add(new CommandUsage(usage, description));
        return this;
    }

    public CommandHelpBuilder addNote(String note) {
        this.notes.add(note);
        return this;
    }

    /**
     * Send the help message directly to a CommandSender.
     * This is the recommended method - simpler and more reliable.
     */
    public void send(CommandSender sender) {
        sender.sendMessage(Component.empty());

        // Header
        sender.sendMessage(header(commandName.toUpperCase() + " Command"));

        // Description
        if (description != null) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text(description, LABEL));
        }

        // Usage section
        if (!usages.isEmpty()) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(headerSimple("Usage:"));
            for (CommandUsage usage : usages) {
                sender.sendMessage(command(usage.usage, usage.description));
            }
        }

        // Notes section
        if (!notes.isEmpty()) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(headerSimple("Notes:"));
            for (String note : notes) {
                sender.sendMessage(bullet(note));
            }
        }

        sender.sendMessage(Component.empty());
    }

    /**
     * Build as a single Component (alternative method).
     * Note: Use send() instead if possible - it's more reliable.
     */
    public Component build() {
        List<Component> components = new ArrayList<>();

        components.add(Component.empty());
        components.add(header(commandName.toUpperCase() + " Command"));

        if (description != null) {
            components.add(Component.empty());
            components.add(Component.text(description, LABEL));
        }

        if (!usages.isEmpty()) {
            components.add(Component.empty());
            components.add(headerSimple("Usage:"));
            for (CommandUsage usage : usages) {
                components.add(command(usage.usage, usage.description));
            }
        }

        if (!notes.isEmpty()) {
            components.add(Component.empty());
            components.add(headerSimple("Notes:"));
            for (String note : notes) {
                components.add(bullet(note));
            }
        }

        components.add(Component.empty());

        // Join all components with newlines
        Component result = Component.empty();
        for (int i = 0; i < components.size(); i++) {
            result = result.append(components.get(i));
            if (i < components.size() - 1) {
                result = result.append(Component.newline());
            }
        }

        return result;
    }

    private static class CommandUsage {
        final String usage;
        final String description;

        CommandUsage(String usage, String description) {
            this.usage = usage;
            this.description = description;
        }
    }
}

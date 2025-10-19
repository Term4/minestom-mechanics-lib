package com.test.minestom.commands;

import com.test.minestom.commands.combat.BlockingCommand;
import com.test.minestom.commands.combat.BlockingConfigCommand;
import com.test.minestom.commands.combat.KnockbackCommand;
import com.test.minestom.commands.debug.MemCheckCommand;
import com.test.minestom.gui.views.BlockingSettingsGui;
import com.minestom.mechanics.manager.CombatManager;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

import static com.minestom.mechanics.util.MessageBuilder.*;

/**
 * Central registry for all server commands.
 * Handles registration, initialization, and management of commands.
 *
 * Updated to work with CombatManager architecture!
 */
public class CommandRegistry {
    private static final Map<String, Command> registeredCommands = new HashMap<>();

    // Command categories for organization
    public enum CommandCategory {
        COMBAT("Combat Commands", NamedTextColor.RED),
        ADMIN("Admin Commands", NamedTextColor.GOLD),
        UTILITY("Utility Commands", NamedTextColor.AQUA),
        DEBUG("Debug Commands", NamedTextColor.GRAY);

        private final String displayName;
        private final NamedTextColor color;

        CommandCategory(String displayName, NamedTextColor color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public NamedTextColor getColor() { return color; }
    }

    /**
     * Command metadata for better organization
     */
    public static class CommandInfo {
        private final Command command;
        private final CommandCategory category;
        private final String description;
        private final String[] aliases;
        private final boolean enabledByDefault;

        public CommandInfo(Command command, CommandCategory category,
                           String description, boolean enabledByDefault, String... aliases) {
            this.command = command;
            this.category = category;
            this.description = description;
            this.aliases = aliases;
            this.enabledByDefault = enabledByDefault;
        }

        public Command getCommand() { return command; }
        public CommandCategory getCategory() { return category; }
        public String getDescription() { return description; }
        public String[] getAliases() { return aliases; }
        public boolean isEnabledByDefault() { return enabledByDefault; }
    }

    private static final List<CommandInfo> COMMAND_LIST = new ArrayList<>();

    /**
     * Initialize and register all commands
     */
    private static final LogUtil.SystemLogger log = LogUtil.system("CommandRegistry");

    public static void initialize() {
        log.debug("Initializing command system...");

        // Register combat commands
        registerCombatCommands();

        // Register admin commands
        registerAdminCommands();

        // Register utility commands
        registerUtilityCommands();

        // Register debug commands
        registerDebugCommands();

        // Actually register all commands with Minestom
        registerAllCommands();

        // Setup join message with command info
        setupJoinMessages();

        // Initialize BlockingSettingsGui with BlockingSystem reference
        try {
            var blocking = CombatManager.getInstance().getBlockingSystem();
            BlockingSettingsGui.setBlockingSystem(blocking);
            log.debug("[CommandRegistry] BlockingSettingsGui initialized");
        } catch (Exception e) {
            log.warn("[CommandRegistry] Could not initialize BlockingSettingsGui: " + e.getMessage());
        }

        log.debug("[CommandRegistry] Successfully registered " + COMMAND_LIST.size() + " commands");
    }

    private static void registerCombatCommands() {
        // Knockback command
        COMMAND_LIST.add(new CommandInfo(
                new KnockbackCommand(),
                CommandCategory.COMBAT,
                "Configure knockback settings and profiles",
                true,
                "kb", "knockback"
        ));

        // Blocking player command (opens GUI)
        COMMAND_LIST.add(new CommandInfo(
                new BlockingCommand(),
                CommandCategory.COMBAT,
                "Open blocking preferences menu",
                true,
                "block", "blocking"
        ));

        // Blocking config command (admin)
        COMMAND_LIST.add(new CommandInfo(
                new BlockingConfigCommand(),
                CommandCategory.COMBAT,
                "Configure blocking system (Admin)",
                true,
                "blockconfig", "bconfig"
        ));
    }

    private static void registerAdminCommands() {
        // Add admin commands here
        // Example:
        /*
        COMMAND_LIST.add(new CommandInfo(
            new GamemodeCommand(),
            CommandCategory.ADMIN,
            "Change player gamemode",
            true,
            "gm", "gamemode"
        ));
        */
    }

    private static void registerUtilityCommands() {
        // Add utility commands here
        // Example:
        /*
        COMMAND_LIST.add(new CommandInfo(
            new HelpCommand(),
            CommandCategory.UTILITY,
            "Display help information",
            true,
            "help", "?"
        ));
        */
    }

    private static void registerAllCommands() {
        var commandManager = MinecraftServer.getCommandManager();

        for (CommandInfo info : COMMAND_LIST) {
            if (info.isEnabledByDefault()) {
                commandManager.register(info.getCommand());
                registeredCommands.put(info.getCommand().getName(), info.getCommand());

                log.debug(String.format("[CommandRegistry] Registered %s command: /%s",
                        info.getCategory().name(), info.getCommand().getName()));
            }
        }
    }

    private static void setupJoinMessages() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                var player = event.getPlayer();

                // Send welcome message with command categories
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("Welcome to ", LABEL)
                        .append(Component.text("PvP Server", PRIMARY, TextDecoration.BOLD)));

                player.sendMessage(Component.text("Available Commands:", VALUE));

                // Group commands by category
                Map<CommandCategory, List<CommandInfo>> byCategory = new HashMap<>();
                for (CommandInfo info : COMMAND_LIST) {
                    if (info.isEnabledByDefault()) {
                        byCategory.computeIfAbsent(info.getCategory(), k -> new ArrayList<>()).add(info);
                    }
                }

                // Display each category
                for (CommandCategory category : CommandCategory.values()) {
                    List<CommandInfo> commands = byCategory.get(category);
                    if (commands != null && !commands.isEmpty()) {
                        player.sendMessage(Component.empty());
                        player.sendMessage(Component.text(category.getDisplayName() + ":", category.getColor(), TextDecoration.BOLD));

                        for (CommandInfo info : commands) {
                            player.sendMessage(Component.text("  • ", MUTED)
                                    .append(Component.text("/" + info.getCommand().getName(), ACCENT))
                                    .append(Component.text(" - " + info.getDescription(), LABEL)));
                        }
                    }
                }

                player.sendMessage(Component.empty());
            }
        });
    }

    // ===========================
    // COMMAND MANAGEMENT
    // ===========================

    /**
     * Register a single command at runtime
     */
    public static boolean registerCommand(Command command) {
        if (registeredCommands.containsKey(command.getName())) {
            log.warn("[CommandRegistry] Command already registered: " + command.getName());
            return false;
        }

        MinecraftServer.getCommandManager().register(command);
        registeredCommands.put(command.getName(), command);
        log.debug("[CommandRegistry] Registered command: " + command.getName());
        return true;
    }

    /**
     * Unregister a command by name
     */
    public static boolean unregisterCommand(String commandName) {
        Command command = registeredCommands.remove(commandName);
        if (command != null) {
            MinecraftServer.getCommandManager().unregister(command);
            log.info("[CommandRegistry] Unregistered command: " + commandName);
            return true;
        }
        return false;
    }

    /**
     * Get a registered command by name
     */
    public static Optional<Command> getCommand(String commandName) {
        return Optional.ofNullable(registeredCommands.get(commandName));
    }

    /**
     * Get all registered commands
     */
    public static Collection<Command> getAllCommands() {
        return Collections.unmodifiableCollection(registeredCommands.values());
    }

    /**
     * Get command info list
     */
    public static List<CommandInfo> getCommandInfoList() {
        return Collections.unmodifiableList(COMMAND_LIST);
    }

    /**
     * Check if a command is registered
     */
    public static boolean isCommandRegistered(String commandName) {
        return registeredCommands.containsKey(commandName);
    }

    private static void registerDebugCommands() {
        // Memory leak checker
        COMMAND_LIST.add(new CommandInfo(
                new MemCheckCommand(),
                CommandCategory.DEBUG,
                "Check for memory leaks in tracked systems",
                true,
                "memcheck", "leakcheck"
        ));
    }

    /**
     * Reload all commands (useful for configuration changes)
     */
    public static void reload() {
        log.info("[CommandRegistry] Reloading commands...");

        // Unregister all current commands
        var commandManager = MinecraftServer.getCommandManager();
        for (Command command : registeredCommands.values()) {
            commandManager.unregister(command);
        }
        registeredCommands.clear();
        COMMAND_LIST.clear();

        // Re-initialize
        initialize();
    }
}
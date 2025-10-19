package com.test.minestom.commands.combat;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.config.knockback.KnockbackPresets;
import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.util.CommandHelpBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.minestom.mechanics.util.MessageBuilder.*;

/**
 * Knockback command for 1.8 PvP with advanced features.
 * Updated to use KnockbackConfig instead of KnockbackProfile enum.
 */
public class KnockbackCommand extends Command {

    private final KnockbackHandler handler;

    // Available presets
    private static final Map<String, KnockbackConfig> PRESETS = new LinkedHashMap<>();
    static {
        PRESETS.put("vanilla", KnockbackPresets.vanilla18());
        PRESETS.put("minemen", KnockbackPresets.minemen());
        PRESETS.put("hypixel", KnockbackPresets.hypixel());
    }

    public KnockbackCommand() {
        super("kb", "knockback");
        this.handler = KnockbackHandler.getInstance();
        setupCommands();
    }

    private void setupCommands() {
        // Default - show current settings
        setDefaultExecutor(this::showCurrent);

        // /kb help
        addSyntax((sender, context) -> showHelp(sender),
                ArgumentType.Literal("help"));

        // /kb list - List all profiles
        addSyntax((sender, context) -> showList(sender),
                ArgumentType.Literal("list"));

        // /kb set <profile> - Change profile
        var profileArg = ArgumentType.Word("profile").from("vanilla", "minemen", "hypixel");
        addSyntax((sender, context) -> {
            String profileName = context.get(profileArg);
            changeProfile(sender, profileName);
        }, ArgumentType.Literal("set"), profileArg);

        // /kb info - Show detailed info
        addSyntax((sender, context) -> showDetailedInfo(sender),
                ArgumentType.Literal("info"));

        // /kb sync <on|off> - Toggle knockback sync
        addSyntax((sender, context) -> {
                    String state = context.get("sync_state");
                    toggleSync(sender, state.equals("on"));
                }, ArgumentType.Literal("sync"),
                ArgumentType.Word("sync_state").from("on", "off"));

        // /kb air <horizontal> <vertical> - Set custom air multipliers
        var hMultiplierArg = ArgumentType.Double("h_multiplier").between(0.5, 2.0);
        var vMultiplierArg = ArgumentType.Double("v_multiplier").between(0.5, 2.0);

        addSyntax((sender, context) -> {
            double horizontal = context.get(hMultiplierArg);
            double vertical = context.get(vMultiplierArg);
            setAirMultipliers(sender, horizontal, vertical);
        }, ArgumentType.Literal("air"), hMultiplierArg, vMultiplierArg);

        // /kb air reset - Reset air multipliers
        addSyntax((sender, context) -> resetAirMultipliers(sender),
                ArgumentType.Literal("air"), ArgumentType.Literal("reset"));
    }

    private void showHelp(CommandSender sender) {
        CommandHelpBuilder.create("knockback")
                .description("Configure knockback settings and profiles")
                .addUsage("/kb", "Show current knockback configuration")
                .addUsage("/kb list", "List all available profiles")
                .addUsage("/kb set <profile>", "Change to a different profile")
                .addUsage("/kb info", "Show detailed configuration")
                .addUsage("/kb sync <on|off>", "Toggle knockback sync (if supported)")
                .addUsage("/kb air <h> <v>", "Set custom air multipliers")
                .addUsage("/kb air reset", "Reset air multipliers to defaults")
                .addNote("Available profiles: vanilla, minemen, hypixel")
                .addNote("Sync is only available for certain profiles")
                .addNote("Air multipliers range from 0.5x to 2.0x")
                .send(sender);
    }

    private void showCurrent(CommandSender sender, Object context) {
        var config = handler.getserverDefaultConfig();

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text()
                .append(Component.text("Current Knockback Config", PRIMARY))
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

        // Show active features
        if (handler.hasCustomAirMultipliers()) {
            sender.sendMessage(info("Custom air multipliers active"));
        }

        if (handler.isKnockbackSyncEnabled()) {
            sender.sendMessage(Component.text()
                    .append(Component.text("  ⚡ ", SUCCESS))
                    .append(Component.text("Knockback sync enabled", SUCCESS))
                    .build());
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text()
                .append(Component.text("Use ", LABEL))
                .append(Component.text("/kb help", ACCENT))
                .append(Component.text(" for all commands", LABEL))
                .build());
    }

    private void showList(CommandSender sender) {
        var current = handler.getserverDefaultConfig();

        sender.sendMessage(Component.empty());
        sender.sendMessage(headerSimple("Available Knockback Profiles"));
        sender.sendMessage(Component.text("Use /kb set <profile> to change", LABEL));
        sender.sendMessage(Component.empty());

        for (var entry : PRESETS.entrySet()) {
            String name = entry.getKey();
            KnockbackConfig config = entry.getValue();
            boolean isCurrent = configEquals(config, current);

            var builder = Component.text()
                    .append(Component.text(isCurrent ? " ► " : "   ",
                            isCurrent ? SUCCESS : MUTED))
                    .append(Component.text(name.toUpperCase(),
                            isCurrent ? PRIMARY : LABEL));

            if (config.knockbackSyncSupported()) {
                builder.append(Component.text(" [SYNC]", INFO));
            }

            if (config.modern()) {
                builder.append(Component.text(" [MODERN]", SUCCESS));
            }

            builder.append(Component.text(" - ", MUTED))
                    .append(Component.text(
                            String.format("H=%.3f V=%.3f", config.horizontal(), config.vertical()),
                            isCurrent ? VALUE : LABEL));

            sender.sendMessage(builder.build());
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("[SYNC] = Supports knockback sync", INFO));
        sender.sendMessage(Component.text("[MODERN] = Modern knockback mechanics", INFO));
    }

    private void changeProfile(CommandSender sender, String profileName) {
        KnockbackConfig config = PRESETS.get(profileName.toLowerCase());
        if (config == null) {
            sender.sendMessage(error("Unknown profile: " + profileName));
            return;
        }

        var oldConfig = handler.getserverDefaultConfig();

        if (configEquals(config, oldConfig)) {
            sender.sendMessage(Component.text()
                    .append(Component.text("Already using profile: ", LABEL))
                    .append(Component.text(profileName, VALUE))
                    .build());
            return;
        }

        // Change config
        handler.setConfig(config);

        // Reset custom multipliers
        if (handler.hasCustomAirMultipliers()) {
            handler.resetAirMultipliers();
            sender.sendMessage(info("Reset custom air multipliers"));
        }

        // Handle sync compatibility
        if (config.knockbackSyncSupported() && !handler.isKnockbackSyncEnabled()) {
            sender.sendMessage(Component.text()
                    .append(Component.text("  ℹ This profile supports sync. Use ", LABEL))
                    .append(Component.text("/kb sync on", INFO))
                    .append(Component.text(" to enable", LABEL))
                    .build());
        } else if (!config.knockbackSyncSupported() && handler.isKnockbackSyncEnabled()) {
            handler.setKnockbackSyncEnabled(false);
            sender.sendMessage(warning("Knockback sync disabled (not supported)"));
        }

        // Broadcast change
        var message = broadcast(
                PREFIX_COMBAT,
                "Knockback changed to: " + profileName.toUpperCase()
        );

        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                player.sendMessage(message));

        // Log
        MinecraftServer.LOGGER.info("[Knockback] Profile changed to: " + profileName);
    }

    private void toggleSync(CommandSender sender, boolean enable) {
        var config = handler.getserverDefaultConfig();

        if (!config.knockbackSyncSupported()) {
            sender.sendMessage(error("Knockback sync not supported for current config"));

            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("Supported profiles:", LABEL));
            PRESETS.forEach((name, cfg) -> {
                if (cfg.knockbackSyncSupported()) {
                    sender.sendMessage(bullet(name.toUpperCase()));
                }
            });
            return;
        }

        if (handler.isKnockbackSyncEnabled() == enable) {
            sender.sendMessage(Component.text()
                    .append(Component.text("Knockback sync is already ", LABEL))
                    .append(status(enable))
                    .build());
            return;
        }

        handler.setKnockbackSyncEnabled(enable);

        // Broadcast
        var message = broadcast(
                "⚡",
                "Knockback Sync " + (enable ? "ENABLED" : "DISABLED")
        );

        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                player.sendMessage(message));
    }

    private void setAirMultipliers(CommandSender sender, double horizontal, double vertical) {
        handler.setCustomAirMultipliers(horizontal, vertical);

        var message = broadcast(
                "✈",
                String.format("Air Multipliers SET [H:%.1fx V:%.1fx]", horizontal, vertical)
        );

        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                player.sendMessage(message));

        sender.sendMessage(Component.text()
                .append(Component.text("Use ", LABEL))
                .append(Component.text("/kb air reset", INFO))
                .append(Component.text(" to restore defaults", LABEL))
                .build());
    }

    private void resetAirMultipliers(CommandSender sender) {
        if (!handler.hasCustomAirMultipliers()) {
            sender.sendMessage(Component.text("Already using profile defaults", LABEL));
            return;
        }

        handler.resetAirMultipliers();

        var message = broadcast("✈", "Air Multipliers RESET to profile defaults");

        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                player.sendMessage(message));
    }

    private void showDetailedInfo(CommandSender sender) {
        var config = handler.getserverDefaultConfig();

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

        boolean hasCustom = handler.hasCustomAirMultipliers();
        String hMultiplier = String.format("%.2fx", handler.getAirHorizontalMultiplier());
        String vMultiplier = String.format("%.2fx", handler.getAirVerticalMultiplier());

        if (hasCustom) {
            hMultiplier += " (custom)";
            vMultiplier += " (custom)";
        }

        sender.sendMessage(labelValue("  Horizontal", hMultiplier, hasCustom ? INFO : VALUE));
        sender.sendMessage(labelValue("  Vertical", vMultiplier, hasCustom ? INFO : VALUE));

        // Other settings
        sender.sendMessage(Component.empty());
        sender.sendMessage(headerSimple("Other Settings"));
        sender.sendMessage(labelValue("  Look Weight", String.format("%.0f%%", handler.getLookWeight() * 100)));

        String syncStatus = handler.isKnockbackSyncEnabled() ? "Enabled" :
                (config.knockbackSyncSupported() ? "Available" : "Not Supported");
        NamedTextColor syncColor = handler.isKnockbackSyncEnabled() ? SUCCESS :
                (config.knockbackSyncSupported() ? VALUE : ERROR);
        sender.sendMessage(labelValue("  KB Sync", syncStatus, syncColor));

        sender.sendMessage(Component.empty());
        sender.sendMessage(separator());
    }

    /**
     * Helper method to compare two configs for equality
     */
    private boolean configEquals(KnockbackConfig a, KnockbackConfig b) {
        return Math.abs(a.horizontal() - b.horizontal()) < 0.001 &&
                Math.abs(a.vertical() - b.vertical()) < 0.001 &&
                Math.abs(a.verticalLimit() - b.verticalLimit()) < 0.001 &&
                Math.abs(a.sprintBonusHorizontal() - b.sprintBonusHorizontal()) < 0.001 &&
                Math.abs(a.sprintBonusVertical() - b.sprintBonusVertical()) < 0.001;
    }
}
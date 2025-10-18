package com.test.minestom.commands.combat;

import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.features.knockback.KnockbackProfile;
import com.minestom.mechanics.util.CommandHelpBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

import static com.minestom.mechanics.util.MessageBuilder.*;


/**
 * Knockback command for 1.8 PvP with advanced features.
 * REFACTORED VERSION - Uses MessageBuilder for consistent formatting
 */
public class KnockbackCommand extends Command {

    private final KnockbackHandler handler;

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
        var profileArg = ArgumentType.Enum("profile", KnockbackProfile.class);
        addSyntax((sender, context) -> {
            KnockbackProfile profile = context.get(profileArg);
            changeProfile(sender, profile);
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
                .addUsage("/kb", "Show current knockback profile")
                .addUsage("/kb list", "List all available profiles")
                .addUsage("/kb set <profile>", "Change to a different profile")
                .addUsage("/kb info", "Show detailed configuration")
                .addUsage("/kb sync <on|off>", "Toggle knockback sync (if supported)")
                .addUsage("/kb air <h> <v>", "Set custom air multipliers")
                .addUsage("/kb air reset", "Reset air multipliers to defaults")
                .addNote("Sync is only available for certain profiles")
                .addNote("Air multipliers range from 0.5x to 2.0x")
                .send(sender);
    }

    private void showCurrent(CommandSender sender, Object context) {
        var profile = handler.getCurrentProfile();

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text()
                .append(Component.text("Current Profile: ", LABEL))
                .append(Component.text(profile.name(), PRIMARY))
                .build());

        sender.sendMessage(Component.text()
                .append(Component.text("  ", MUTED))
                .append(Component.text(profile.getDescription(), LABEL))
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
        var current = handler.getCurrentProfile();

        sender.sendMessage(Component.empty());
        sender.sendMessage(headerSimple("Available Knockback Profiles"));
        sender.sendMessage(Component.text("Use /kb set <profile> to change", LABEL));
        sender.sendMessage(Component.empty());

        for (KnockbackProfile profile : KnockbackProfile.values()) {
            boolean isCurrent = profile == current;

            var builder = Component.text()
                    .append(Component.text(isCurrent ? " ► " : "   ",
                            isCurrent ? SUCCESS : MUTED))
                    .append(Component.text(profile.name(),
                            isCurrent ? PRIMARY : LABEL));

            if (profile.isKnockbackSyncSupported()) {
                builder.append(Component.text(" [SYNC]", INFO));
            }

            builder.append(Component.text(" - ", MUTED))
                    .append(Component.text(profile.getDescription(),
                            isCurrent ? VALUE : LABEL));

            sender.sendMessage(builder.build());
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("[SYNC] = Supports knockback sync", INFO));
    }

    private void changeProfile(CommandSender sender, KnockbackProfile profile) {
        var oldProfile = handler.getCurrentProfile();

        if (oldProfile == profile) {
            sender.sendMessage(Component.text()
                    .append(Component.text("Already using profile: ", LABEL))
                    .append(Component.text(profile.name(), VALUE))
                    .build());
            return;
        }

        // Change profile
        handler.setProfile(profile);

        // Reset custom multipliers
        if (handler.hasCustomAirMultipliers()) {
            handler.resetAirMultipliers();
            sender.sendMessage(info("Reset custom air multipliers"));
        }

        // Handle sync compatibility
        if (profile.isKnockbackSyncSupported() && !handler.isKnockbackSyncEnabled()) {
            sender.sendMessage(Component.text()
                    .append(Component.text("  ℹ This profile supports sync. Use ", LABEL))
                    .append(Component.text("/kb sync on", INFO))
                    .append(Component.text(" to enable", LABEL))
                    .build());
        } else if (!profile.isKnockbackSyncSupported() && handler.isKnockbackSyncEnabled()) {
            handler.setKnockbackSyncEnabled(false);
            sender.sendMessage(warning("Knockback sync disabled (not supported)"));
        }

        // Broadcast change
        var message = broadcast(
                PREFIX_COMBAT,
                "Knockback changed: " + oldProfile.name() + " → " + profile.name()
        );

        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                player.sendMessage(message));

        // Log
        MinecraftServer.LOGGER.info("[Knockback] Profile changed: " +
                oldProfile.name() + " → " + profile.name());
    }

    private void toggleSync(CommandSender sender, boolean enable) {
        var profile = handler.getCurrentProfile();

        if (!profile.isKnockbackSyncSupported()) {
            sender.sendMessage(error("Knockback sync not supported for " + profile.name()));

            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("Supported profiles:", LABEL));
            for (KnockbackProfile p : KnockbackProfile.values()) {
                if (p.isKnockbackSyncSupported()) {
                    sender.sendMessage(bullet(p.name()));
                }
            }
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
        var profile = handler.getCurrentProfile();
        var settings = profile.getSettings();

        sender.sendMessage(Component.empty());
        sender.sendMessage(header("Knockback Configuration"));

        // Profile info
        sender.sendMessage(Component.empty());
        sender.sendMessage(labelValue("Profile", profile.name()));
        sender.sendMessage(labelValue("Description", profile.getDescription()));

        // Base values
        sender.sendMessage(Component.empty());
        sender.sendMessage(headerSimple("Base Values"));
        sender.sendMessage(labelValue("  Horizontal", String.format("%.3f", settings.horizontal())));
        sender.sendMessage(labelValue("  Vertical", String.format("%.3f", settings.vertical())));
        sender.sendMessage(labelValue("  Vertical Limit", String.format("%.3f", settings.verticalLimit())));

        // Sprint bonus
        sender.sendMessage(Component.empty());
        sender.sendMessage(headerSimple("Sprint Bonus"));
        sender.sendMessage(labelValue("  Extra Horizontal", String.format("%.3f", settings.extraHorizontal())));
        sender.sendMessage(labelValue("  Extra Vertical", String.format("%.3f", settings.extraVertical())));

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
        sender.sendMessage(labelValue("  Look Weight", String.format("%.0f%%", profile.getLookWeight() * 100)));

        String syncStatus = handler.isKnockbackSyncEnabled() ? "Enabled" :
                (profile.isKnockbackSyncSupported() ? "Available" : "Not Supported");
        NamedTextColor syncColor = handler.isKnockbackSyncEnabled() ? SUCCESS :
                (profile.isKnockbackSyncSupported() ? VALUE : ERROR);
        sender.sendMessage(labelValue("  KB Sync", syncStatus, syncColor));

        sender.sendMessage(Component.empty());
        sender.sendMessage(separator());
    }
}
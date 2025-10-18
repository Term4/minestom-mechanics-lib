package com.test.minestom.commands.combat;

import com.minestom.mechanics.manager.CombatManager;
import com.minestom.mechanics.config.blocking.BlockingConfig;
import com.minestom.mechanics.features.blocking.BlockingSystem;
import com.minestom.mechanics.util.CommandHelpBuilder;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.kyori.adventure.text.Component;

import static com.minestom.mechanics.util.MessageBuilder.*;

/**
 * Admin command to configure blocking system at runtime.
 * ✅ REFACTORED: Uses MessageBuilder for all output
 */
public class BlockingConfigCommand extends Command {

    public BlockingConfigCommand() {
        super("blockconfig", "bconfig");
        setupCommands();
    }

    private void setupCommands() {
        // Default - show current config
        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            showCurrentConfig(player);
        });

        // Help command
        addSyntax((sender, context) -> showHelp(sender),
                ArgumentType.Literal("help"));

        // Enable/disable blocking
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            CombatManager.getInstance().setBlockingEnabled(true);
            player.sendMessage(success("Blocking system ENABLED"));
        }, ArgumentType.Literal("enable"));

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            CombatManager.getInstance().setBlockingEnabled(false);
            player.sendMessage(error("Blocking system DISABLED"));
        }, ArgumentType.Literal("disable"));

        // Damage reduction
        var damageArg = ArgumentType.Literal("damage");
        var damageValueArg = ArgumentType.Double("percentage").between(0.0, 100.0);

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            double percentage = context.get(damageValueArg);
            double reduction = percentage / 100.0;

            BlockingSystem blocking = CombatManager.getInstance().getBlockingSystem();
            blocking.getConfig().setDamageReduction(reduction);

            player.sendMessage(success("Damage reduction set to " +
                    String.format("%.0f%%", percentage)));
        }, damageArg, damageValueArg);

        // Horizontal knockback reduction
        var hKnockbackArg = ArgumentType.Literal("hkb");
        var hKnockbackValueArg = ArgumentType.Double("percentage").between(0.0, 100.0);

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            double percentage = context.get(hKnockbackValueArg);
            double multiplier = 1.0 - (percentage / 100.0);

            BlockingSystem blocking = CombatManager.getInstance().getBlockingSystem();
            blocking.getConfig().setKnockbackHorizontalMultiplier(multiplier);

            player.sendMessage(success("Horizontal KB reduction set to " +
                    String.format("%.0f%%", percentage)));
        }, hKnockbackArg, hKnockbackValueArg);

        // Vertical knockback reduction
        var vKnockbackArg = ArgumentType.Literal("vkb");
        var vKnockbackValueArg = ArgumentType.Double("percentage").between(0.0, 100.0);

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            double percentage = context.get(vKnockbackValueArg);
            double multiplier = 1.0 - (percentage / 100.0);

            BlockingSystem blocking = CombatManager.getInstance().getBlockingSystem();
            blocking.getConfig().setKnockbackVerticalMultiplier(multiplier);

            player.sendMessage(success("Vertical KB reduction set to " +
                    String.format("%.0f%%", percentage)));
        }, vKnockbackArg, vKnockbackValueArg);

        // Toggle damage messages
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            BlockingSystem blocking = CombatManager.getInstance().getBlockingSystem();
            BlockingConfig config = blocking.getConfig();
            boolean newState = !config.isShowDamageMessages();
            config.setShowDamageMessages(newState);

            player.sendMessage(Component.text("Damage messages ", LABEL)
                    .append(status(newState)));
        }, ArgumentType.Literal("messages"));

        // Toggle block effects
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            BlockingSystem blocking = CombatManager.getInstance().getBlockingSystem();
            BlockingConfig config = blocking.getConfig();
            boolean newState = !config.isShowBlockEffects();
            config.setShowBlockEffects(newState);

            player.sendMessage(Component.text("Block effects ", LABEL)
                    .append(status(newState)));
        }, ArgumentType.Literal("effects"));

        // Presets
        var presetArg = ArgumentType.Literal("preset");
        var presetTypeArg = ArgumentType.Word("type").from("vanilla", "reduced", "minimal", "maximum");

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            String preset = context.get(presetTypeArg);
            BlockingSystem blocking = CombatManager.getInstance().getBlockingSystem();
            BlockingConfig config = blocking.getConfig();

            switch (preset) {
                case "vanilla" -> {
                    config.setDamageReduction(0.5);
                    config.setKnockbackHorizontalMultiplier(0.4);
                    config.setKnockbackVerticalMultiplier(0.4);
                    player.sendMessage(success("Applied VANILLA preset"));
                    player.sendMessage(Component.text("50% damage reduction, 60% KB reduction", LABEL));
                }
                case "reduced" -> {
                    config.setDamageReduction(0.25);
                    config.setKnockbackHorizontalMultiplier(0.7);
                    config.setKnockbackVerticalMultiplier(0.7);
                    player.sendMessage(success("Applied REDUCED preset"));
                    player.sendMessage(Component.text("25% damage reduction, 30% KB reduction", LABEL));
                }
                case "minimal" -> {
                    config.setDamageReduction(0.1);
                    config.setKnockbackHorizontalMultiplier(0.9);
                    config.setKnockbackVerticalMultiplier(0.9);
                    player.sendMessage(success("Applied MINIMAL preset"));
                    player.sendMessage(Component.text("10% damage reduction, 10% KB reduction", LABEL));
                }
                case "maximum" -> {
                    config.setDamageReduction(0.75);
                    config.setKnockbackHorizontalMultiplier(0.1);
                    config.setKnockbackVerticalMultiplier(0.1);
                    player.sendMessage(success("Applied MAXIMUM preset"));
                    player.sendMessage(Component.text("75% damage reduction, 90% KB reduction", LABEL));
                }
            }
        }, presetArg, presetTypeArg);
    }

    private void showHelp(net.minestom.server.command.CommandSender sender) {
        CommandHelpBuilder.create("blockconfig")
                .description("Configure the blocking system (admin command)")
                .addUsage("/blockconfig", "Show current configuration")
                .addUsage("/blockconfig enable/disable", "Toggle blocking system")
                .addUsage("/blockconfig damage <0-100>", "Set damage reduction percentage")
                .addUsage("/blockconfig hkb <0-100>", "Set horizontal KB reduction")
                .addUsage("/blockconfig vkb <0-100>", "Set vertical KB reduction")
                .addUsage("/blockconfig messages", "Toggle damage messages")
                .addUsage("/blockconfig effects", "Toggle block effects")
                .addUsage("/blockconfig preset <type>", "Apply preset (vanilla/reduced/minimal/maximum)")
                .addNote("Changes apply immediately to all players")
                .addNote("Use /block to configure personal preferences")
                .send(sender);
    }

    private void showCurrentConfig(Player player) {
        BlockingSystem blocking = CombatManager.getInstance().getBlockingSystem();
        BlockingConfig config = blocking.getConfig();

        player.sendMessage(Component.empty());
        player.sendMessage(header("Blocking Configuration"));
        player.sendMessage(Component.empty());

        // Status
        player.sendMessage(Component.text("Status: ", LABEL)
                .append(status(blocking.isEnabled())));

        player.sendMessage(Component.empty());

        // Damage reduction
        player.sendMessage(labelValue("Damage Reduction",
                String.format("%.0f%%", config.getDamageReduction() * 100)));

        // Knockback reduction
        double hKbReduction = (1.0 - config.getKnockbackHorizontalMultiplier()) * 100;
        double vKbReduction = (1.0 - config.getKnockbackVerticalMultiplier()) * 100;

        player.sendMessage(labelValue("Horizontal KB Reduction",
                String.format("%.0f%%", hKbReduction)));
        player.sendMessage(labelValue("Vertical KB Reduction",
                String.format("%.0f%%", vKbReduction)));

        player.sendMessage(Component.empty());

        // Options
        player.sendMessage(Component.text("Show Messages: ", LABEL)
                .append(toggle(config.isShowDamageMessages())));
        player.sendMessage(Component.text("Show Effects: ", LABEL)
                .append(toggle(config.isShowBlockEffects())));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Use ", LABEL)
                .append(Component.text("/blockconfig help", ACCENT))
                .append(Component.text(" for all commands", LABEL)));
    }
}
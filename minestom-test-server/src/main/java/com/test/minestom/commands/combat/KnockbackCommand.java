package com.test.minestom.commands.combat;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.config.knockback.KnockbackPresets;
import com.minestom.mechanics.systems.knockback.KnockbackSystem;
import com.minestom.mechanics.systems.knockback.tags.KnockbackTagValue;
import com.test.minestom.misc.CommandHelpBuilder;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.util.Locale;

import static com.test.minestom.misc.MessageBuilder.*;

/**
 * Knockback command - Display and modify every knockback config value.
 * /kb (no args) = show all values
 * /kb set <key> <value> = set one value
 */
public class KnockbackCommand extends Command {

    private final KnockbackSystem system;

    public KnockbackCommand() {
        super("kb", "knockback");
        this.system = KnockbackSystem.getInstance();
        setupCommands();
    }

    private void setupCommands() {
        setDefaultExecutor((sender, ctx) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command.", ERROR));
                return;
            }
            showAllValues(player);
        });

        addSyntax((sender, ctx) -> showHelp(sender), ArgumentType.Literal("help"));

        addSyntax((sender, ctx) -> {
            if (!(sender instanceof Player player)) return;
            KnockbackSystem.setDebugToChat(true);
            player.sendMessage(success("Knockback velocity debug ON - prints to chat on hit"));
        }, ArgumentType.Literal("debug"), ArgumentType.Literal("on"));
        addSyntax((sender, ctx) -> {
            if (!(sender instanceof Player player)) return;
            KnockbackSystem.setDebugToChat(false);
            player.sendMessage(success("Knockback velocity debug OFF"));
        }, ArgumentType.Literal("debug"), ArgumentType.Literal("off"));

        addSyntax((sender, ctx) -> {
            if (!(sender instanceof Player player)) return;
            clearWorldConfig(player);
            player.sendMessage(success("Reset to server preset (cleared world override)"));
        }, ArgumentType.Literal("reset"));
        addSyntax((sender, ctx) -> {
            if (!(sender instanceof Player player)) return;
            applyPreset(player, "minemen");
        }, ArgumentType.Literal("preset"), ArgumentType.Literal("minemen"));
        addSyntax((sender, ctx) -> {
            if (!(sender instanceof Player player)) return;
            applyPreset(player, "hypixel");
        }, ArgumentType.Literal("preset"), ArgumentType.Literal("hypixel"));
        addSyntax((sender, ctx) -> {
            if (!(sender instanceof Player player)) return;
            applyPreset(player, "vanilla18");
        }, ArgumentType.Literal("preset"), ArgumentType.Literal("vanilla18"));

        var keyArg = ArgumentType.Word("key").setSuggestionCallback((sender, context, suggestion) -> {
            for (String k : KEYS) suggestion.addEntry(new SuggestionEntry(k));
        });
        var valueArg = ArgumentType.String("value");

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            String key = context.get(keyArg).toLowerCase(Locale.ROOT);
            String raw = context.get(valueArg);
            setValue(player, key, raw != null ? raw : "");
        }, ArgumentType.Literal("set"), keyArg, valueArg);
    }

    private static final String[] KEYS = {
            "horizontal", "vertical", "verticalLimit",
            "sprintBonusH", "sprintBonusV",
            "airMultH", "airMultV",
            "lookWeight", "sprintLookWeight",
            "modern", "knockbackSync",
            "meleeDirection", "projectileDirection",
            "degenerateFallback", "directionBlendMode", "velocityApplyMode",
            "horizontalFriction", "verticalFriction",
            "sprintBufferTicks",
            "rangeStartH", "rangeStartV", "rangeFactorH", "rangeFactorV", "rangeMaxH", "rangeMaxV",
            "sprintRangeStartH", "sprintRangeStartV", "sprintRangeFactorH", "sprintRangeFactorV",
            "sprintRangeMaxH", "sprintRangeMaxV"
    };

    private KnockbackConfig getEffectiveConfig(Player player) {
        var wrapper = player.getInstance().getTag(KnockbackSystem.CUSTOM);
        if (wrapper != null && wrapper.getCustom() != null) return wrapper.getCustom();
        return system.getConfig();
    }

    private void setWorldConfig(Player player, KnockbackConfig config) {
        player.getInstance().setTag(KnockbackSystem.CUSTOM, KnockbackTagValue.kbSet(config));
    }

    private void clearWorldConfig(Player player) {
        player.getInstance().removeTag(KnockbackSystem.CUSTOM);
    }

    private void showAllValues(Player player) {
        var c = getEffectiveConfig(player);
        var rb = c.rangeReduction();
        var rs = c.sprintRangeReduction();

        player.sendMessage(Component.empty());
        player.sendMessage(header("Knockback Config (All Values)"));
        player.sendMessage(Component.empty());

        player.sendMessage(headerSimple("Strength"));
        player.sendMessage(labelValue("  horizontal", fmt(c.horizontal())));
        player.sendMessage(labelValue("  vertical", fmt(c.vertical())));
        player.sendMessage(labelValue("  verticalLimit", fmt(c.verticalLimit())));

        player.sendMessage(Component.empty());
        player.sendMessage(headerSimple("Sprint Bonus"));
        player.sendMessage(labelValue("  sprintBonusH", fmt(c.sprintBonusHorizontal())));
        player.sendMessage(labelValue("  sprintBonusV", fmt(c.sprintBonusVertical())));

        player.sendMessage(Component.empty());
        player.sendMessage(headerSimple("Air Multipliers"));
        player.sendMessage(labelValue("  airMultH", fmt(c.airMultiplierHorizontal())));
        player.sendMessage(labelValue("  airMultV", fmt(c.airMultiplierVertical())));

        player.sendMessage(Component.empty());
        player.sendMessage(headerSimple("Direction & Look"));
        player.sendMessage(labelValue("  lookWeight", fmt(c.lookWeight())));
        player.sendMessage(labelValue("  sprintLookWeight", c.sprintLookWeight() == null ? "null" : fmt(c.sprintLookWeight())));
        player.sendMessage(labelValue("  meleeDirection", c.meleeDirection().name()));
        player.sendMessage(labelValue("  projectileDirection", c.projectileDirection().name()));
        player.sendMessage(labelValue("  degenerateFallback", c.degenerateFallback().name()));
        player.sendMessage(labelValue("  directionBlendMode", c.directionBlendMode().name()));

        player.sendMessage(Component.empty());
        player.sendMessage(headerSimple("Friction & Mode"));
        player.sendMessage(labelValue("  horizontalFriction", fmt(c.horizontalFriction())));
        player.sendMessage(labelValue("  verticalFriction", fmt(c.verticalFriction())));
        player.sendMessage(labelValue("  velocityApplyMode", c.velocityApplyMode().name()));

        player.sendMessage(Component.empty());
        player.sendMessage(headerSimple("Misc"));
        player.sendMessage(labelValue("  modern", String.valueOf(c.modern())));
        player.sendMessage(labelValue("  knockbackSync", String.valueOf(c.knockbackSyncSupported())));
        player.sendMessage(labelValue("  sprintBufferTicks", String.valueOf(c.sprintBufferTicks())));

        player.sendMessage(Component.empty());
        player.sendMessage(headerSimple("Range Reduction (non-sprint)"));
        player.sendMessage(labelValue("  rangeStartH", fmt(rb.startDistanceHorizontal())));
        player.sendMessage(labelValue("  rangeStartV", fmt(rb.startDistanceVertical())));
        player.sendMessage(labelValue("  rangeFactorH", fmt(rb.factorHorizontal())));
        player.sendMessage(labelValue("  rangeFactorV", fmt(rb.factorVertical())));
        player.sendMessage(labelValue("  rangeMaxH", rb.maxHorizontal() == Double.POSITIVE_INFINITY ? "inf" : fmt(rb.maxHorizontal())));
        player.sendMessage(labelValue("  rangeMaxV", rb.maxVertical() == Double.POSITIVE_INFINITY ? "inf" : fmt(rb.maxVertical())));

        player.sendMessage(Component.empty());
        player.sendMessage(headerSimple("Range Reduction (sprint)"));
        player.sendMessage(labelValue("  sprintRangeStartH", fmt(rs.startDistanceHorizontal())));
        player.sendMessage(labelValue("  sprintRangeStartV", fmt(rs.startDistanceVertical())));
        player.sendMessage(labelValue("  sprintRangeFactorH", fmt(rs.factorHorizontal())));
        player.sendMessage(labelValue("  sprintRangeFactorV", fmt(rs.factorVertical())));
        player.sendMessage(labelValue("  sprintRangeMaxH", rs.maxHorizontal() == Double.POSITIVE_INFINITY ? "inf" : fmt(rs.maxHorizontal())));
        player.sendMessage(labelValue("  sprintRangeMaxV", rs.maxVertical() == Double.POSITIVE_INFINITY ? "inf" : fmt(rs.maxVertical())));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Use ", LABEL).append(Component.text("/kb set <key> <value>", ACCENT))
                .append(Component.text(" to modify. ", LABEL))
                .append(Component.text("/kb help", ACCENT)).append(Component.text(" for keys.", LABEL)));
    }

    private static String fmt(double d) {
        return d == (long) d ? String.valueOf((long) d) : String.format("%.4f", d);
    }

    private void applyPreset(Player player, String name) {
        KnockbackConfig preset = switch (name) {
            case "minemen" -> KnockbackPresets.minemen();
            case "hypixel" -> KnockbackPresets.hypixel();
            case "vanilla18" -> KnockbackPresets.vanilla18();
            default -> null;
        };
        if (preset == null) {
            player.sendMessage(error("Unknown preset: " + name));
            return;
        }
        setWorldConfig(player, preset);
        player.sendMessage(success("Applied preset: " + name.toUpperCase()));
    }

    private void setValue(Player player, String key, Object value) {
        KnockbackConfig c = getEffectiveConfig(player);
        try {
            KnockbackConfig next = applySet(c, key, value);
            if (next != null) {
                setWorldConfig(player, next);
                player.sendMessage(success("Set " + key + " = " + value));
            } else {
                player.sendMessage(error("Unknown key: " + key + ". Use /kb to see all keys."));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(error(e.getMessage()));
        }
    }

    private KnockbackConfig applySet(KnockbackConfig c, String key, Object value) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "horizontal" -> c.withHorizontal(parseDouble(value));
            case "vertical" -> c.withVertical(parseDouble(value));
            case "verticallimit" -> c.withVerticalLimit(parseDouble(value));
            case "sprintbonush", "sprintbonushorizontal" -> c.withSprintBonusHorizontal(parseDouble(value));
            case "sprintbonusv", "sprintbonusvertical" -> c.withSprintBonusVertical(parseDouble(value));
            case "airmulth", "airmultiplierhorizontal" -> c.withAirMultiplierHorizontal(parseDouble(value));
            case "airmultv", "airmultipliervertical" -> c.withAirMultiplierVertical(parseDouble(value));
            case "lookweight" -> c.withLookWeight(parseDouble(value));
            case "sprintlookweight" -> c.withSprintLookWeight(parseDoubleOrNull(value));
            case "modern" -> c.withModern(parseBool(value));
            case "knockbacksync", "knockbacksynccupported" -> c.withKnockbackSyncSupported(parseBool(value));
            case "meleedirection" -> c.withMeleeDirection(KnockbackSystem.KnockbackDirectionMode.valueOf(parseWord(value)));
            case "projectiledirection" -> c.withProjectileDirection(KnockbackSystem.KnockbackDirectionMode.valueOf(parseWord(value)));
            case "degeneratefallback" -> c.withDegenerateFallback(KnockbackSystem.DegenerateFallback.valueOf(parseWord(value)));
            case "directionblendmode" -> c.withDirectionBlendMode(KnockbackSystem.DirectionBlendMode.valueOf(parseWord(value)));
            case "velocityapplymode" -> c.withVelocityApplyMode(KnockbackSystem.VelocityApplyMode.valueOf(parseWord(value)));
            case "horizontalfriction" -> c.withHorizontalFriction(parseDouble(value));
            case "verticalfriction" -> c.withVerticalFriction(parseDouble(value));
            case "sprintbufferticks" -> c.withSprintBufferTicks(parseInt(value));
            case "rangestarth" -> c.withRangeReduction(updateRange(c.rangeReduction(), 0, parseDouble(value)), c.sprintRangeReduction());
            case "rangestartv" -> c.withRangeReduction(updateRange(c.rangeReduction(), 1, parseDouble(value)), c.sprintRangeReduction());
            case "rangefactorh" -> c.withRangeReduction(updateRange(c.rangeReduction(), 2, parseDouble(value)), c.sprintRangeReduction());
            case "rangefactorv" -> c.withRangeReduction(updateRange(c.rangeReduction(), 3, parseDouble(value)), c.sprintRangeReduction());
            case "rangemaxh" -> c.withRangeReduction(updateRange(c.rangeReduction(), 4, parseDoubleOrInf(value)), c.sprintRangeReduction());
            case "rangemaxv" -> c.withRangeReduction(updateRange(c.rangeReduction(), 5, parseDoubleOrInf(value)), c.sprintRangeReduction());
            case "sprintrangestarth" -> c.withRangeReduction(c.rangeReduction(), updateRange(c.sprintRangeReduction(), 0, parseDouble(value)));
            case "sprintrangestartv" -> c.withRangeReduction(c.rangeReduction(), updateRange(c.sprintRangeReduction(), 1, parseDouble(value)));
            case "sprintrangefactorh" -> c.withRangeReduction(c.rangeReduction(), updateRange(c.sprintRangeReduction(), 2, parseDouble(value)));
            case "sprintrangefactorv" -> c.withRangeReduction(c.rangeReduction(), updateRange(c.sprintRangeReduction(), 3, parseDouble(value)));
            case "sprintrangemaxh" -> c.withRangeReduction(c.rangeReduction(), updateRange(c.sprintRangeReduction(), 4, parseDoubleOrInf(value)));
            case "sprintrangemaxv" -> c.withRangeReduction(c.rangeReduction(), updateRange(c.sprintRangeReduction(), 5, parseDoubleOrInf(value)));
            default -> null;
        };
    }

    private KnockbackSystem.RangeReductionConfig updateRange(KnockbackSystem.RangeReductionConfig r, int idx, double newVal) {
        return new KnockbackSystem.RangeReductionConfig(
                idx == 0 ? newVal : r.startDistanceHorizontal(),
                idx == 1 ? newVal : r.startDistanceVertical(),
                idx == 2 ? newVal : r.factorHorizontal(),
                idx == 3 ? newVal : r.factorVertical(),
                idx == 4 ? newVal : r.maxHorizontal(),
                idx == 5 ? newVal : r.maxVertical()
        );
    }

    private double parseDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(v).trim());
    }

    private Double parseDoubleOrNull(Object v) {
        String s = String.valueOf(v).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
        return Double.parseDouble(s);
    }

    private double parseDoubleOrInf(Object v) {
        String s = String.valueOf(v).trim();
        if ("inf".equalsIgnoreCase(s) || "infinity".equalsIgnoreCase(s)) return Double.POSITIVE_INFINITY;
        return parseDouble(v);
    }

    private int parseInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(v).trim());
    }

    private boolean parseBool(Object v) {
        if (v instanceof Boolean b) return b;
        String s = String.valueOf(v).trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s)) return true;
        if ("false".equals(s) || "0".equals(s) || "no".equals(s)) return false;
        throw new IllegalArgumentException("Invalid boolean: " + v);
    }

    private String parseWord(Object v) {
        return String.valueOf(v).trim().toUpperCase(Locale.ROOT);
    }

    private void showHelp(CommandSender sender) {
        CommandHelpBuilder.create("knockback")
                .description("Display and modify every knockback config value")
                .addUsage("/kb", "Show all knockback values")
                .addUsage("/kb set <key> <value>", "Set one value (e.g. /kb set horizontal 0.5)")
                .addUsage("/kb preset <minemen|hypixel|vanilla18>", "Apply a preset")
                .addUsage("/kb reset", "Clear world override, revert to server preset")
                .addUsage("/kb debug on|off", "Print velocity debug to chat on each knockback (attacker + victim)")
                .addNote("Keys: horizontal, vertical, verticalLimit, sprintBonusH, sprintBonusV,")
                .addNote("  airMultH, airMultV, lookWeight, sprintLookWeight,")
                .addNote("  horizontalFriction, verticalFriction, velocityApplyMode,")
                .addNote("  rangeStartH, rangeStartV, rangeFactorH, rangeFactorV, rangeMaxH, rangeMaxV,")
                .addNote("  sprintRangeStartH, sprintRangeStartV, ... (same for sprint)")
                .addNote("  modern, knockbackSync, meleeDirection, projectileDirection,")
                .addNote("  degenerateFallback, directionBlendMode (BLEND_DIRECTION|ADD_VECTORS), sprintBufferTicks")
                .addNote("Use 'inf' for rangeMax* (no cap)")
                .send(sender);
    }
}

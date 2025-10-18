package com.minestom.mechanics.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Centralized message builder for consistent chat formatting across the server.
 * Provides standardized colors, styles, and message patterns.
 */
public class MessageBuilder {

    // ===========================
    // COLOR SCHEME (Single source of truth)
    // ===========================

    public static final NamedTextColor PRIMARY = NamedTextColor.GOLD;
    public static final NamedTextColor SECONDARY = NamedTextColor.YELLOW;
    public static final NamedTextColor ACCENT = NamedTextColor.AQUA;

    public static final NamedTextColor SUCCESS = NamedTextColor.GREEN;
    public static final NamedTextColor ERROR = NamedTextColor.RED;
    public static final NamedTextColor WARNING = NamedTextColor.YELLOW;
    public static final NamedTextColor INFO = NamedTextColor.AQUA;

    public static final NamedTextColor LABEL = NamedTextColor.GRAY;
    public static final NamedTextColor VALUE = NamedTextColor.YELLOW;
    public static final NamedTextColor MUTED = NamedTextColor.DARK_GRAY;

    // ===========================
    // PREFIXES & SYMBOLS
    // ===========================

    public static final String PREFIX_SUCCESS = "✓";
    public static final String PREFIX_ERROR = "✗";
    public static final String PREFIX_WARNING = "⚠";
    public static final String PREFIX_INFO = "ℹ";
    public static final String PREFIX_COMBAT = "⚔";
    public static final String PREFIX_KNOCKBACK = "⚡";
    public static final String PREFIX_BLOCKING = "⛨";

    // ===========================
    // SEPARATORS
    // ===========================

    public static final String SEPARATOR_THIN = "═══════════════════════════════";
    public static final String SEPARATOR_THICK = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    public static final String SEPARATOR_DOTTED = "· · · · · · · · · · · · · · · ·";

    // ===========================
    // BUILDERS
    // ===========================

    /**
     * Create a success message with green checkmark
     */
    public static Component success(String message) {
        return Component.text(PREFIX_SUCCESS + " ", SUCCESS, TextDecoration.BOLD)
                .append(Component.text(message, SUCCESS));
    }

    /**
     * Create an error message with red X
     */
    public static Component error(String message) {
        return Component.text(PREFIX_ERROR + " ", ERROR, TextDecoration.BOLD)
                .append(Component.text(message, ERROR));
    }

    /**
     * Create a warning message
     */
    public static Component warning(String message) {
        return Component.text(PREFIX_WARNING + " ", WARNING, TextDecoration.BOLD)
                .append(Component.text(message, WARNING));
    }

    /**
     * Create an info message
     */
    public static Component info(String message) {
        return Component.text(PREFIX_INFO + " ", INFO)
                .append(Component.text(message, INFO));
    }

    /**
     * Create a header with separator lines
     */
    public static Component header(String title) {
        return Component.text(SEPARATOR_THIN, MUTED)
                .append(Component.newline())
                .append(Component.text(title, PRIMARY, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(SEPARATOR_THIN, MUTED));
    }

    /**
     * Create a simple header without separators
     */
    public static Component headerSimple(String title) {
        return Component.text(title, PRIMARY, TextDecoration.BOLD);
    }

    /**
     * Create a label-value pair (e.g., "Damage: 5.0")
     */
    public static Component labelValue(String label, String value) {
        return Component.text(label + ": ", LABEL)
                .append(Component.text(value, VALUE));
    }

    /**
     * Create a label-value pair with custom value color
     */
    public static Component labelValue(String label, String value, NamedTextColor valueColor) {
        return Component.text(label + ": ", LABEL)
                .append(Component.text(value, valueColor));
    }

    /**
     * Create a bullet point item
     */
    public static Component bullet(String text) {
        return Component.text("  • ", MUTED)
                .append(Component.text(text, LABEL));
    }

    /**
     * Create a command usage example
     */
    public static Component command(String command, String description) {
        return Component.text(command, ACCENT)
                .append(Component.text(" - ", MUTED))
                .append(Component.text(description, LABEL));
    }

    /**
     * Create a separator line
     */
    public static Component separator() {
        return Component.text(SEPARATOR_THIN, MUTED);
    }

    /**
     * Create a broadcast message (for server-wide announcements)
     */
    public static Component broadcast(String prefix, String message) {
        return Component.text(prefix + " ", PRIMARY, TextDecoration.BOLD)
                .append(Component.text(message, SECONDARY));
    }

    /**
     * Create a progress indicator (e.g., "50%")
     */
    public static Component percentage(double value) {
        return Component.text(String.format("%.0f%%", value * 100), VALUE);
    }

    /**
     * Create an enabled/disabled status
     */
    public static Component status(boolean enabled) {
        return Component.text(
                enabled ? "ENABLED" : "DISABLED",
                enabled ? SUCCESS : ERROR,
                TextDecoration.BOLD
        );
    }

    /**
     * Create an on/off status
     */
    public static Component toggle(boolean on) {
        return Component.text(
                on ? "ON" : "OFF",
                on ? SUCCESS : ERROR
        );
    }
}

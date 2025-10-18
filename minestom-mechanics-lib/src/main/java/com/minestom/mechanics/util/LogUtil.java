package com.minestom.mechanics.util;

import org.slf4j.Logger;

/**
 * Utility class for standardized logging across all systems.
 * Ensures consistent log formatting with system prefixes.
 * NOW SUPPORTS NUMBER FORMATTING!
 *
 * Usage:
 * <pre>
 * LogUtil.system("DamageSystem").info("Player took {} damage", 5.5);
 * LogUtil.system("CombatSystem").warn("Invalid reach detected");
 * LogUtil.system("ArmorSystem").info("Blocked {:.2f} damage", 7.5555); // Formats to 2 decimals
 * </pre>
 */
public final class LogUtil {

    private LogUtil() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * Get a logger wrapper for a specific system.
     *
     * @param systemName Name of the system (e.g., "DamageSystem", "KnockbackHandler")
     * @return SystemLogger instance for that system
     */
    public static SystemLogger system(String systemName) {
        return new SystemLogger(systemName);
    }

    /**
     * Logger wrapper that automatically adds system prefix to all messages.
     * ALSO handles Python-style number formatting like {:.2f}
     */
    public static class SystemLogger {
        private final Logger logger;  // Each system gets its own logger!
        private final String prefix;

        private SystemLogger(String systemName) {
            // Create a logger specific to this system
            this.logger = org.slf4j.LoggerFactory.getLogger("com.minestom.mechanics." + systemName);
            this.prefix = "[" + systemName + "] ";
        }

        /**
         * Log an info message.
         */
        public void info(String message) {
            logger.info(prefix + message);
        }

        /**
         * Log an info message with formatting.
         * Supports both {} and {:.Xf} format specifiers.
         */
        public void info(String format, Object... args) {
            logger.info(prefix + formatMessage(format, args));
        }

        /**
         * Log a warning message.
         */
        public void warn(String message) {
            logger.warn(prefix + message);
        }

        /**
         * Log a warning message with formatting.
         */
        public void warn(String format, Object... args) {
            logger.warn(prefix + formatMessage(format, args));
        }

        /**
         * Log an error message.
         */
        public void error(String message) {
            logger.error(prefix + message);
        }

        /**
         * Log an error message with exception.
         */
        public void error(String message, Throwable throwable) {
            logger.error(prefix + message, throwable);
        }

        /**
         * Log an error message with formatting.
         */
        public void error(String format, Object... args) {
            logger.error(prefix + formatMessage(format, args));
        }

        /**
         * Log a debug message (only if debug is enabled).
         */
        public void debug(String message) {
            if (logger.isDebugEnabled()) {
                logger.debug(prefix + message);
            }
        }

        /**
         * Log a debug message with formatting.
         */
        public void debug(String format, Object... args) {
            if (logger.isDebugEnabled()) {
                logger.debug(prefix + formatMessage(format, args));
            }
        }

        /**
         * Format a message with both {} and {:.Xf} format specifiers.
         *
         * Examples:
         * - "{} did {:.2f} damage" with args ["Player", 7.555] -> "Player did 7.56 damage"
         * - "Health: {:.0f}%" with args [85.7] -> "Health: 86%"
         * - "Position: ({:.1f}, {:.1f})" with args [12.34, 56.78] -> "Position: (12.3, 56.8)"
         */
        private String formatMessage(String format, Object... args) {
            if (args == null || args.length == 0) {
                return format;
            }

            StringBuilder result = new StringBuilder();
            int argIndex = 0;
            int i = 0;

            while (i < format.length()) {
                if (format.charAt(i) == '{' && i + 1 < format.length()) {
                    // Check if it's a format specifier like {:.2f}
                    if (i + 2 < format.length() && format.charAt(i + 1) == ':') {
                        // Find the closing brace
                        int closeBrace = format.indexOf('}', i);
                        if (closeBrace != -1 && argIndex < args.length) {
                            // Extract format spec (e.g., ":.2f")
                            String formatSpec = format.substring(i + 2, closeBrace);

                            // Apply formatting
                            Object arg = args[argIndex++];
                            String formatted = formatNumber(arg, formatSpec);
                            result.append(formatted);

                            i = closeBrace + 1;
                            continue;
                        }
                    }
                    // Simple {} placeholder
                    else if (format.charAt(i + 1) == '}' && argIndex < args.length) {
                        result.append(args[argIndex++]);
                        i += 2;
                        continue;
                    }
                }

                result.append(format.charAt(i));
                i++;
            }

            return result.toString();
        }

        /**
         * Format a number according to the format spec.
         *
         * Format specs:
         * - .0f = no decimal places (e.g., 7.555 -> "8")
         * - .1f = 1 decimal place (e.g., 7.555 -> "7.6")
         * - .2f = 2 decimal places (e.g., 7.555 -> "7.56")
         */
        private String formatNumber(Object value, String formatSpec) {
            if (!(value instanceof Number)) {
                return String.valueOf(value);
            }

            // Parse format spec like ".2f"
            if (formatSpec.matches("\\.\\d+f")) {
                int decimals = Integer.parseInt(formatSpec.substring(1, formatSpec.length() - 1));
                return String.format("%." + decimals + "f", ((Number) value).doubleValue());
            }

            // Fallback to default toString
            return String.valueOf(value);
        }
    }

    // ===========================
    // CONVENIENCE METHODS
    // ===========================

    /**
     * Log initialization message for a system.
     */
    public static void logInit(String systemName) {
        system(systemName).debug("System initialized");
    }

    /**
     * Log initialization message with additional info.
     */
    public static void logInit(String systemName, String details) {
        system(systemName).debug("System initialized - " + details);
    }

    /**
     * Log that a system is already initialized.
     */
    public static void logAlreadyInitialized(String systemName) {
        system(systemName).warn("System already initialized!");
    }

    /**
     * Log cleanup message for a player.
     */
    public static void logCleanup(String systemName, String playerName) {
        system(systemName).info("Cleaned up data for player: " + playerName);
    }

    /**
     * Log configuration change.
     */
    public static void logConfigChange(String systemName, String setting, String oldValue, String newValue) {
        system(systemName).info(String.format("Config changed: %s (%s → %s)",
                setting, oldValue, newValue));
    }

    /**
     * Log initialization start with simple message.
     */
    public static void logInitStart(String systemName) {
        SystemLogger logger = system(systemName);
        logger.info("Initializing...");
    }

    /**
     * Log initialization complete with simple message.
     */
    public static void logInitComplete(String systemName) {
        SystemLogger logger = system(systemName);
        logger.info("Initialized");
    }
}

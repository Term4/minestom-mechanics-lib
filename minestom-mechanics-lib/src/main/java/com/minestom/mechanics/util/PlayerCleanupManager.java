package com.minestom.mechanics.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// TODO: Should move to the aformentioned (to be created) player package
//  ALSO need to ensure that cleanup is done properly, and we aren't having
//  a bunch of methods implement their own cleanup when we should be using
//  the centralized cleanup manager. SOME systems will need their own cleanup, we just
//  need to make sure we're being efficient.

/**
 * CRITICAL MEMORY LEAK PREVENTION
 *
 * Ensures ALL player data is cleaned up when they disconnect.
 * Register all your systems here to prevent memory leaks.
 *
 * Usage in main():
 * <pre>
 * PlayerCleanupManager.initialize();
 *
 * // Register systems that need cleanup
 * PlayerCleanupManager.register(player -> {
 *     DamageFeature.getInstance().cleanup(player);
 * }, "DamageSystem");
 * </pre>
 */
public class PlayerCleanupManager {

    private static final LogUtil.SystemLogger log = LogUtil.system("CleanupManager");
    private static final List<CleanupEntry> handlers = new CopyOnWriteArrayList<>();
    private static boolean initialized = false;

    // Statistics
    private static long totalCleanups = 0;
    private static long totalFailures = 0;

    /**
     * Initialize the cleanup manager.
     * Call this ONCE at server startup, before initializing any systems.
     */
    public static void initialize() {
        if (initialized) {
            log.warn("Already initialized!");
            return;
        }

        MinecraftServer.getGlobalEventHandler()
                .addListener(PlayerDisconnectEvent.class, event -> {
                    cleanupPlayer(event.getPlayer());
                });

        initialized = true;
        log.info("Player cleanup manager initialized");
    }

    /**
     * Register a cleanup handler (simple lambda version)
     *
     * @param handler The cleanup function to run
     * @param name Descriptive name for logging
     */
    public static void register(CleanupHandler handler, String name) {
        if (!initialized) {
            log.warn("Not initialized! Call initialize() first");
        }

        handlers.add(new CleanupEntry(handler, name));
        log.info("Registered cleanup handler: {}", name);
    }

    /**
     * Clean up ALL data for a player.
     * Called automatically on disconnect.
     */
    private static void cleanupPlayer(Player player) {
        String username = player.getUsername();
        log.debug("Cleaning up player: {}", username);

        long startTime = System.nanoTime();
        int cleaned = 0;
        int failed = 0;

        for (CleanupEntry entry : handlers) {
            try {
                entry.handler.cleanup(player);
                cleaned++;
            } catch (Exception e) {
                failed++;
                totalFailures++;
                log.error("Cleanup FAILED for system '{}' (player: {}): {}",
                        entry.name, username, e.getMessage(), e);
            }
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        totalCleanups++;

        if (failed > 0) {
            log.warn("Cleaned up {} (success: {}, FAILED: {}) in {}ms",
                    username, cleaned, failed, durationMs);
        } else {
            log.debug("Cleaned up {} ({} systems) in {}ms",
                    username, cleaned, durationMs);
        }
    }

    /**
     * Get cleanup statistics
     */
    public static CleanupStats getStats() {
        return new CleanupStats(
                handlers.size(),
                totalCleanups,
                totalFailures
        );
    }

    /**
     * List all registered cleanup handlers
     */
    public static List<String> getRegisteredHandlers() {
        return handlers.stream()
                .map(entry -> entry.name)
                .toList();
    }

    // ===========================
    // INTERFACES
    // ===========================

    /**
     * Simple cleanup function interface
     */
    @FunctionalInterface
    public interface CleanupHandler {
        void cleanup(Player player) throws Exception;
    }

    /**
     * Internal entry for tracking handlers
     */
    private record CleanupEntry(CleanupHandler handler, String name) {}

    /**
     * Statistics about cleanup operations
     * 
     * @param registeredHandlers the number of registered cleanup handlers
     * @param totalCleanups the total number of cleanup operations performed
     * @param totalFailures the total number of cleanup failures
     */
    public record CleanupStats(
            int registeredHandlers,
            long totalCleanups,
            long totalFailures
    ) {
        public double failureRate() {
            return totalCleanups > 0
                    ? (double) totalFailures / totalCleanups
                    : 0.0;
        }
    }
}

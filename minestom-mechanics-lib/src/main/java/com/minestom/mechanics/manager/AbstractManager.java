package com.minestom.mechanics.manager;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all manager implementations.
 *
 * Consolidates common patterns:
 * - Initialization state management
 * - Consistent error handling
 * - Standardized logging with LogUtil helpers
 * - Automatic cleanup/shutdown via system registry
 *
 * Reduces boilerplate across all managers while maintaining
 * useful debugging capabilities.
 *
 * @param <T> The concrete manager type extending AbstractManager
 */
public abstract class AbstractManager<T extends AbstractManager<T>>
        implements Lifecycle {

    protected boolean initialized = false;
    protected final LogUtil.SystemLogger log;

    protected AbstractManager(String systemName) {
        this.log = LogUtil.system(systemName);
    }

    // ===========================
    // SYSTEM REGISTRY
    // ===========================

    /**
     * Internal record for tracking registered systems.
     */
    private record SystemEntry(Object system, String name) {}

    /**
     * Registry of all systems managed by this manager.
     */
    private final List<SystemEntry> systems = new ArrayList<>();

    /**
     * Register a system for automatic cleanup/shutdown/status.
     * Call this during initialization for each system you create.
     *
     * @param system The system instance (can be null)
     * @param name The system name for logging and status
     */
    protected void registerSystem(Object system, String name) {
        systems.add(new SystemEntry(system, name));
    }

    /**
     * Get the total number of registered systems.
     */
    protected int getSystemCount() {
        return systems.size();
    }

    /**
     * Get the number of active (non-null) systems.
     */
    protected int getActiveSystemCount() {
        return (int) systems.stream()
                .filter(entry -> entry.system != null)
                .count();
    }

    // ===========================
    // COMMON INITIALIZATION PATTERNS
    // ===========================

    /**
     * Common initialization guard - throws if not initialized
     */
    protected void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException(getSystemName() + " not initialized! Call initialize() first.");
        }
    }

    /**
     * Common initialization wrapper with consistent error handling
     */
    @SuppressWarnings("unchecked")
    protected T initializeWithWrapper(Runnable actualInit) {
        if (initialized) {
            log.warn("Already initialized! Call shutdown() first to reinitialize.");
            return (T) this;
        }

        LogUtil.logInitStart(getSystemName());

        try {
            actualInit.run();
            initialized = true;
            LogUtil.logInitComplete(getSystemName());
            logMinimalConfig();
        } catch (Exception e) {
            log.error("Initialization failed!", e);
            cleanup();
            throw new RuntimeException("Failed to initialize " + getSystemName(), e);
        }

        return (T) this;
    }

    /**
     * Common shutdown wrapper with consistent cleanup
     */
    protected void shutdownWithWrapper(Runnable actualCleanup) {
        if (!initialized) {
            log.warn("Not initialized, nothing to shutdown");
            return;
        }

        log.info("Shutting down...");
        actualCleanup.run();
        initialized = false;
        log.info("Shutdown complete");
    }

    // ===========================
    // LIFECYCLE IMPLEMENTATION
    // ===========================

    /**
     * Check if manager is initialized.
     * Overrides Lifecycle default to use actual state.
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Automatically clean up all registered systems for a player.
     * Checks for Lifecycle interface (works for both systems and managers).
     */
    @Override
    public void cleanupPlayer(Player player) {
        if (!initialized) return;

        for (SystemEntry entry : systems) {
            if (entry.system == null) continue;

            try {
                // NEW: Check for Lifecycle interface (works for both systems and managers)
                if (entry.system instanceof Lifecycle lifecycle) {
                    lifecycle.cleanupPlayer(player);
                }
            } catch (Exception e) {
                log.error("Cleanup failed for {} (player: {}): {}",
                        entry.name, player.getUsername(), e.getMessage(), e);
            }
        }
    }

    /**
     * Automatically shutdown all registered systems.
     * Uses system registry for automatic cleanup.
     */
    @Override
    public void shutdown() {
        shutdownWithWrapper(() -> {
            // Clean up all online players first
            MinecraftServer.getConnectionManager().getOnlinePlayers()
                    .forEach(this::cleanupPlayer);

            // Shutdown each system
            for (SystemEntry entry : systems) {
                if (entry.system == null) continue;

                try {
                    // NEW: Check for Lifecycle interface (works for both systems and managers)
                    if (entry.system instanceof Lifecycle lifecycle) {
                        lifecycle.shutdown();
                    }
                } catch (Exception e) {
                    log.error("Shutdown failed for {}: {}", entry.name, e.getMessage(), e);
                }
            }

            // Clear registry
            systems.clear();

            // Subclass cleanup hook
            cleanup();
        });
    }

    /**
     * Get current status using system registry.
     * Override if you need custom status reporting.
     */
    @Override
    public String getStatus() {
        if (!initialized) {
            return getSystemName() + ": NOT INITIALIZED";
        }

        StringBuilder status = new StringBuilder();
        status.append(getSystemName()).append(" Status:\n");

        for (SystemEntry entry : systems) {
            status.append("  ").append(entry.name).append(": ")
                    .append(entry.system != null ? "✓" : "✗").append("\n");
        }

        return status.toString().trim();
    }

    // ===========================
    // ABSTRACT METHODS FOR CUSTOMIZATION
    // ===========================

    /**
     * Get the system name for logging and error messages
     */
    protected abstract String getSystemName();

    /**
     * Log minimal configuration info (not verbose).
     * Called after successful initialization.
     * Tip: Use getActiveSystemCount() to report how many systems were initialized.
     */
    protected abstract void logMinimalConfig();

    /**
     * Cleanup resources when initialization fails or during shutdown.
     * Override if you have additional cleanup beyond registered systems.
     * Default implementation is no-op since registry handles most cleanup.
     */
    protected void cleanup() {
        // Default: no-op, override if needed
    }
}
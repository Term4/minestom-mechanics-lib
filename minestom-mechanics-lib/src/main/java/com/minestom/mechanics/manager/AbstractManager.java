package com.minestom.mechanics.manager;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.Player;

// TODO: I actually like this a lot. Notice how a lot of the
//  logging is handled HERE in the abstract system? That means
//  that all managers can be guaranteed to have consistent logging.
//  ISSUE is we still added additional verbose logging to each manager anyways,
//  making each manager class much longer than it needs to be

/**
 * Abstract base class for all manager implementations.
 * 
 * Consolidates common patterns:
 * - Initialization state management
 * - Consistent error handling
 * - Standardized logging with LogUtil helpers
 * - Common cleanup patterns
 * 
 * Reduces boilerplate across all managers while maintaining
 * useful debugging capabilities.
 * 
 * @param <T> The concrete manager type extending AbstractManager
 */
public abstract class AbstractManager<T extends AbstractManager<T>> {
    protected boolean initialized = false;
    protected final LogUtil.SystemLogger log;
    
    protected AbstractManager(String systemName) {
        this.log = LogUtil.system(systemName);
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
            logMinimalConfig(); // Optional minimal logging
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
    // ABSTRACT METHODS FOR CUSTOMIZATION
    // ===========================
    
    /**
     * Get the system name for logging and error messages
     */
    protected abstract String getSystemName();
    
    /**
     * Log minimal configuration info (not verbose)
     */
    protected abstract void logMinimalConfig();
    
    /**
     * Cleanup resources when initialization fails
     */
    protected abstract void cleanup();
    
    /**
     * Get current status for debugging (keep useful)
     */
    public abstract String getStatus();
    
    // ===========================
    // COMMON UTILITY METHODS
    // ===========================
    
    /**
     * Check if manager is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Optional per-player cleanup (default: no-op)
     * Override in subclasses if needed
     */
    public void cleanupPlayer(Player player) {
        // Default: no-op, override if needed
    }
}

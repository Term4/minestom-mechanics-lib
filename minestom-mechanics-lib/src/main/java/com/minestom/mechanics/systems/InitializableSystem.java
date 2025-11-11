package com.minestom.mechanics.systems;

import com.minestom.mechanics.manager.Lifecycle;

/**
 * Base class for systems that require explicit initialization.
 * Provides common initialization state management and validation.
 * Includes helper methods for singleton pattern implementation.
 *
 * Now implements Lifecycle to provide cleanup/shutdown capabilities.
 */
public abstract class InitializableSystem implements Lifecycle {

    /**
     * Tracks whether this system has been initialized.
     * Subclasses should set this via markInitialized() after setup is complete.
     */
    protected boolean initialized = false;

    /**
     * Mark this system as initialized.
     * Should be called by subclasses after their initialization logic completes.
     */
    protected void markInitialized() {
        this.initialized = true;
    }

    /**
     * Validate that this system has been initialized.
     * Throws IllegalStateException if not initialized.
     *
     * Call this at the start of methods that require initialization.
     *
     * @throws IllegalStateException if system is not initialized
     */
    protected void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + " not initialized! Call initialize() first."
            );
        }
    }

    /**
     * Check if this system has been initialized.
     * Overrides the Lifecycle default to use actual state.
     *
     * @return true if initialized, false otherwise
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Reset initialization state.
     * Useful for testing or reinitializing systems.
     */
    protected void resetInitialization() {
        this.initialized = false;
    }

    /**
     * Helper method for singleton getInstance() implementations.
     * Validates that the instance is not null and throws a consistent exception.
     *
     * @param instance The singleton instance to validate
     * @param className The class name for error messages
     * @param <T> The type of the instance
     * @return The validated instance
     * @throws IllegalStateException if instance is null
     */
    protected static <T> T requireInstance(T instance, String className) {
        if (instance == null) {
            throw new IllegalStateException(className + " not initialized!");
        }
        return instance;
    }

    // cleanupPlayer() and shutdown() inherited from Lifecycle with default no-op
    // Override in subclasses if cleanup/shutdown is needed
}
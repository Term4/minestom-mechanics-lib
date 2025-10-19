package com.minestom.mechanics.util;

// TODO: This is actually pretty good. Could update with some more features maybe?
//  But overall this is pretty good.

/**
 * Base class for systems that require explicit initialization.
 * Provides common initialization state management and validation.
 * Includes helper methods for singleton pattern implementation.
 *
 * Usage:
 * <pre>
 * public class MySystem extends InitializableSystem {
 *     private static MySystem instance;
 *
 *     public static MySystem initialize() {
 *         if (instance != null &amp;&amp; instance.isInitialized()) {
 *             LogUtil.logAlreadyInitialized("MySystem");
 *             return instance;
 *         }
 *
 *         instance = new MySystem();
 *         instance.setup();
 *         instance.markInitialized(); // Call this when setup is complete
 *         return instance;
 *     }
 *
 *     public static MySystem getInstance() {
 *         return requireInstance(instance, "MySystem");
 *     }
 *
 *     public void someMethod() {
 *         requireInitialized(); // Validates before executing
 *         // ... method implementation
 *     }
 * }
 * </pre>
 */
public abstract class InitializableSystem {

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
     *
     * @return true if initialized, false otherwise
     */
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
}

package com.minestom.mechanics.manager;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.features.knockback.KnockbackHandler;

/**
 * @deprecated KnockbackManager is redundant. Use {@link KnockbackHandler} directly.
 * This class will be removed in v2.0.
 *
 * Migration:
 * <pre>
 * // Old
 * KnockbackManager.getInstance().initialize(config);
 *
 * // New
 * KnockbackHandler.initialize(config);
 * </pre>
 */
@Deprecated
public class KnockbackManager extends AbstractManager<KnockbackManager> {
    private static KnockbackManager instance;
    private KnockbackHandler knockbackHandler;

    private KnockbackManager() {
        super("KnockbackManager");
    }

    public static KnockbackManager getInstance() {
        if (instance == null) {
            instance = new KnockbackManager();
        }
        return instance;
    }

    public KnockbackManager initialize(KnockbackConfig config, boolean enableSync) {
        return initializeWithWrapper(() -> {
            log.warn("KnockbackManager is deprecated. Use KnockbackHandler directly.");
            knockbackHandler = KnockbackHandler.initialize(config);
            knockbackHandler.setKnockbackSyncEnabled(enableSync);
        });
    }

    @Override
    protected String getSystemName() {
        return "KnockbackManager";
    }

    @Override
    protected void logMinimalConfig() {
        // No-op
    }

    @Override
    protected void cleanup() {
        if (knockbackHandler != null) {
            knockbackHandler.shutdown();
        }
    }

    @Override
    public void cleanupPlayer(net.minestom.server.entity.Player player) {
        if (knockbackHandler != null) {
            knockbackHandler.cleanup(player);
        }
    }

    @Override
    public String getStatus() {
        return "KnockbackManager (deprecated - use KnockbackHandler)";
    }

    /**
     * Get the underlying handler. Use this directly instead of the manager.
     */
    public KnockbackHandler getKnockbackHandler() {
        requireInitialized();
        return knockbackHandler;
    }
}
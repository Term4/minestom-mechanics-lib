package com.minestom.mechanics.systems.blocking;

import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.config.blocking.BlockingPreferences;
import com.minestom.mechanics.systems.compatibility.legacy_1_8.fix.LegacyAnimationFix;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages blocking state for players - tracks who's blocking and handles state transitions.
 * Focused responsibility: Core blocking state management only.
 */
public class BlockingState {
    private static final LogUtil.SystemLogger log = LogUtil.system("BlockingStateManager");

    // Core tags
    public static final Tag<Boolean> BLOCKING = Tag.Boolean("blocking").defaultValue(false);
    public static final Tag<BlockingPreferences> PREFERENCES = Tag.Transient("blocking_prefs");

    // TODO: do we need to keep a map of all the blocking players if we have them tagged as blocking?
    //  I think we do just making sure though

    // State tracking
    private final Map<UUID, Boolean> blockingPlayers = new ConcurrentHashMap<>();
    private final CombatConfig config;

    public BlockingState(CombatConfig config) {
        this.config = config;
    }

    /**
     * Check if a player is currently blocking
     */
    public boolean isBlocking(Player player) {
        return Boolean.TRUE.equals(blockingPlayers.get(player.getUuid()));
    }

    /**
     * Start blocking for a player
     */
    public void startBlocking(Player player) {
        if (!config.blockingEnabled()) return;
        if (isBlocking(player)) return;

        UUID uuid = player.getUuid();
        player.setTag(BLOCKING, true);
        blockingPlayers.put(uuid, true);

        // Register animation
        LegacyAnimationFix.getInstance().registerAnimation(
                player,
                "blocking",
                this::isBlocking
        );

        log.debug("{} started blocking", player.getUsername());
    }

    /**
     * Stop blocking for a player
     */
    public void stopBlocking(Player player) {
        if (!isBlocking(player)) return;

        LegacyAnimationFix.getInstance().unregisterAnimation(player);

        UUID uuid = player.getUuid();
        player.setTag(BLOCKING, false);
        blockingPlayers.remove(uuid);

        log.debug("{} stopped blocking", player.getUsername());
    }

    /**
     * Force stop blocking for a player (cleanup)
     */
    public void forceStopBlocking(Player player) {
        if (isBlocking(player)) {
            stopBlocking(player);
        }
    }

    /**
     * Get the number of currently blocking players
     */
    public int getActiveCount() {
        return blockingPlayers.size();
    }

    /**
     * Clean up all blocking data for a player
     */
    public void cleanup(Player player) {
        UUID uuid = player.getUuid();
        
        forceStopBlocking(player);
        blockingPlayers.remove(uuid);
        
        player.removeTag(BLOCKING);
        player.removeTag(PREFERENCES);

        log.debug("Cleaned up blocking state for: {}", player.getUsername());
    }

    /**
     * Get all currently blocking players
     */
    public Map<UUID, Boolean> getBlockingPlayers() {
        return blockingPlayers;
    }
}

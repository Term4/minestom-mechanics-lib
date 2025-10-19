package com.minestom.mechanics.features.knockback.components;

import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.minestom.mechanics.config.combat.CombatConstants.*;

// TODO: See if this is even necessary. Seems overly complex, also shouldn't
//  knockback just be applied if a hit went through to the damage system? what's all this "canReceiveKnockback" for?
//  ALSO any of this ground state tracking, A. it'd be a lot better to probably have a general method for that, outside
//  of this class, and B. seems like it'd be used more for KnockbackSync than anything. I mean we don't get access to friction, sooo

/**
 * Component responsible for managing knockback state and player data.
 * Extracted from monolithic KnockbackHandler for better maintainability.
 */
public class KnockbackStateManager {
    
    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackStateManager");
    
    // ✅ REFACTORED: Using constants
    private static final Tag<Long> LAST_GROUND_TIME = Tag.Long("last_ground_time");
    private static final Tag<Boolean> KB_GROUNDED = Tag.Boolean("kb_grounded").defaultValue(true);
    
    // Player data tracking
    private final Map<UUID, KnockbackHandler.PlayerKnockbackData> playerDataMap = new ConcurrentHashMap<>();
    private final Set<UUID> knockbackThisTick = ConcurrentHashMap.newKeySet();
    
    /**
     * Track ground state for knockback calculations.
     * ✅ REFACTORED: Using GROUND_STATE_DELAY_MS constant
     */
    public void trackGroundState(Player player) {
        long now = System.currentTimeMillis();

        if (player.isOnGround()) {
            player.setTag(LAST_GROUND_TIME, now);
            player.setTag(KB_GROUNDED, true);
        } else {
            Long lastGround = player.getTag(LAST_GROUND_TIME);
            // ✅ REFACTORED: Using constant for delay
            if (lastGround != null && (now - lastGround) > GROUND_STATE_DELAY_MS) {
                player.setTag(KB_GROUNDED, false);
            }
        }
    }
    
    /**
     * Update player combat state.
     */
    public void updateCombatState(Player player) {
        KnockbackHandler.PlayerKnockbackData data = getOrCreatePlayerData(player);
        data.lastCombatTime = System.currentTimeMillis();
    }
    
    /**
     * Get or create player data.
     */
    public KnockbackHandler.PlayerKnockbackData getOrCreatePlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUuid(), KnockbackHandler.PlayerKnockbackData::new);
    }
    
    /**
     * Get player data.
     */
    public KnockbackHandler.PlayerKnockbackData getPlayerData(Player player) {
        return playerDataMap.get(player.getUuid());
    }
    
    /**
     * Remove player data.
     */
    public void removePlayerData(Player player) {
        UUID uuid = player.getUuid();

        // Remove from maps
        KnockbackHandler.PlayerKnockbackData data = playerDataMap.remove(uuid);
        if (data != null) {
            // Clear any references inside the data
            //  data.lastKnockback = Vec.ZERO;
        }

        knockbackThisTick.remove(uuid);

        // Remove all tags
        player.removeTag(LAST_GROUND_TIME);
        player.removeTag(KB_GROUNDED);

        log.debug("Cleaned up knockback data for: {}", player.getUsername());
    }
    
    /**
     * Get tracked players count.
     */
    public int getTrackedPlayers() {
        return playerDataMap.size();
    }
    
    /**
     * Cleanup all data.
     */
    public void shutdown() {
        playerDataMap.clear();
        knockbackThisTick.clear();
    }
}

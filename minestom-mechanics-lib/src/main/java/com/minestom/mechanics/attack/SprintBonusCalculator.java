package com.minestom.mechanics.attack;

import com.minestom.mechanics.config.combat.CombatRulesConfig;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.minestom.mechanics.config.combat.CombatConstants.*;

// TODO: I like this, also how it's configurable as well.
//  Make sure it's thread and memory safe, and not to
//  performance intensive.

/**
 * Calculates sprint bonus for attacks.
 * Single responsibility: Only sprint bonus logic.
 */
public class SprintBonusCalculator {
    private static final LogUtil.SystemLogger log = LogUtil.system("SprintBonusCalculator");
    
    private final CombatRulesConfig config;
    private final Map<UUID, SprintStateBuffer> sprintBuffers = new ConcurrentHashMap<>();
    private Task sprintTrackingTask;
    
    public SprintBonusCalculator(CombatRulesConfig config) {
        this.config = config;
    }
    
    /**
     * Start tracking sprint states for all players.
     */
    public void startSprintTracking() {
        sprintTrackingTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                        SprintStateBuffer buffer = sprintBuffers.computeIfAbsent(
                                player.getUuid(),
                                uuid -> new SprintStateBuffer()
                        );
                        buffer.recordState(player.isSprinting());
                    }
                })
                .repeat(TaskSchedule.tick(1))
                .schedule();
    }
    
    /**
     * Check if player was recently sprinting (for sprint bonus).
     */
    public boolean wasRecentlySprinting(Player player) {
        SprintStateBuffer buffer = sprintBuffers.get(player.getUuid());
        if (buffer == null) return player.isSprinting();
        
        int windowTicks = calculateSprintWindowTicks(player);
        return buffer.wasSprintingInLastTicks(windowTicks);
    }
    
    /**
     * Calculate sprint window based on player ping and configuration.
     */
    private int calculateSprintWindowTicks(Player player) {
        int windowTicks;
        
        if (config.isDynamicSprintWindow()) {
            // Dynamic: scales with player's ping (one-way latency)
            long rttMs = player.getLatency();
            long oneWayLatency = rttMs / 2;
            
            windowTicks = (int) Math.ceil(
                    (double) (oneWayLatency * ServerFlag.SERVER_TICKS_PER_SECOND) / 1000
            );
            
            if (config.isSprintWindowDouble()) {
                windowTicks *= 2;
            }
        } else {
            // Fixed: use configured value
            windowTicks = config.getSprintWindowMaxTicks();
        }
        
        // Enforce limits
        windowTicks = Math.min(windowTicks, config.getSprintWindowMaxTicks());
        windowTicks = Math.max(windowTicks, 1);
        
        return windowTicks;
    }
    
    /**
     * Clean up player data.
     */
    public void cleanup(Player player) {
        sprintBuffers.remove(player.getUuid());
    }
    
    /**
     * Shutdown sprint tracking.
     */
    public void shutdown() {
        if (sprintTrackingTask != null) {
            sprintTrackingTask.cancel();
            sprintTrackingTask = null;
        }
        sprintBuffers.clear();
        log.debug("SprintBonusCalculator shutdown complete");
    }
    
    /**
     * Thread-safe ring buffer tracking recent sprint states.
     * Compensates for packet ordering issues by checking last N ticks.
     */
    private static class SprintStateBuffer {
        private static final int BUFFER_SIZE = SPRINTWINDOW_TRACKING_TICKS;
        private final boolean[] states = new boolean[BUFFER_SIZE];
        private volatile int index = 0;
        
        public synchronized void recordState(boolean sprinting) {
            states[index] = sprinting;
            index = (index + 1) % BUFFER_SIZE;
        }
        
        public synchronized boolean wasSprintingInLastTicks(int ticks) {
            ticks = Math.min(ticks, BUFFER_SIZE);
            for (int i = 0; i < ticks; i++) {
                int checkIndex = (index - 1 - i + BUFFER_SIZE) % BUFFER_SIZE;
                if (states[checkIndex]) {
                    return true;
                }
            }
            return false;
        }
    }
}

package com.minestom.mechanics.manager;

import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.systems.validation.hits.HitDetection;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

// TODO: This one isn't actually terrible, but still a bit long

/**
 * HitboxManager - Manages all hitbox and bounding box related functionality
 * 
 * Consolidates:
 * - HitDetectionFeature (hit validation and detection)
 * - Hit validation components and utilities
 * - Hitbox expansion calculations
 * 
 * Can be used independently of combat for general hitbox needs.
 */
public class HitboxManager extends AbstractManager<HitboxManager> {
    private static HitboxManager instance;
    
    // System references
    private HitDetection hitDetection;
    
    // Current configuration
    private HitDetectionConfig hitDetectionConfig;
    
    private HitboxManager() {
        super("HitboxManager");
    }
    
    public static HitboxManager getInstance() {
        if (instance == null) {
            instance = new HitboxManager();
        }
        return instance;
    }
    
    // ===========================
    // INITIALIZATION
    // ===========================

    /**
     * Initialize with specific configuration.
     *
     * @param hitDetectionConfig configuration for hit detection/validation
     * @return this manager for chaining
     */
    public HitboxManager initialize(HitDetectionConfig hitDetectionConfig) {
        this.hitDetectionConfig = hitDetectionConfig;

        return initializeWithWrapper(() -> {
            log.debug("Initializing HitDetectionFeature...");
            hitDetection = HitDetection.initialize(hitDetectionConfig);
        });
    }

    /**
     * Initialize with default standard configuration.
     *
     * @return this manager for chaining
     */
    public HitboxManager initialize() {
        return initialize(HitDetectionConfig.standard());
    }
    
    // ===========================
    // ABSTRACT METHOD IMPLEMENTATIONS
    // ===========================

    @Override
    protected String getSystemName() {
        return "HitboxManager";
    }

    @Override
    protected void logMinimalConfig() {
        log.debug("Server-side Reach: {} blocks", hitDetectionConfig.serverSideReach());
        log.debug("Attack Packet Reach: {} blocks", hitDetectionConfig.attackPacketReach());
        if (hitDetectionConfig.enableAngleValidation()) {
            log.debug("Angle Validation: enabled ({}°)", hitDetectionConfig.angleThreshold());
        } else {
            log.debug("Angle Validation: disabled");
        }
        log.debug("Snapshot Tracking: {}", hitDetectionConfig.trackHitSnapshots() ? "enabled" : "disabled");
    }

    @Override
    protected void cleanup() {
        hitDetection = null;
    }

    @Override
    public String getStatus() {
        return initialized ? "HitboxManager: ✓" : "HitboxManager: NOT INITIALIZED";
    }
    
    @Override
    public void cleanupPlayer(Player player) {
        if (!initialized) return;
        
        if (hitDetection != null) {
            hitDetection.cleanup(player);
        }
        // HitboxSystem handles its own cleanup automatically
    }
    
    // ===========================
    // RUNTIME CONFIGURATION
    // ===========================

    /**
     * Get the current hit detection configuration.
     *
     * @return the current hit detection config
     */
    public HitDetectionConfig getConfig() {
        requireInitialized();
        return hitDetectionConfig;
    }

    /**
     * Update the hit detection configuration at runtime.
     *
     * @param newConfig the new hit detection configuration
     */
    public void updateConfig(HitDetectionConfig newConfig) {
        requireInitialized();
        this.hitDetectionConfig = newConfig;
        hitDetection.updateConfig(newConfig);
        log.info("Hit detection config updated");
    }
    
    /**
     * Shutdown all systems (for server stop or reinitialize)
     */
    public void shutdown() {
        shutdownWithWrapper(() -> {
            // Cleanup all players first
            MinecraftServer.getConnectionManager().getOnlinePlayers()
                    .forEach(this::cleanupPlayer);
            
            // Shutdown each system
            if (hitDetection != null) {
                try {
                    hitDetection.shutdown();
                } catch (Exception e) {
                    log.error("HitDetectionFeature shutdown failed", e);
                }
            }
            
            // Reset references
            hitDetection = null;
        });
    }
    
    // ===========================
    // SYSTEM ACCESS (for advanced use)
    // ===========================

    /**
     * Get the hit detection feature for advanced use cases.
     *
     * <p>Provides access to hit validation, server-side raycasting,
     * and hit analytics. Most users won't need this - use {@link #getConfig()}
     * and {@link #updateConfig(HitDetectionConfig)} for configuration instead.</p>
     *
     * @return the hit detection feature
     */
    public HitDetection getHitDetectionFeature() {
        requireInitialized();
        return hitDetection;
    }
}

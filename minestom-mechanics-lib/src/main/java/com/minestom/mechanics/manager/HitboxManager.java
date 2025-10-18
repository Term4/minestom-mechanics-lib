package com.minestom.mechanics.manager;

import com.minestom.mechanics.config.combat.HitDetectionConfig;
import com.minestom.mechanics.validation.HitDetectionFeature;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

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
    private HitDetectionFeature hitDetectionFeature;
    
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
     * Initialize with default configurations
     */
    public HitboxManager initialize() {
        return initialize(HitDetectionConfig.defaultConfig());
    }
    
    /**
     * Initialize with specific configuration
     * 
     * @param hitDetectionConfig Configuration for hit detection/validation
     * @return this manager for chaining
     */
    public HitboxManager initialize(HitDetectionConfig hitDetectionConfig) {
        this.hitDetectionConfig = hitDetectionConfig;
        
        return initializeWithWrapper(() -> {
            // Initialize HitDetectionFeature
            log.debug("Initializing HitDetectionFeature...");
            hitDetectionFeature = HitDetectionFeature.initialize(hitDetectionConfig);
        });
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
        log.debug("Server-side Reach: {} blocks", hitDetectionConfig.getServerSideReach());
        log.debug("Attack Packet Reach: {} blocks", hitDetectionConfig.getAttackPacketReach());
        if (hitDetectionConfig.isAngleValidationEnabled()) {
            log.debug("Angle Validation: enabled ({}°)", hitDetectionConfig.getAngleThreshold());
        } else {
            log.debug("Angle Validation: disabled");
        }
        log.debug("Snapshot Tracking: {}", hitDetectionConfig.shouldTrackHitSnapshots() ? "enabled" : "disabled");
    }

    @Override
    protected void cleanup() {
        // Clear all player data
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(this::cleanupPlayer);
        
        // Reset references
        hitDetectionFeature = null;
    }

    @Override
    public String getStatus() {
        if (!initialized) {
            return "Hitbox Systems: NOT INITIALIZED";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("Hitbox System Status:\n");
        status.append("  HitDetectionFeature: ").append(hitDetectionFeature != null ? "✓" : "✗").append("\n");
        
        return status.toString();
    }
    
    @Override
    public void cleanupPlayer(Player player) {
        if (!initialized) return;
        
        if (hitDetectionFeature != null) {
            hitDetectionFeature.cleanup(player);
        }
        // HitboxSystem handles its own cleanup automatically
    }
    
    // ===========================
    // RUNTIME CONFIGURATION
    // ===========================
    
    /**
     * Update hit detection reach values at runtime
     */
    public void updateReachValues(double serverSideReach, double attackPacketReach) {
        requireInitialized();
        hitDetectionConfig = new HitDetectionConfig(
            serverSideReach,
            attackPacketReach,
            hitDetectionConfig.getAngleThreshold(),
            hitDetectionConfig.shouldTrackHitSnapshots()
        );
        hitDetectionFeature.updateConfig(hitDetectionConfig);
        log.info("Updated reach values - Server: {}, Packet: {}", serverSideReach, attackPacketReach);
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
            if (hitDetectionFeature != null) {
                try {
                    hitDetectionFeature.shutdown();
                } catch (Exception e) {
                    log.error("HitDetectionFeature shutdown failed", e);
                }
            }
            
            // Reset references
            hitDetectionFeature = null;
        });
    }
    
    // ===========================
    // SYSTEM ACCESS (for advanced use)
    // ===========================
    
    public HitDetectionFeature getHitDetectionFeature() {
        requireInitialized();
        return hitDetectionFeature;
    }
}

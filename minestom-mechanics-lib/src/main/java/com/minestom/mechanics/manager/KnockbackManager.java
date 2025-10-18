package com.minestom.mechanics.manager;

import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.features.knockback.KnockbackProfile;
import com.minestom.mechanics.features.knockback.sync.KnockbackSyncHandler;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

// TODO: Seems to be a lot of duplicate methods here from the knockback feature package

/**
 * KnockbackManager - Manages knockback mechanics
 * 
 * Features:
 * - Knockback calculations and application
 * - Multiple knockback profiles (VANILLA, MINEMEN, etc.)
 * - Knockback synchronization support
 * - Sprint and critical hit modifiers
 * 
 * Can be used independently of combat for any knockback needs
 * (e.g., explosions, custom mechanics).
 */
public class KnockbackManager extends AbstractManager<KnockbackManager> {
    private static KnockbackManager instance;
    
    // System references
    private KnockbackHandler knockbackHandler;
    
    // Configuration
    private KnockbackProfile currentProfile;
    private boolean knockbackSyncEnabled = false;
    
    private KnockbackManager() {
        super("KnockbackManager");
    }
    
    public static KnockbackManager getInstance() {
        if (instance == null) {
            instance = new KnockbackManager();
        }
        return instance;
    }
    
    // ===========================
    // INITIALIZATION
    // ===========================
    
    /**
     * Initialize with default VANILLA knockback profile
     */
    public KnockbackManager initialize() {
        return initialize(KnockbackProfile.VANILLA);
    }
    
    /**
     * Initialize with a specific knockback profile
     * 
     * @param profile The knockback profile to use
     * @return this manager for chaining
     */
    public KnockbackManager initialize(KnockbackProfile profile) {
        return initialize(profile, false);
    }
    
    /**
     * Initialize with a specific knockback profile and sync settings
     * 
     * @param profile The knockback profile to use
     * @param enableSync Whether to enable knockback synchronization
     * @return this manager for chaining
     */
    public KnockbackManager initialize(KnockbackProfile profile, boolean enableSync) {
        this.currentProfile = profile;
        this.knockbackSyncEnabled = enableSync;
        
        return initializeWithWrapper(() -> {
            log.debug("Initializing KnockbackHandler...");
            knockbackHandler = KnockbackHandler.initialize(profile);
            knockbackHandler.setKnockbackSyncEnabled(enableSync);
        });
    }
    
    // ===========================
    // ABSTRACT METHOD IMPLEMENTATIONS
    // ===========================

    @Override
    protected String getSystemName() {
        return "KnockbackManager";
    }

    @Override
    protected void logMinimalConfig() {
        log.debug("Knockback Profile: {}", currentProfile.name());
        log.debug("Base Horizontal: {}", currentProfile.getHorizontal());
        log.debug("Base Vertical: {}", currentProfile.getVertical());
        log.debug("Sprint Multiplier: {}x", currentProfile.getSprintMultiplier());
        log.debug("Y-Limit: {}", currentProfile.getyLimit());
        log.debug("Friction: {}", currentProfile.getFriction());
        log.debug("Knockback Sync: {}", knockbackSyncEnabled ? "enabled" : "disabled");
    }

    @Override
    protected void cleanup() {
        // Clear all player data
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(this::cleanupPlayer);
        
        // Reset references
        knockbackHandler = null;
    }

    @Override
    public String getStatus() {
        if (!initialized) {
            return "Knockback System: NOT INITIALIZED";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("Knockback System Status:\n");
        status.append("  KnockbackHandler: ").append(knockbackHandler != null ? "✓" : "✗").append("\n");
        status.append("  Profile: ").append(currentProfile.name()).append("\n");
        status.append("  Sync Enabled: ").append(knockbackSyncEnabled ? "✓" : "✗").append("\n");
        
        return status.toString();
    }
    
    @Override
    public void cleanupPlayer(Player player) {
        if (!initialized) return;
        
        if (knockbackHandler != null) {
            knockbackHandler.removePlayerData(player);
        }
    }
    
    // ===========================
    // RUNTIME CONFIGURATION
    // ===========================
    
    /**
     * Change knockback profile at runtime
     */
    public void setKnockbackProfile(KnockbackProfile profile) {
        requireInitialized();
        this.currentProfile = profile;
        knockbackHandler.setKnockbackProfile(profile);
        log.info("Knockback profile changed to: {}", profile.name());
    }
    
    /**
     * Enable or disable knockback synchronization at runtime
     */
    public void setKnockbackSyncEnabled(boolean enabled) {
        requireInitialized();
        this.knockbackSyncEnabled = enabled;
        knockbackHandler.setKnockbackSyncEnabled(enabled);
        log.info("Knockback sync {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Get the current knockback profile
     */
    public KnockbackProfile getCurrentProfile() {
        return currentProfile;
    }
    
    /**
     * Check if knockback sync is enabled
     */
    public boolean isKnockbackSyncEnabled() {
        return knockbackSyncEnabled;
    }
    
    // ===========================
    // KNOCKBACK OPERATIONS
    // ===========================
    
    /**
     * Apply knockback to a player
     * 
     * @param victim The player receiving knockback
     * @param attacker The entity causing knockback (can be null for non-entity sources)
     * @param sprintBonus Additional knockback from sprinting
     * @param kbEnchantLevel Knockback enchantment level
     */
    public void applyKnockback(Player victim, net.minestom.server.entity.Entity attacker, 
                              boolean sprintBonus, int kbEnchantLevel) {
        requireInitialized();
        knockbackHandler.applyKnockback(victim, attacker, sprintBonus, kbEnchantLevel);
    }
    
    /**
     * Apply custom knockback with specific velocity
     * 
     * @param victim The player receiving knockback
     * @param velocityX X component of knockback velocity
     * @param velocityY Y component of knockback velocity
     * @param velocityZ Z component of knockback velocity
     */
    public void applyCustomKnockback(Player victim, double velocityX, double velocityY, double velocityZ) {
        requireInitialized();
        victim.setVelocity(victim.getVelocity().add(velocityX, velocityY, velocityZ));
        
        // Handle sync if enabled
        if (knockbackSyncEnabled) {
            knockbackHandler.syncKnockback(victim);
        }
    }
    
    /**
     * Shutdown all systems (for server stop or reinitialize)
     */
    public void shutdown() {
        shutdownWithWrapper(() -> {
            // Cleanup all players first
            MinecraftServer.getConnectionManager().getOnlinePlayers()
                    .forEach(this::cleanupPlayer);
            
            // Shutdown knockback handler
            if (knockbackHandler != null) {
                try {
                    knockbackHandler.shutdown();
                } catch (Exception e) {
                    log.error("KnockbackHandler shutdown failed", e);
                }
            }
            
            // Shutdown sync handler
            try {
                KnockbackSyncHandler.getInstance().shutdown();
            } catch (Exception e) {
                log.error("KnockbackSyncHandler shutdown failed", e);
            }
            
            // Reset references
            knockbackHandler = null;
        });
    }
    
    // ===========================
    // SYSTEM ACCESS (for advanced use)
    // ===========================
    
    public KnockbackHandler getKnockbackHandler() {
        requireInitialized();
        return knockbackHandler;
    }
}

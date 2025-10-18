package com.minestom.mechanics.manager;

import com.minestom.mechanics.systems.ArmorSystem;
import net.minestom.server.entity.Player;

/**
 * ArmorManager - Manages armor calculations and damage reduction
 * 
 * Features:
 * - Armor damage reduction calculations
 * - Armor toughness calculations
 * - Damage type filtering (which damage types bypass armor)
 * 
 * Can be used independently for any armor-related calculations.
 */
public class ArmorManager extends AbstractManager<ArmorManager> {
    private static ArmorManager instance;
    
    // System references
    private ArmorSystem armorSystem;
    
    // Configuration
    private boolean enabled = true;
    
    private ArmorManager() {
        super("ArmorManager");
    }
    
    public static ArmorManager getInstance() {
        if (instance == null) {
            instance = new ArmorManager();
        }
        return instance;
    }
    
    // ===========================
    // INITIALIZATION
    // ===========================
    
    /**
     * Initialize with default configuration (armor enabled)
     */
    public ArmorManager initialize() {
        return initialize(true);
    }
    
    /**
     * Initialize with specific configuration
     * 
     * @param enabled Whether armor damage reduction should be enabled
     * @return this manager for chaining
     */
    public ArmorManager initialize(boolean enabled) {
        this.enabled = enabled;
        
        return initializeWithWrapper(() -> {
            if (enabled) {
                log.debug("Initializing ArmorSystem...");
                armorSystem = ArmorSystem.initialize();
            } else {
                log.debug("ArmorSystem disabled");
            }
        });
    }
    
    // ===========================
    // ABSTRACT METHOD IMPLEMENTATIONS
    // ===========================
    
    @Override
    protected String getSystemName() {
        return "ArmorManager";
    }
    
    @Override
    protected void logMinimalConfig() {
        log.debug("Armor damage reduction: {}", enabled ? "enabled" : "disabled");
    }
    
    @Override
    protected void cleanup() {
        armorSystem = null;
    }
    
    @Override
    public String getStatus() {
        if (!initialized) {
            return "Armor System: NOT INITIALIZED";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("Armor System Status:\n");
        status.append("  ArmorSystem: ").append(armorSystem != null ? "✓" : "✗").append("\n");
        status.append("  Enabled: ").append(enabled ? "✓" : "✗").append("\n");
        
        return status.toString();
    }
    
    // ===========================
    // RUNTIME CONFIGURATION
    // ===========================
    
    /**
     * Enable or disable armor damage reduction at runtime
     * Note: This doesn't remove the system, just disables its effect
     */
    public void setEnabled(boolean enabled) {
        requireInitialized();
        this.enabled = enabled;
        
        if (enabled && armorSystem == null) {
            // Need to initialize armor system
            armorSystem = ArmorSystem.initialize();
            log.debug("ArmorSystem initialized and enabled");
        } else if (!enabled) {
            log.warn("Armor damage reduction disabled. System remains initialized for performance.");
        }
    }
    
    /**
     * Check if armor damage reduction is enabled
     */
    public boolean isEnabled() {
        return enabled && initialized;
    }
    
    // ===========================
    // UTILITY METHODS
    // ===========================
    
    /**
     * Calculate armor points for a player
     * 
     * @param player The player to check
     * @return Total armor points from equipped armor
     */
    public int getArmorPoints(Player player) {
        requireInitialized();
        if (armorSystem == null) return 0;
        return armorSystem.getArmorPoints(player);
    }
    
    /**
     * Calculate damage after armor reduction
     * This is exposed for external use cases
     * 
     * @param player The player taking damage
     * @param damage The original damage amount
     * @param damageType The type of damage
     * @return The reduced damage amount
     */
    public float calculateReducedDamage(Player player, float damage, net.minestom.server.registry.RegistryKey<net.minestom.server.entity.damage.DamageType> damageType) {
        requireInitialized();
        if (armorSystem == null || !enabled) return damage;
        return armorSystem.calculateDamageAfterArmor(player, damage, damageType);
    }
    
    // ===========================
    // SHUTDOWN
    // ===========================
    
    /**
     * Shutdown all systems (for server stop or reinitialize)
     */
    public void shutdown() {
        shutdownWithWrapper(() -> {
            // ArmorSystem doesn't have explicit shutdown
            armorSystem = null;
        });
    }
    
    // ===========================
    // SYSTEM ACCESS (for advanced use)
    // ===========================
    
    public ArmorSystem getArmorSystem() {
        requireInitialized();
        return armorSystem;
    }
}

package com.minestom.mechanics.manager;

import com.minestom.mechanics.config.gameplay.GameplayConfig;
import com.minestom.mechanics.config.gameplay.EyeHeightConfig;
import com.minestom.mechanics.config.gameplay.MovementConfig;
import com.minestom.mechanics.config.gameplay.HitboxConfig;
import com.minestom.mechanics.config.gameplay.PlayerCollisionConfig;
import com.minestom.mechanics.config.gameplay.DamageConfig;
import com.minestom.mechanics.features.gameplay.EyeHeightSystem;
import com.minestom.mechanics.features.gameplay.MovementRestrictionSystem;
import com.minestom.mechanics.features.gameplay.HitboxSystem;
import com.minestom.mechanics.features.gameplay.PlayerCollisionSystem;
import com.minestom.mechanics.damage.DamageFeature;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

/**
 * GameplayManager - Manages general gameplay mechanics
 * 
 * Independent of combat systems - handles:
 * - Eye height enforcement
 * - Movement restrictions  
 * - Hitbox control
 * - Player collision control
 * - Environmental damage (fall, fire, etc.)
 * 
 * Can be used with or without CombatManager.
 */
public class GameplayManager extends AbstractManager<GameplayManager> {
    private static GameplayManager instance;
    
    private EyeHeightSystem eyeHeightSystem;
    private MovementRestrictionSystem movementSystem;
    private HitboxSystem hitboxSystem;
    private PlayerCollisionSystem playerCollisionSystem;
    private DamageFeature damageFeature;
    
    private GameplayManager() {
        super("GameplayManager");
    }
    
    public static GameplayManager getInstance() {
        if (instance == null) {
            instance = new GameplayManager();
        }
        return instance;
    }
    
    /**
     * Initialize gameplay systems with configuration.
     * 
     * @param config The gameplay configuration
     * @return this manager for chaining
     */
    public GameplayManager initialize(GameplayConfig config) {
        return initializeWithWrapper(() -> {
            // Initialize eye height system if enabled
            if (config.getEyeHeight().enabled()) {
                log.debug("Initializing EyeHeightSystem...");
                eyeHeightSystem = EyeHeightSystem.initialize(config.getEyeHeight());
            }
            
            // Initialize movement system if it has restrictions
            if (config.getMovement().hasRestrictions()) {
                log.debug("Initializing MovementRestrictionSystem...");
                movementSystem = MovementRestrictionSystem.initialize(config.getMovement());
            }
            
            // Initialize hitbox system if fixed hitbox is enforced
            if (config.getHitbox().enforceFixed()) {
                log.debug("Initializing HitboxSystem...");
                hitboxSystem = HitboxSystem.initialize(config.getHitbox());
            }
            
            // Initialize player collision system
            log.debug("Initializing PlayerCollisionSystem...");
            playerCollisionSystem = PlayerCollisionSystem.initialize(config.getPlayerCollision());
            
            // Initialize damage feature
            log.debug("Initializing DamageFeature...");
            damageFeature = DamageFeature.initialize(config.getDamage());
        });
    }
    
    /**
     * Get a configuration builder for custom gameplay setup.
     * 
     * @return a new ConfigurationBuilder instance
     */
    public ConfigurationBuilder configure() {
        if (initialized) {
            throw new IllegalStateException("Cannot configure after initialization! Call shutdown() first.");
        }
        return new ConfigurationBuilder();
    }
    
    // ===========================
    // ABSTRACT METHOD IMPLEMENTATIONS
    // ===========================

    @Override
    protected String getSystemName() {
        return "GameplayManager";
    }

    @Override
    protected void logMinimalConfig() {
        // This would need access to the config, but we don't store it
        // For now, just log that systems are initialized
        log.info("Gameplay systems initialized");
    }

    @Override
    protected void cleanup() {
        // Clear all player data
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(this::cleanupPlayer);
        
        // Reset references
        eyeHeightSystem = null;
        movementSystem = null;
        hitboxSystem = null;
        playerCollisionSystem = null;
        damageFeature = null;
    }

    @Override
    public String getStatus() {
        if (!initialized) {
            return "Gameplay Systems: NOT INITIALIZED";
        }

        StringBuilder status = new StringBuilder();
        status.append("Gameplay Systems Status:\n");
        status.append("  EyeHeightSystem: ").append(eyeHeightSystem != null ? "✓" : "✗").append("\n");
        status.append("  MovementRestrictionSystem: ").append(movementSystem != null ? "✓" : "✗").append("\n");
        status.append("  HitboxSystem: ").append(hitboxSystem != null ? "✓" : "✗").append("\n");
        status.append("  PlayerCollisionSystem: ").append(playerCollisionSystem != null ? "✓" : "✗").append("\n");
        status.append("  DamageFeature: ").append(damageFeature != null ? "✓" : "✗").append("\n");

        return status.toString();
    }
    
    @Override
    public void cleanupPlayer(Player player) {
        if (!initialized) return;

        if (eyeHeightSystem != null) {
            // EyeHeightSystem handles its own cleanup
        }
        if (movementSystem != null) {
            // MovementRestrictionSystem handles its own cleanup
        }
        if (hitboxSystem != null) {
            // HitboxSystem handles its own cleanup
        }
        if (playerCollisionSystem != null) {
            // PlayerCollisionSystem handles its own cleanup
        }
        if (damageFeature != null) {
            damageFeature.cleanup(player);
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

            // Shutdown each system
            if (eyeHeightSystem != null) {
                try {
                    // Systems handle their own cleanup
                } catch (Exception e) {
                    log.error("EyeHeightSystem shutdown failed", e);
                }
            }

            if (movementSystem != null) {
                try {
                    // Systems handle their own cleanup
                } catch (Exception e) {
                    log.error("MovementRestrictionSystem shutdown failed", e);
                }
            }

            if (hitboxSystem != null) {
                try {
                    // Systems handle their own cleanup
                } catch (Exception e) {
                    log.error("HitboxSystem shutdown failed", e);
                }
            }
            
            if (playerCollisionSystem != null) {
                try {
                    // PlayerCollisionSystem handles its own cleanup
                } catch (Exception e) {
                    log.error("PlayerCollisionSystem shutdown failed", e);
                }
            }
            
            if (damageFeature != null) {
                try {
                    damageFeature.shutdown();
                } catch (Exception e) {
                    log.error("DamageFeature shutdown failed", e);
                }
            }

            // Reset references
            eyeHeightSystem = null;
            movementSystem = null;
            hitboxSystem = null;
            playerCollisionSystem = null;
            damageFeature = null;
        });
    }
    
    // ===========================
    // RUNTIME CONFIGURATION
    // ===========================

    /**
     * Enable or disable player collisions at runtime
     */
    public void setPlayerCollisionsEnabled(boolean enabled) {
        requireInitialized();
        playerCollisionSystem.setEnabled(enabled);
        log.info("Player collisions {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Update damage configuration at runtime
     */
    public void updateDamageConfig(DamageConfig config) {
        requireInitialized();
        damageFeature.updateConfig(config);
        log.info("Damage configuration updated");
    }

    // ===========================
    // SYSTEM ACCESS
    // ===========================

    public EyeHeightSystem getEyeHeightSystem() {
        requireInitialized();
        return eyeHeightSystem;
    }

    public MovementRestrictionSystem getMovementSystem() {
        requireInitialized();
        return movementSystem;
    }

    public HitboxSystem getHitboxSystem() {
        requireInitialized();
        return hitboxSystem;
    }

    public PlayerCollisionSystem getPlayerCollisionSystem() {
        requireInitialized();
        return playerCollisionSystem;
    }

    public DamageFeature getDamageFeature() {
        requireInitialized();
        return damageFeature;
    }
    
    // ===========================
    // CONFIGURATION BUILDER
    // ===========================
    
    /**
     * Fluent API for configuring gameplay systems.
     */
    public class ConfigurationBuilder {
        private GameplayConfig config;
        
        private ConfigurationBuilder() {
            // Start with vanilla preset
            this.config = GameplayConfig.VANILLA;
        }
        
        /**
         * Start from a full preset and optionally override parts.
         * 
         * @param preset The preset to start from
         * @return this builder for chaining
         */
        public ConfigurationBuilder fromPreset(GameplayConfig preset) {
            this.config = preset;
            return this;
        }
        
        /**
         * Override eye height configuration.
         * 
         * @param eyeHeight The eye height config to use
         * @return this builder for chaining
         */
        public ConfigurationBuilder withEyeHeight(EyeHeightConfig eyeHeight) {
            this.config = GameplayConfig.builder()
                .eyeHeight(eyeHeight)
                .movement(config.getMovement())
                .hitbox(config.getHitbox())
                .playerCollision(config.getPlayerCollision())
                .damage(config.getDamage())
                .build();
            return this;
        }
        
        /**
         * Override movement configuration.
         * 
         * @param movement The movement config to use
         * @return this builder for chaining
         */
        public ConfigurationBuilder withMovement(MovementConfig movement) {
            this.config = GameplayConfig.builder()
                .eyeHeight(config.getEyeHeight())
                .movement(movement)
                .hitbox(config.getHitbox())
                .playerCollision(config.getPlayerCollision())
                .damage(config.getDamage())
                .build();
            return this;
        }
        
        /**
         * Override hitbox configuration.
         * 
         * @param hitbox The hitbox config to use
         * @return this builder for chaining
         */
        public ConfigurationBuilder withHitbox(HitboxConfig hitbox) {
            this.config = GameplayConfig.builder()
                .eyeHeight(config.getEyeHeight())
                .movement(config.getMovement())
                .hitbox(hitbox)
                .playerCollision(config.getPlayerCollision())
                .damage(config.getDamage())
                .build();
            return this;
        }
        
        /**
         * Override player collision configuration.
         * 
         * @param playerCollision The player collision config to use
         * @return this builder for chaining
         */
        public ConfigurationBuilder withPlayerCollision(PlayerCollisionConfig playerCollision) {
            this.config = GameplayConfig.builder()
                .eyeHeight(config.getEyeHeight())
                .movement(config.getMovement())
                .hitbox(config.getHitbox())
                .playerCollision(playerCollision)
                .damage(config.getDamage())
                .build();
            return this;
        }
        
        /**
         * Override damage configuration.
         * 
         * @param damage The damage config to use
         * @return this builder for chaining
         */
        public ConfigurationBuilder withDamage(DamageConfig damage) {
            this.config = GameplayConfig.builder()
                .eyeHeight(config.getEyeHeight())
                .movement(config.getMovement())
                .hitbox(config.getHitbox())
                .playerCollision(config.getPlayerCollision())
                .damage(damage)
                .build();
            return this;
        }
        
        /**
         * Apply the configuration and initialize the gameplay systems.
         * 
         * @return the GameplayManager instance
         */
        public GameplayManager apply() {
            return GameplayManager.this.initialize(config);
        }
    }
}

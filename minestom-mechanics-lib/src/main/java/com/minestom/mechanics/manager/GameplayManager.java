package com.minestom.mechanics.manager;

import com.minestom.mechanics.config.gameplay.GameplayConfig;
import com.minestom.mechanics.systems.compatibility.hitbox.EyeHeightSystem;
import com.minestom.mechanics.systems.compatibility.movement.MovementRestrictionSystem;
import com.minestom.mechanics.systems.compatibility.hitbox.HitboxSystem;
import com.minestom.mechanics.systems.misc.collision.PlayerCollisionSystem;

/**
 * GameplayManager - Manages general gameplay mechanics
 *
 * Independent of combat systems - handles:
 * - Eye height enforcement
 * - Movement restrictions
 * - Hitbox control
 * - Player collision control
 *
 * Can be used with or without CombatManager.
 */
public class GameplayManager extends AbstractManager<GameplayManager> {
    private static GameplayManager instance;

    private EyeHeightSystem eyeHeightSystem;
    private MovementRestrictionSystem movementSystem;
    private HitboxSystem hitboxSystem;
    private PlayerCollisionSystem playerCollisionSystem;

    private GameplayManager() {
        super("GameplayManager");
    }

    public static GameplayManager getInstance() {
        if (instance == null) {
            instance = new GameplayManager();
        }
        return instance;
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    /**
     * Initialize gameplay systems with configuration.
     *
     * @param config The gameplay configuration
     * @return this manager for chaining
     */
    public GameplayManager initialize(GameplayConfig config) {
        return initializeWithWrapper(() -> {
            // Initialize eye height system if enabled
            if (config.eyeHeight().enabled()) {
                log.debug("Initializing EyeHeightSystem...");
                eyeHeightSystem = EyeHeightSystem.initialize(config.eyeHeight());
                registerSystem(eyeHeightSystem, "EyeHeightSystem");
            }

            // Initialize movement system if it has restrictions
            if (config.movement().hasRestrictions()) {
                log.debug("Initializing MovementRestrictionSystem...");
                movementSystem = MovementRestrictionSystem.initialize(config.movement());
                registerSystem(movementSystem, "MovementSystem");
            }

            // Initialize hitbox system if fixed hitbox is enforced
            if (config.hitbox().enforceFixed()) {
                log.debug("Initializing HitboxSystem...");
                hitboxSystem = HitboxSystem.initialize(config.hitbox());
                registerSystem(hitboxSystem, "HitboxSystem");
            }

            // Initialize player collision system
            log.debug("Initializing PlayerCollisionSystem...");
            playerCollisionSystem = PlayerCollisionSystem.initialize(config.playerCollisionEnabled());
            registerSystem(playerCollisionSystem, "PlayerCollisionSystem");
        });
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
        log.info("Initialized {} gameplay systems", getActiveSystemCount());
    }

    // cleanup(), shutdown(), getStatus(), cleanupPlayer() are AUTO-HANDLED by AbstractManager!

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
}
package com.minestom.mechanics.config.gameplay;

/**
 * Gameplay mechanics configuration (eye height, movement, hitbox, collision).
 * Damage configuration is now separate - see DamageConfig.
 */
public class GameplayConfig {
    private final EyeHeightConfig eyeHeight;
    private final MovementConfig movement;
    private final HitboxConfig hitbox;
    private final PlayerCollisionConfig playerCollision;

    private GameplayConfig(Builder builder) {
        this.eyeHeight = builder.eyeHeight;
        this.movement = builder.movement;
        this.hitbox = builder.hitbox;
        this.playerCollision = builder.playerCollision;
    }

    public static Builder builder() {
        return new Builder();
    }

    // TODO: Add these to future "presets" package
    // Presets
    public static final GameplayConfig VANILLA = builder()
            .eyeHeight(EyeHeightConfig.VANILLA)
            .movement(MovementConfig.VANILLA)
            .hitbox(HitboxConfig.VANILLA)
            .playerCollision(PlayerCollisionConfig.defaultConfig())
            .build();

    public static final GameplayConfig MINEMEN = builder()
            .eyeHeight(EyeHeightConfig.MINECRAFT_1_8)
            .movement(MovementConfig.MINECRAFT_1_8)
            .hitbox(HitboxConfig.MINECRAFT_1_8)
            .playerCollision(PlayerCollisionConfig.noCollisions())
            .build();

    public static class Builder {
        private EyeHeightConfig eyeHeight = EyeHeightConfig.VANILLA;
        private MovementConfig movement = MovementConfig.VANILLA;
        private HitboxConfig hitbox = HitboxConfig.VANILLA;
        private PlayerCollisionConfig playerCollision = PlayerCollisionConfig.defaultConfig();

        public Builder eyeHeight(EyeHeightConfig config) {
            this.eyeHeight = config;
            return this;
        }

        public Builder movement(MovementConfig config) {
            this.movement = config;
            return this;
        }

        public Builder hitbox(HitboxConfig config) {
            this.hitbox = config;
            return this;
        }

        public Builder playerCollision(PlayerCollisionConfig config) {
            this.playerCollision = config;
            return this;
        }
        
        public GameplayConfig build() {
            return new GameplayConfig(this);
        }
    }

    // Getters
    public EyeHeightConfig getEyeHeight() { return eyeHeight; }
    public MovementConfig getMovement() { return movement; }
    public HitboxConfig getHitbox() { return hitbox; }
    public PlayerCollisionConfig getPlayerCollision() { return playerCollision; }
}
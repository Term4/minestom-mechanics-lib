package com.minestom.mechanics.config.gameplay;

/**
 * Gameplay mechanics configuration (eye height, movement, hitbox, collision).
 * Follows the same pattern as CombatConfig - nested configs with convenience delegates.
 *
 * Usage:
 * <pre>
 * GameplayConfig config = GameplayPresets.MINEMEN
 *     .withStandingEyeHeight(1.65)
 *     .withPlayerCollision(false);
 * </pre>
 */
public record GameplayConfig(
        // Nested configs (complex)
        EyeHeightConfig eyeHeight,
        MovementConfig movement,
        HitboxConfig hitbox,

        // Flattened (simple - just one boolean)
        boolean playerCollisionEnabled
) {
    // Validation
    public GameplayConfig {
        if (eyeHeight == null || movement == null || hitbox == null)
            throw new IllegalArgumentException("Nested configs cannot be null");
    }

    // ===== NESTED CONFIG "WITH" METHODS =====

    public GameplayConfig withEyeHeight(EyeHeightConfig config) {
        return new GameplayConfig(config, movement, hitbox, playerCollisionEnabled);
    }

    public GameplayConfig withMovement(MovementConfig config) {
        return new GameplayConfig(eyeHeight, config, hitbox, playerCollisionEnabled);
    }

    public GameplayConfig withHitbox(HitboxConfig config) {
        return new GameplayConfig(eyeHeight, movement, config, playerCollisionEnabled);
    }

    // ===== FLATTENED FIELD "WITH" METHOD =====

    public GameplayConfig withPlayerCollision(boolean enabled) {
        return new GameplayConfig(eyeHeight, movement, hitbox, enabled);
    }

    // ===== CONVENIENCE DELEGATES (like CombatConfig does for knockback) =====

    // Eye height delegates
    public GameplayConfig withEyeHeightEnabled(boolean enabled) {
        return new GameplayConfig(eyeHeight.withEnabled(enabled), movement, hitbox, playerCollisionEnabled);
    }

    public GameplayConfig withStandingEyeHeight(double height) {
        return new GameplayConfig(eyeHeight.withStandingHeight(height), movement, hitbox, playerCollisionEnabled);
    }

    public GameplayConfig withSneakingEyeHeight(double height) {
        return new GameplayConfig(eyeHeight.withSneakingHeight(height), movement, hitbox, playerCollisionEnabled);
    }

    // Movement delegates
    public GameplayConfig withSwimming(boolean allow) {
        return new GameplayConfig(eyeHeight, movement.withSwimming(allow), hitbox, playerCollisionEnabled);
    }

    public GameplayConfig withCrawling(boolean allow) {
        return new GameplayConfig(eyeHeight, movement.withCrawling(allow), hitbox, playerCollisionEnabled);
    }

    public GameplayConfig withElytraFlying(boolean allow) {
        return new GameplayConfig(eyeHeight, movement.withElytraFlying(allow), hitbox, playerCollisionEnabled);
    }

    // Hitbox delegates
    public GameplayConfig withEnforceFixedHitbox(boolean enforce) {
        return new GameplayConfig(eyeHeight, movement, hitbox.withEnforceFixed(enforce), playerCollisionEnabled);
    }

    public GameplayConfig withHitboxDimensions(double width, double height) {
        return new GameplayConfig(eyeHeight, movement, hitbox.withDimensions(width, height), playerCollisionEnabled);
    }

    // ===== COMPATIBILITY GETTERS (for systems that expect nested configs) =====

    public EyeHeightConfig getEyeHeight() { return eyeHeight; }
    public MovementConfig getMovement() { return movement; }
    public HitboxConfig getHitbox() { return hitbox; }
}
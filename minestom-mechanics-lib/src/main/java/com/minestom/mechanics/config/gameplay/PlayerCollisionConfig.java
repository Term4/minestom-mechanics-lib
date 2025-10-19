package com.minestom.mechanics.config.gameplay;

// TODO: Very small config. Probably consolidate with hitboxes.
//  Also why are there presets? It's a simple boolean.
//  In the future I might want to add per-player collision settings, or
//  add some method to enforce collisions for 1.8 clients (think about
//  how we originally were preventing modern clients from sneaking
//  under 1.5 blocks. a SOFT push, all we'd need to do is NOT cancel player move event,
//  and NOT teleport them. Simple, soft velocity push originating from other players center.

/**
 * @deprecated This config has been flattened into GameplayConfig as a single boolean.
 * Use {@link GameplayConfig#playerCollisionEnabled()} instead.
 * Will be removed in v2.0.
 */
@Deprecated(since = "1.5", forRemoval = true)
public record PlayerCollisionConfig(boolean enabled) {

    public static PlayerCollisionConfig defaultConfig() {
        return new PlayerCollisionConfig(true);
    }

    public static PlayerCollisionConfig noCollisions() {
        return new PlayerCollisionConfig(false);
    }
}
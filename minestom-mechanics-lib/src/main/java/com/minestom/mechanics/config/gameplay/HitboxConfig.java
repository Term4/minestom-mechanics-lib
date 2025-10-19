package com.minestom.mechanics.config.gameplay;

// TODO: Ensure no overlap with eyeheight. There SHOULDN'T be,
//  as eye height manages players eye height, not their hitboxes,
//  but just checking. I think maybe moving eye height, hitboxes,
//  player collision, etc all to ONE bounding box package would
//  be much easier and make more sense

/**
 * Configuration for player hitbox enforcement.
 * Control collision box dimensions and validation.
 */
public record HitboxConfig(
        boolean enforceFixed,
        double width,
        double height,
        boolean heightChangesOnSneak,
        double sneakingHeight,
        int validationIntervalTicks,
        boolean strictCollision
) {
    // Validation
    public HitboxConfig {
        if (width <= 0 || height <= 0 || sneakingHeight <= 0)
            throw new IllegalArgumentException("Hitbox dimensions must be positive");
        if (validationIntervalTicks < 1)
            throw new IllegalArgumentException("Validation interval must be >= 1");
    }

    // TODO: Get these values from a constants class!!!
    // Compact constructor
    public HitboxConfig(boolean enforceFixed, double width, double height) {
        this(enforceFixed, width, height, true, height - 0.3, 5, false);
    }

    // Presets
    public static HitboxConfig minecraft18() {
        return new HitboxConfig(true, 0.6, 1.8, false, 1.5, 5, true);
    }

    public static HitboxConfig vanilla() {
        return new HitboxConfig(false, 0.6, 1.8, true, 1.5, 5, false);
    }

    // TODO: What is enforceFixed? What does this do?
    // "With" methods
    public HitboxConfig withEnforceFixed(boolean enforce) {
        return new HitboxConfig(enforce, width, height, heightChangesOnSneak,
                sneakingHeight, validationIntervalTicks, strictCollision);
    }

    public HitboxConfig withDimensions(double width, double height) {
        return new HitboxConfig(enforceFixed, width, height, heightChangesOnSneak,
                sneakingHeight, validationIntervalTicks, strictCollision);
    }

    public HitboxConfig withWidth(double width) {
        return new HitboxConfig(enforceFixed, width, height, heightChangesOnSneak,
                sneakingHeight, validationIntervalTicks, strictCollision);
    }

    public HitboxConfig withHeight(double height) {
        return new HitboxConfig(enforceFixed, width, height, heightChangesOnSneak,
                sneakingHeight, validationIntervalTicks, strictCollision);
    }

    public HitboxConfig withHeightChangesOnSneak(boolean changes) {
        return new HitboxConfig(enforceFixed, width, height, changes,
                sneakingHeight, validationIntervalTicks, strictCollision);
    }

    public HitboxConfig withSneakingHeight(double height) {
        return new HitboxConfig(enforceFixed, width, this.height, heightChangesOnSneak,
                height, validationIntervalTicks, strictCollision);
    }

    // TODO: What is validationIntervalTicks? What does this do? The way we're "changing the hitbox" is server side, and players send packets when they
    //  start / stop sneaking. What are "validation ticks" for? Server side hitboxes will never appear synced with client side hitboxes, so we need to
    //  shouldn't worry about validating them.
    public HitboxConfig withValidationInterval(int ticks) {
        return new HitboxConfig(enforceFixed, width, height, heightChangesOnSneak,
                sneakingHeight, ticks, strictCollision);
    }

    // TODO: What is strictCollision? Is this for projectiles or something?? Player collision is handled in gameplay. Although actually I think maybe moving
    //  eye height, hitboxes, player collision, etc all to ONE bounding box package would be much easier and make more sense
    public HitboxConfig withStrictCollision(boolean strict) {
        return new HitboxConfig(enforceFixed, width, height, heightChangesOnSneak,
                sneakingHeight, validationIntervalTicks, strict);
    }
}
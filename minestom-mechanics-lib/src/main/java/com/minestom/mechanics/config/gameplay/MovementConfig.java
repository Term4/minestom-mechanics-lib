package com.minestom.mechanics.config.gameplay;

import net.minestom.server.entity.EntityPose;
import java.util.EnumSet;
import java.util.Set;

// TODO: Right now I feel like this is a hacky way to solve this problem.
//  There's probably a cleaner way, check the minestom API.
//  ALSO should probably be moved over to the player package
//  (player package should manage OPTIONAL client version detection via ViaVersion vv:mod_details)
//  But introduce a player package, and include the bounding box, poses, version detection,
//  preferences, maybe some others idk

/**
 * Configuration for allowed/disallowed movement mechanics.
 * Control which poses and movement modes are allowed server-wide.
 */
public record MovementConfig(
        boolean allowSwimming,
        boolean allowCrawling,
        boolean allowElytraFlying,
        boolean allowSpinAttack,
        Set<EntityPose> allowedPoses,
        int checkIntervalTicks
) {
    // Validation
    public MovementConfig {
        if (checkIntervalTicks < 1)
            throw new IllegalArgumentException("Check interval must be >= 1");
        // Defensive copy for immutability
        allowedPoses = EnumSet.copyOf(allowedPoses);
    }

    // Presets
    public static MovementConfig minecraft18() {
        return new MovementConfig(
                false, false, false, false,
                EnumSet.of(EntityPose.STANDING, EntityPose.SNEAKING),
                1
        );
    }

    public static MovementConfig vanilla() {
        return new MovementConfig(
                true, true, true, true,
                EnumSet.allOf(EntityPose.class),
                1
        );
    }

    // Helper
    public boolean hasRestrictions() {
        return !allowSwimming || !allowCrawling || !allowElytraFlying || !allowSpinAttack;
    }

    // "With" methods
    public MovementConfig withSwimming(boolean allow) {
        return new MovementConfig(allow, allowCrawling, allowElytraFlying,
                allowSpinAttack, allowedPoses, checkIntervalTicks);
    }

    public MovementConfig withCrawling(boolean allow) {
        return new MovementConfig(allowSwimming, allow, allowElytraFlying,
                allowSpinAttack, allowedPoses, checkIntervalTicks);
    }

    public MovementConfig withElytraFlying(boolean allow) {
        return new MovementConfig(allowSwimming, allowCrawling, allow,
                allowSpinAttack, allowedPoses, checkIntervalTicks);
    }

    public MovementConfig withSpinAttack(boolean allow) {
        return new MovementConfig(allowSwimming, allowCrawling, allowElytraFlying,
                allow, allowedPoses, checkIntervalTicks);
    }

    public MovementConfig withAllowedPoses(Set<EntityPose> poses) {
        return new MovementConfig(allowSwimming, allowCrawling, allowElytraFlying,
                allowSpinAttack, poses, checkIntervalTicks);
    }

    public MovementConfig withCheckInterval(int ticks) {
        return new MovementConfig(allowSwimming, allowCrawling, allowElytraFlying,
                allowSpinAttack, allowedPoses, ticks);
    }

    public MovementConfig allowAll() {
        return new MovementConfig(true, true, true, true,
                EnumSet.allOf(EntityPose.class), checkIntervalTicks);
    }
}
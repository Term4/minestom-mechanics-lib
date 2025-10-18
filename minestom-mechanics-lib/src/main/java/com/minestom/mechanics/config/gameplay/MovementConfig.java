package com.minestom.mechanics.config.gameplay;

import net.minestom.server.entity.EntityPose;
import java.util.Set;
import java.util.EnumSet;

// TODO: Right now I feel like this is a hacky way to solve this problem.
//  There's probably a cleaner way, check the minestom API.
//  ALSO should probably be moved over to the player package
//  (player package should manage OPTIONAL client version detection-
//  -use a plugin messanger and a velocity plugin with the viaversion API)
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
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final MovementConfig MINECRAFT_1_8 = new MovementConfig(
        false, false, false, false,
        EnumSet.of(EntityPose.STANDING, EntityPose.SNEAKING),
        1
    );
        
    public static final MovementConfig VANILLA = new MovementConfig(
        true, true, true, true,
        EnumSet.allOf(EntityPose.class),
        1
    );
    
    public boolean hasRestrictions() {
        return !allowSwimming || !allowCrawling || !allowElytraFlying || !allowSpinAttack;
    }
    
    public static class Builder {
        private boolean allowSwimming = true;
        private boolean allowCrawling = true;
        private boolean allowElytraFlying = true;
        private boolean allowSpinAttack = true;
        private Set<EntityPose> allowedPoses = EnumSet.allOf(EntityPose.class);
        private int checkIntervalTicks = 1;
        
        public Builder allowSwimming(boolean allow) {
            this.allowSwimming = allow;
            return this;
        }
        
        public Builder allowCrawling(boolean allow) {
            this.allowCrawling = allow;
            return this;
        }
        
        public Builder allowElytraFlying(boolean allow) {
            this.allowElytraFlying = allow;
            return this;
        }
        
        public Builder allowSpinAttack(boolean allow) {
            this.allowSpinAttack = allow;
            return this;
        }
        
        public Builder allowedPoses(Set<EntityPose> poses) {
            this.allowedPoses = poses;
            return this;
        }
        
        public Builder checkIntervalTicks(int ticks) {
            this.checkIntervalTicks = ticks;
            return this;
        }
        
        public Builder allowAll() {
            this.allowSwimming = true;
            this.allowCrawling = true;
            this.allowElytraFlying = true;
            this.allowSpinAttack = true;
            this.allowedPoses = EnumSet.allOf(EntityPose.class);
            return this;
        }
        
        public MovementConfig build() {
            return new MovementConfig(allowSwimming, allowCrawling, allowElytraFlying, 
                                    allowSpinAttack, allowedPoses, checkIntervalTicks);
        }
    }
}

package com.minestom.mechanics.systems.damage.util;

import com.minestom.mechanics.systems.util.LogUtil;
import com.minestom.mechanics.config.gameplay.DamageConfig;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.tag.Tag;

import static com.minestom.mechanics.config.constants.CombatConstants.SAFE_FALL_DISTANCE;

// TODO: As I said, introduce an AbstractDamageTracker class, and extend for different damage types.
//  ALSO could be cool to add a way for library users to add their own damage types without modifying the
//  library source code. That way if I'm missing a type they want, or they want to add
//  a custom damage type, no issues.

/**
 * Tracks and applies fall damage to players.
 * Handles fall distance accumulation and damage calculation.
 */
public class FallDamageTracker {
    private static final LogUtil.SystemLogger log = LogUtil.system("FallDamageTracker");
    
    private static final Tag<Double> FALL_DISTANCE = Tag.Double("fall_distance").defaultValue(0.0);
    private static final Tag<Boolean> WAS_ON_GROUND = Tag.Boolean("was_on_ground").defaultValue(true);
    private static final Tag<Double> LAST_Y_POS = Tag.Double("last_y_pos").defaultValue(0.0);
    
    private final DamageConfig config;

    public FallDamageTracker(DamageConfig config) {
        this.config = config;
    }

    /**
     * Track fall damage for a player on each tick
     */
    public void trackFallDamage(Player player) {
        if (!config.isFallDamageEnabled()) return;

        Pos currentPos = player.getPosition();
        double currentY = currentPos.y();
        double lastY = player.getTag(LAST_Y_POS);
        boolean isGrounded = player.isOnGround();
        boolean wasGrounded = player.getTag(WAS_ON_GROUND);

        // Accumulate fall distance while falling
        if (currentY < lastY && !isGrounded) {
            double currentFall = player.getTag(FALL_DISTANCE);
            double fallDelta = lastY - currentY;
            player.setTag(FALL_DISTANCE, currentFall + fallDelta);
        }

        // Detect landing transition (was airborne, now grounded)
        if (isGrounded && !wasGrounded) {
            applyFallDamage(player);
        }

        // Update state for next tick
        player.setTag(LAST_Y_POS, currentY);
        player.setTag(WAS_ON_GROUND, isGrounded);
    }

    /**
     * Apply fall damage to a player
     */
    private void applyFallDamage(Player player) {
        double fallDistance = player.getTag(FALL_DISTANCE);

        if (fallDistance > SAFE_FALL_DISTANCE) {
            float baseDamage = (float) (fallDistance - SAFE_FALL_DISTANCE);
            float finalDamage = baseDamage * config.getFallDamageMultiplier();

            if (player.hasEffect(PotionEffect.SLOW_FALLING)) {
                finalDamage = 0;
            }

            if (finalDamage > 0) {
                player.damage(new Damage(
                        DamageType.FALL,
                        null,
                        null,
                        player.getPosition(),
                        finalDamage
                ));
                
                // ✅ REMOVED: Duplicate damage logging - now handled centrally in DamageFeature
            }
        }

        // Always reset after checking, even if no damage dealt
        player.setTag(FALL_DISTANCE, 0.0);
    }

    /**
     * Reset fall distance for a player
     */
    public void resetFallDistance(Player player) {
        player.setTag(FALL_DISTANCE, 0.0);
        log.debug("Reset fall distance for {}", player.getUsername());
    }

    /**
     * Get current fall distance for a player
     */
    public double getFallDistance(Player player) {
        return player.getTag(FALL_DISTANCE);
    }
}

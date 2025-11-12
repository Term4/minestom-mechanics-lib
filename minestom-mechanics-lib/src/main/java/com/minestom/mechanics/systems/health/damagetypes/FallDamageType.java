package com.minestom.mechanics.systems.health.damagetypes;

import com.minestom.mechanics.systems.health.HealthConfig;
import com.minestom.mechanics.systems.health.HealthEvent;
import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.tag.Tag;

import static com.minestom.mechanics.config.constants.CombatConstants.SAFE_FALL_DISTANCE;

/**
 * Fall damage type implementation.
 * Tracks and applies fall damage to players.
 */
public class FallDamageType extends AbstractDamageType {
    private static final LogUtil.SystemLogger log = LogUtil.system("FallDamageType");
    
    private static final Tag<Double> FALL_DISTANCE = Tag.Double("fall_distance").defaultValue(0.0);
    private static final Tag<Boolean> WAS_ON_GROUND = Tag.Boolean("was_on_ground").defaultValue(true);
    private static final Tag<Double> LAST_Y_POS = Tag.Double("last_y_pos").defaultValue(0.0);
    
    public FallDamageType(HealthConfig config) {
        super(config, "FALL", HealthSystem.FALL_DAMAGE);
    }
    
    @Override
    protected boolean isEnabledByDefault() {
        return config.fallDamageEnabled();
    }
    
    @Override
    protected float getDefaultMultiplier() {
        return config.fallDamageMultiplier();
    }
    
    @Override
    public boolean shouldHandle(HealthEvent event) {
        // Fall damage is applied directly, not through EntityDamageEvent
        // So we return false here - we handle it via PlayerTickEvent
        return false;
    }
    
    @Override
    public void processHealthEvent(HealthEvent event) {
        // Fall damage is handled via trackFallDamage, not through health events
    }
    
    /**
     * Track fall damage for a player on each tick
     */
    public void trackFallDamage(Player player) {
        if (!isEnabled(player)) return;

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
            float finalDamage = calculateDamage(player, baseDamage);

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
    
    @Override
    public void cleanup(LivingEntity entity) {
        super.cleanup(entity);
        if (entity instanceof Player player) {
            player.removeTag(FALL_DISTANCE);
            player.removeTag(WAS_ON_GROUND);
            player.removeTag(LAST_Y_POS);
        }
    }
}


package com.minestom.mechanics.features.knockback.components;

import com.minestom.mechanics.features.blocking.BlockingSystem;
import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;

/**
 * Component responsible for applying modifiers to knockback.
 * Handles blocking reduction and other combat modifiers.
 */
public class KnockbackModifier {
    
    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackModifier");
    
    /**
     * Apply blocking reduction to knockback.
     */
    public KnockbackHandler.KnockbackStrength applyBlockingReduction(LivingEntity victim, 
                                                                   KnockbackHandler.KnockbackStrength base) {
        if (!(victim instanceof Player player)) {
            return base;
        }
        
        try {
            BlockingSystem blocking = BlockingSystem.getInstance();
            if (blocking.isBlocking(player) && blocking.isEnabled()) {
                double hMultiplier = blocking.getConfig().getKnockbackHorizontalMultiplier();
                double vMultiplier = blocking.getConfig().getKnockbackVerticalMultiplier();

                double newHorizontal = base.horizontal * hMultiplier;
                double newVertical = base.vertical * vMultiplier;

                log.debug("{} blocked knockback (h: {:.0f}%, v: {:.0f}%)",
                        player.getUsername(),
                        (1-hMultiplier)*100,
                        (1-vMultiplier)*100);
                
                return new KnockbackHandler.KnockbackStrength(newHorizontal, newVertical);
            }
        } catch (IllegalStateException e) {
            // BlockingSystem not initialized yet
        }
        
        return base;
    }
    
    /**
     * Apply air multipliers to knockback.
     */
    public KnockbackHandler.KnockbackStrength applyAirMultipliers(LivingEntity victim,
                                                                KnockbackHandler.KnockbackStrength base,
                                                                double airHorizontalMultiplier,
                                                                double airVerticalMultiplier) {
        if (!victim.isOnGround()) {
            double newHorizontal = base.horizontal * airHorizontalMultiplier;
            double newVertical = base.vertical * airVerticalMultiplier;
            return new KnockbackHandler.KnockbackStrength(newHorizontal, newVertical);
        }
        
        return base;
    }
    
    /**
     * Apply falling knockback modifiers.
     */
    public KnockbackHandler.KnockbackStrength applyFallingModifiers(LivingEntity victim,
                                                                   KnockbackHandler.KnockbackStrength base) {
        net.minestom.server.coordinate.Vec velocity = victim.getVelocity();
        boolean isInAir = !victim.isOnGround();
        boolean isFalling = isInAir && velocity.y() < -0.1; // FALLING_VELOCITY_THRESHOLD
        
        if (isFalling) {
            // Ensure minimum vertical knockback for falling players
            double minVertical = 0.1; // MIN_FALLING_KNOCKBACK
            double newVertical = Math.max(Math.abs(base.vertical), minVertical);
            return new KnockbackHandler.KnockbackStrength(base.horizontal, newVertical);
        }
        
        return base;
    }
}

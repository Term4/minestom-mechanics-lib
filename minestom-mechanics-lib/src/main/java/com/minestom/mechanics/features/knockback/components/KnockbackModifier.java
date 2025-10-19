package com.minestom.mechanics.features.knockback.components;

import com.minestom.mechanics.features.blocking.BlockingSystem;
import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;

// TODO: Ensure this is actually necessary and not handled elsewhere. ALSO maybe simplify?
//  Seems like a lot for a simple method. ALSO would this be necessary if we separated legacy knockback from modern?
//  MAYBE generalize this to be able to add custom knockback modifications on a per hit instance? would allow custom
//  items (without enchants), custom projectiles, etc

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
                double hReduction = blocking.getKnockbackHorizontalReduction();
                double vReduction = blocking.getKnockbackVerticalReduction();

                double newHorizontal = base.horizontal * (1.0 - hReduction);
                double newVertical = base.vertical * (1.0 - vReduction);

                log.debug("{} blocked knockback (h: {:.0f}%, v: {:.0f}%)",
                        player.getUsername(),
                        hReduction*100,
                        vReduction*100);

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
        // TODO: Move hardcoded value to constants class
        boolean isFalling = isInAir && velocity.y() < -0.1; // FALLING_VELOCITY_THRESHOLD
        
        if (isFalling) {
            // TODO: Move hardcoded value to constants class
            // Ensure minimum vertical knockback for falling players
            double minVertical = 0.1; // MIN_FALLING_KNOCKBACK
            double newVertical = Math.max(Math.abs(base.vertical), minVertical);
            return new KnockbackHandler.KnockbackStrength(base.horizontal, newVertical);
        }
        
        return base;
    }
}

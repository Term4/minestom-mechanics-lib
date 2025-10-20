package com.minestom.mechanics.features.knockback.components;
import com.minestom.mechanics.features.knockback.components.KnockbackStrength;
import com.minestom.mechanics.features.blocking.BlockingSystem;
import com.minestom.mechanics.util.GameplayUtils;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.MechanicsConstants;
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
     * Apply air multipliers to knockback.
     */
    public KnockbackStrength applyAirMultipliers(LivingEntity victim,
                                                                 KnockbackStrength base,
                                                                 double airHorizontalMultiplier,
                                                                 double airVerticalMultiplier) {
        if (!victim.isOnGround()) {
            double newHorizontal = base.horizontal() * airHorizontalMultiplier;
            double newVertical = base.vertical() * airVerticalMultiplier;
            return new KnockbackStrength(newHorizontal, newVertical);
        }
        
        return base;
    }
}

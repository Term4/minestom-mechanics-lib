package com.minestom.mechanics.features.knockback.components;

import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.features.knockback.sync.KnockbackSyncHandler;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;

/**
 * Component responsible for applying knockback velocity to entities.
 * Handles velocity application and client synchronization.
 */
public class KnockbackVelocityHandler {
    
    // private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackVelocityHandler");
    
    private final KnockbackStateManager stateManager;
    private final boolean knockbackSyncEnabled;
    
    public KnockbackVelocityHandler(KnockbackStateManager stateManager, boolean knockbackSyncEnabled) {
        this.stateManager = stateManager;
        this.knockbackSyncEnabled = knockbackSyncEnabled;
    }

    // TODO: Seems like duplicate code, no? Did we not apply this logic in the calculator?
    /**
     * Apply knockback velocity to victim.
     */
    public void applyKnockbackVelocity(LivingEntity victim, 
                                     Entity attacker, 
                                     Vec knockbackDirection, 
                                     KnockbackHandler.KnockbackStrength strength,
                                     KnockbackHandler.KnockbackType type) {
        
        // Calculate final velocity
        Vec newVelocity = calculateFinalVelocity(victim, knockbackDirection, strength, type);
        
        // Apply sync compensation if enabled
        if (victim instanceof Player player && knockbackSyncEnabled) {
            newVelocity = KnockbackSyncHandler.getInstance()
                    .compensateKnockback(player, newVelocity, attacker, player.isOnGround());
        }
        
        // Apply the velocity
        victim.setVelocity(newVelocity);
        
        // CRITICAL: Send velocity packet to client!
        if (victim instanceof Player player) {
            EntityVelocityPacket velocityPacket = new EntityVelocityPacket(
                    player.getEntityId(),
                    newVelocity  // Just pass the Vec directly - packet handles conversion
            );
            player.sendPacket(velocityPacket);
        }
        
        // Record knockback application
        if (victim instanceof Player player) {
            stateManager.recordKnockback(player, newVelocity);
        }
    }

    // TODO: Yes this is direct duplication from the knockback calculator.
    /**
     * Calculate final velocity from knockback components.
     */
    private Vec calculateFinalVelocity(LivingEntity victim, Vec direction, 
                                     KnockbackHandler.KnockbackStrength strength,
                                     KnockbackHandler.KnockbackType type) {
        double tps = 20.0; // ServerFlag.SERVER_TICKS_PER_SECOND
        double horizontal = strength.horizontal / tps;
        double vertical = strength.vertical / tps;

        Vec oldVelocity = victim.getVelocity();

        // Handle ground state velocity correction
        if (victim.isOnGround()) {
            Vec minestomVel = victim.getVelocity();
            if (minestomVel.y() > 0.1) {
                oldVelocity = oldVelocity.withY(minestomVel.y());
            } else {
                oldVelocity = oldVelocity.withY(Math.max(oldVelocity.y(), 0));
            }
        }

        // Calculate final vertical component
        double finalVertical = calculateVerticalComponent(victim, oldVelocity, vertical, tps);

        // 1.8 legacy calculation
        return new Vec(
                oldVelocity.x() / 2.0 + direction.x() * horizontal * tps,
                finalVertical,
                oldVelocity.z() / 2.0 + direction.z() * horizontal * tps
        );
    }

    // TODO: Same here
    /**
     * Calculate final vertical component of knockback.
     */
    private double calculateVerticalComponent(LivingEntity victim, Vec oldVelocity,
                                           double vertical, double tps) {
        boolean isInAir = !victim.isOnGround();
        boolean isFalling = isInAir && oldVelocity.y() <= 0.0;

        if (victim.isOnGround()) {
            return oldVelocity.y() / 2.0 + vertical * tps;
        }

        // Apply minimum falling knockback
        if (isFalling) {
            return Math.max(vertical * tps, 0.1); // MIN_FALLING_KNOCKBACK
        }

        return oldVelocity.y() / 2.0 + vertical * tps;
    }
}

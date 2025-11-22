package com.minestom.mechanics.systems.health.damagetypes;

import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.systems.health.HealthEvent;
import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.registry.RegistryKey;

/**
 * Fire damage type implementation.
 * Modifies fire damage based on configuration.
 */
public class FireDamageType extends AbstractDamageType {
    private static final LogUtil.SystemLogger log = LogUtil.system("FireDamageType");
    
    public FireDamageType(HealthConfig config) {
        super(config, "FIRE", HealthSystem.FIRE_DAMAGE);
    }
    
    @Override
    protected boolean isEnabledByDefault() {
        return config.fireDamageEnabled();
    }
    
    @Override
    protected float getDefaultMultiplier() {
        return config.fireDamageMultiplier();
    }
    
    @Override
    public boolean shouldHandle(HealthEvent event) {
        // Fire damage is handled via EntityDamageEvent, not HealthEvent
        // This will be called from HealthSystem when processing EntityDamageEvent
        return false;
    }
    
    @Override
    public void processHealthEvent(HealthEvent event) {
        // Fire damage is handled via modifyFireDamage, not through health events
    }
    
    /**
     * Modify fire damage for an entity damage event
     */
    public void modifyFireDamage(EntityDamageEvent event) {
        var damageType = event.getDamage().getType();
        LivingEntity entity = event.getEntity();

        if (isFireDamage(damageType)) {
            if (!isEnabled(entity)) {
                event.setCancelled(true);
                log.debug("Fire damage cancelled for entity");
                return;
            }

            float originalDamage = event.getDamage().getAmount();
            float newDamage = calculateDamage(null, entity, null, originalDamage);
            
            if (newDamage != originalDamage) {
                event.getDamage().setAmount(newDamage);
                
                log.debug("Fire damage modified: {:.2f} -> {:.2f} (multiplier: {:.2f})",
                        originalDamage, newDamage, getMultiplier(null, entity, null));
            }
        }
    }
    
    /**
     * Check if a damage type is fire-related
     */
    private boolean isFireDamage(RegistryKey<DamageType> damageType) {
        return damageType.equals(DamageType.ON_FIRE) ||
                damageType.equals(DamageType.IN_FIRE) ||
                damageType.equals(DamageType.LAVA);
    }
}


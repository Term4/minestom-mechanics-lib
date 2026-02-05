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
 * Cactus damage type implementation.
 * Modifies cactus damage based on configuration.
 */
public class Cactus extends AbstractDamageType {
    private static final LogUtil.SystemLogger log = LogUtil.system("CactusDamageType");
    
    public Cactus(HealthConfig config) {
        super(config, "CACTUS", HealthSystem.CACTUS_DAMAGE);
    }
    
    @Override
    protected boolean isEnabledByDefault() {
        return config.cactusDamageEnabled();
    }
    
    @Override
    protected float getDefaultMultiplier() {
        return config.cactusDamageMultiplier();
    }
    
    @Override
    public boolean shouldHandle(HealthEvent event) {
        // Cactus damage is handled via EntityDamageEvent, not HealthEvent
        return false;
    }
    
    @Override
    public void processHealthEvent(HealthEvent event) {
        // Cactus damage is handled via modifyCactusDamage, not through health events
    }
    
    /**
     * Modify cactus damage for an entity damage event
     */
    public void modifyCactusDamage(EntityDamageEvent event) {
        var damageType = event.getDamage().getType();
        LivingEntity entity = event.getEntity();

        if (isCactusDamage(damageType)) {
            if (!isEnabled(entity)) {
                event.setCancelled(true);
                log.debug("Cactus damage cancelled for entity");
                return;
            }

            float originalDamage = event.getDamage().getAmount();
            float newDamage = calculateDamage(null, entity, null, originalDamage);
            
            if (newDamage != originalDamage) {
                event.getDamage().setAmount(newDamage);
                
                log.debug("Cactus damage modified: {:.2f} -> {:.2f} (multiplier: {:.2f})",
                        originalDamage, newDamage, getMultiplier(null, entity, null));
            }
        }
    }
    
    /**
     * Check if a damage type is cactus-related
     */
    private boolean isCactusDamage(RegistryKey<DamageType> damageType) {
        return damageType.equals(DamageType.CACTUS);
    }
}


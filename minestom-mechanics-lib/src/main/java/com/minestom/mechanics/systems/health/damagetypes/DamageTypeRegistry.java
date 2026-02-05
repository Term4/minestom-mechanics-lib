package com.minestom.mechanics.systems.health.damagetypes;

import com.minestom.mechanics.systems.health.HealthEvent;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for managing all damage types.
 * Allows registration and processing of damage types.
 */
public class DamageTypeRegistry {
    private final List<AbstractDamageType> damageTypes = new CopyOnWriteArrayList<>();
    
    /**
     * Register a damage type
     */
    public void register(AbstractDamageType damageType) {
        damageTypes.add(damageType);
        damageType.initialize();
    }
    
    /**
     * Unregister a damage type
     */
    public void unregister(AbstractDamageType damageType) {
        damageTypes.remove(damageType);
    }
    
    /**
     * Get all registered damage types
     */
    public List<AbstractDamageType> getDamageTypes() {
        return new ArrayList<>(damageTypes);
    }
    
    /**
     * Process a health event through all registered damage types
     */
    public void processHealthEvent(HealthEvent event) {
        for (AbstractDamageType damageType : damageTypes) {
            if (damageType.shouldHandle(event)) {
                damageType.processHealthEvent(event);
                if (event.isCancelled()) {
                    break;
                }
            }
        }
    }
    
    /**
     * Process an entity damage event through all registered damage types
     */
    public void processEntityDamageEvent(EntityDamageEvent event) {
        for (AbstractDamageType damageType : damageTypes) {
            if (damageType instanceof FallDamage) {
                // Fall damage is handled separately via PlayerTickEvent
                continue;
            }
            if (damageType instanceof Fire fireDamage) {
                fireDamage.modifyFireDamage(event);
            } else if (damageType instanceof Cactus cactusDamage) {
                cactusDamage.modifyCactusDamage(event);
            }
            if (event.isCancelled()) {
                break;
            }
        }
    }
    
    /**
     * Cleanup all damage types for an entity
     */
    public void cleanup(LivingEntity entity) {
        for (AbstractDamageType damageType : damageTypes) {
            damageType.cleanup(entity);
        }
    }
    
    /**
     * Get a damage type by class
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractDamageType> T getDamageType(Class<T> clazz) {
        for (AbstractDamageType damageType : damageTypes) {
            if (clazz.isInstance(damageType)) {
                return (T) damageType;
            }
        }
        return null;
    }
}


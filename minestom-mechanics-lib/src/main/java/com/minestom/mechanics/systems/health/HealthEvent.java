package com.minestom.mechanics.systems.health;

import net.minestom.server.entity.LivingEntity;

/**
 * Interface for health alteration events.
 * Represents any change to an entity's health (damage, healing, regeneration).
 */
public interface HealthEvent {
    /**
     * Get the entity whose health is being altered
     */
    LivingEntity getEntity();
    
    /**
     * Get the amount of health change (positive for healing, negative for damage)
     */
    float getAmount();
    
    /**
     * Set the amount of health change
     */
    void setAmount(float amount);
    
    /**
     * Check if this event is cancelled
     */
    boolean isCancelled();
    
    /**
     * Set whether this event is cancelled
     */
    void setCancelled(boolean cancelled);
    
    /**
     * Get the type of health alteration
     */
    HealthEventType getType();
    
    /**
     * Enum for health event types
     */
    enum HealthEventType {
        DAMAGE,
        HEALING,
        REGENERATION
    }
}
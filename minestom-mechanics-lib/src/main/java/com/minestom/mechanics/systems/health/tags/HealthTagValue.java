package com.minestom.mechanics.systems.health.tags;

import com.minestom.mechanics.systems.ConfigTagWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tag value for health/damage type modifications.
 * Supports MULTIPLIER, MODIFY, and CUSTOM config overrides.
 * 
 * Usage:
 * <pre>
 * import static HealthTagValue.*;
 * 
 * // Just multiplier
 * entity.setTag(HealthSystem.DAMAGE_MULTIPLIER, healthMult(2.0f))
 * 
 * // Just modify (additive)
 * entity.setTag(HealthSystem.DAMAGE_MODIFY, healthAdd(5.0f))
 * 
 * // Combined (chainable)
 * entity.setTag(HealthSystem.DAMAGE_MULTIPLIER, healthMult(2.0f).thenAdd(5.0f))
 * </pre>
 */
public record HealthTagValue(
        @Nullable Float multiplier,
        @Nullable Float modify,
        @Nullable Boolean enabled
) implements ConfigTagWrapper<HealthTagValue> {
    
    // ===========================
    // STATIC FACTORY METHODS
    // ===========================
    
    /**
     * Create a multiplier tag value
     */
    public static HealthTagValue healthMult(float multiplier) {
        return new HealthTagValue(multiplier, null, null);
    }
    
    /**
     * Create a modify (additive) tag value
     */
    public static HealthTagValue healthAdd(float modify) {
        return new HealthTagValue(null, modify, null);
    }
    
    /**
     * Create an enabled/disabled tag value
     */
    public static HealthTagValue healthEnabled(boolean enabled) {
        return new HealthTagValue(null, null, enabled);
    }
    
    /**
     * Create a combined tag value with all fields
     */
    public static HealthTagValue healthConfig(float multiplier, float modify, boolean enabled) {
        return new HealthTagValue(multiplier, modify, enabled);
    }
    
    // ===========================
    // CHAINABLE METHODS
    // ===========================
    
    /**
     * Chain: add multiplier (returns new record)
     */
    public HealthTagValue thenMult(float multiplier) {
        return new HealthTagValue(multiplier, this.modify, this.enabled);
    }
    
    /**
     * Chain: add modify (returns new record)
     */
    public HealthTagValue thenAdd(float modify) {
        return new HealthTagValue(this.multiplier, modify, this.enabled);
    }
    
    /**
     * Chain: set enabled (returns new record)
     */
    public HealthTagValue thenEnabled(boolean enabled) {
        return new HealthTagValue(this.multiplier, this.modify, enabled);
    }
    
    // ===========================
    // COMMON PRESETS
    // ===========================
    
    /** Double damage */
    public static final HealthTagValue DOUBLE_DAMAGE = healthMult(2.0f);
    
    /** Half damage */
    public static final HealthTagValue HALF_DAMAGE = healthMult(0.5f);
    
    /** No damage */
    public static final HealthTagValue NO_DAMAGE = healthMult(0.0f);
    
    /** Damage disabled */
    public static final HealthTagValue DISABLED = healthEnabled(false);
    
    /** Damage enabled (default) */
    public static final HealthTagValue ENABLED = healthEnabled(true);
    
    // ===========================
    // ConfigTagWrapper INTERFACE
    // ===========================
    
    @Override
    public List<Double> getMultiplier() {
        return multiplier != null ? List.of((double) multiplier) : null;
    }
    
    @Override
    public List<Double> getModify() {
        return modify != null ? List.of((double) modify) : null;
    }
    
    @Override
    public HealthTagValue getCustom() {
        // For health tags, the custom config is the tag value itself
        // This allows complete override of damage calculation
        return this;
    }
}


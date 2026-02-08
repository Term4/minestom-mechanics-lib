package com.minestom.mechanics.systems.health.tags;

import com.minestom.mechanics.systems.ConfigTagWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wrapper for HealthTagValue that properly implements ConfigTagWrapper.
 * This allows the tag system to work with MULTIPLIER, MODIFY, and CUSTOM fields.
 * 
 * Usage:
 * <pre>
 * import static HealthTagWrapper.*;
 * 
 * // Multiplier (multiplicative: item × player × world)
 * item.withTag(HealthSystem.FALL_DAMAGE, healthMult(2.0))
 * player.setTag(HealthSystem.FALL_DAMAGE, healthMult(0.5))
 * world.setTag(HealthSystem.FALL_DAMAGE, healthMult(1.5))
 * 
 * // Modify (additive: item + player + world)
 * item.withTag(HealthSystem.FALL_DAMAGE, healthAdd(5.0))
 * player.setTag(HealthSystem.FALL_DAMAGE, healthAdd(-2.0))
 * 
 * // Custom config (complete override: item > player > world > server)
 * item.withTag(HealthSystem.FALL_DAMAGE, healthSet(HealthTagValue.healthConfig(2.0f, 0.0f, true)))
 * 
 * // Combined (chainable)
 * item.withTag(HealthSystem.FALL_DAMAGE, healthMult(2.0).thenAdd(5.0))
 * 
 * // Presets
 * player.setTag(HealthSystem.FALL_DAMAGE, DISABLED)
 * world.setTag(HealthSystem.FALL_DAMAGE, DOUBLE_DAMAGE)
 * </pre>
 */
public record HealthTagWrapper(
        @Nullable List<Double> multiplier,
        @Nullable List<Double> modify,
        @Nullable HealthTagValue custom
) implements ConfigTagWrapper<HealthTagValue> {
    
    // ===========================
    // STATIC FACTORY METHODS
    // ===========================
    
    /**
     * Create a multiplier wrapper (multiplicative scaling)
     * Multipliers stack: item × player × world
     */
    public static HealthTagWrapper healthMult(double multiplier) {
        return new HealthTagWrapper(List.of(multiplier), null, null);
    }
    
    /**
     * Create a modify wrapper (additive changes)
     * Modifies stack: item + player + world
     */
    public static HealthTagWrapper healthAdd(double modify) {
        return new HealthTagWrapper(null, List.of(modify), null);
    }
    
    /**
     * Create a custom config wrapper (complete override)
     * Priority: item > player > world > server default
     */
    public static HealthTagWrapper healthSet(HealthTagValue custom) {
        return new HealthTagWrapper(null, null, custom);
    }
    
    /**
     * Create a combined wrapper with multiplier and modify
     */
    public static HealthTagWrapper healthConfig(double multiplier, double modify, HealthTagValue custom) {
        return new HealthTagWrapper(
                multiplier != 1.0 ? List.of(multiplier) : null,
                modify != 0.0 ? List.of(modify) : null,
                custom
        );
    }
    
    // ===========================
    // CHAINABLE METHODS
    // ===========================
    
    /**
     * Chain: add multiplier (returns new record)
     */
    public HealthTagWrapper thenMult(double multiplier) {
        return new HealthTagWrapper(List.of(multiplier), this.modify, this.custom);
    }
    
    /**
     * Chain: add modify (returns new record)
     */
    public HealthTagWrapper thenAdd(double modify) {
        return new HealthTagWrapper(this.multiplier, List.of(modify), this.custom);
    }
    
    /**
     * Chain: set custom config (returns new record)
     */
    public HealthTagWrapper thenSet(HealthTagValue custom) {
        return new HealthTagWrapper(this.multiplier, this.modify, custom);
    }
    
    // ===========================
    // COMMON PRESETS
    // ===========================
    
    /** Double damage (2x multiplier) */
    public static final HealthTagWrapper DOUBLE_DAMAGE = healthMult(2.0);
    
    /** Half damage (0.5x multiplier) */
    public static final HealthTagWrapper HALF_DAMAGE = healthMult(0.5);
    
    /** No damage (0x multiplier) */
    public static final HealthTagWrapper NO_DAMAGE = healthMult(0.0);
    
    /** Damage disabled (custom config with enabled=false) */
    public static final HealthTagWrapper DISABLED = healthSet(new HealthTagValue(null, null, false, null, null));

    /** Damage enabled (custom config with enabled=true) */
    public static final HealthTagWrapper ENABLED = healthSet(new HealthTagValue(null, null, true, null, null));
    
    /** Blocking applies to this damage type (custom config with blockable=true) */
    public static final HealthTagWrapper BLOCKABLE = healthSet(HealthTagValue.BLOCKABLE);
    
    /** Blocking does not apply to this damage type (custom config with blockable=false) */
    public static final HealthTagWrapper NOT_BLOCKABLE = healthSet(HealthTagValue.NOT_BLOCKABLE);
    
    // ===========================
    // ConfigTagWrapper INTERFACE
    // ===========================
    
    @Override
    public List<Double> getMultiplier() {
        return multiplier;
    }
    
    @Override
    public List<Double> getModify() {
        return modify;
    }
    
    @Override
    public HealthTagValue getCustom() {
        return custom;
    }
}


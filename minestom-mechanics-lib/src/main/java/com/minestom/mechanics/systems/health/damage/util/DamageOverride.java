package com.minestom.mechanics.systems.health.damage.util;

import com.minestom.mechanics.systems.ConfigTagWrapper;
import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tag wrapper for runtime damage type overrides.
 * Used with {@code Tag.Transient} for zero-serialization runtime overrides.
 *
 * <p>Supports four override modes:</p>
 * <ul>
 *   <li><b>multiplier</b> - Multiplicative scaling, stacks across sources (item x player x world)</li>
 *   <li><b>modify</b> - Additive adjustment, stacks across sources (item + player + world)</li>
 *   <li><b>custom</b> - Full property override, first-match wins (item > player > world)</li>
 *   <li><b>configOverride</b> - Type-specific config override, first-match wins (item > player > world)</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 * // Double fire damage for this player
 * player.setTag(HealthSystem.tag("fire"), DamageOverride.mult(2.0));
 *
 * // Override fire config for this player (e.g. faster ignition)
 * player.setTag(HealthSystem.tag("fire"), DamageOverride.config(
 *     Fire.Config.DEFAULT.withIgnitionDelayTicks(5)
 * ));
 *
 * // Combine: 2x damage + custom config
 * player.setTag(HealthSystem.tag("fire"),
 *     DamageOverride.mult(2.0).withConfig(new Fire.Config(...)));
 * </pre>
 */
public record DamageOverride(
        @Nullable List<Double> multiplier,
        @Nullable List<Double> modify,
        @Nullable DamageTypeProperties custom,
        @Nullable Object configOverride
) implements ConfigTagWrapper<DamageTypeProperties> {

    // Compact constructor for backward compatibility (3-arg)
    public DamageOverride(@Nullable List<Double> multiplier, @Nullable List<Double> modify, @Nullable DamageTypeProperties custom) {
        this(multiplier, modify, custom, null);
    }

    // ===========================
    // FACTORY METHODS
    // ===========================

    /** Multiplicative damage scaling. Stacks across sources. */
    public static DamageOverride mult(double multiplier) {
        return new DamageOverride(List.of(multiplier), null, null, null);
    }

    /** Additive damage adjustment. Stacks across sources. */
    public static DamageOverride add(double modify) {
        return new DamageOverride(null, List.of(modify), null, null);
    }

    /** Full property override. Highest-priority source wins. */
    public static DamageOverride override(DamageTypeProperties properties) {
        return new DamageOverride(null, null, properties, null);
    }

    /** Type-specific config override. Highest-priority source wins. */
    public static DamageOverride config(Object config) {
        return new DamageOverride(null, null, null, config);
    }

    /** Disable this damage type entirely. */
    public static DamageOverride disabled() {
        return override(DamageTypeProperties.DISABLED);
    }

    // ===========================
    // CHAINABLE METHODS
    // ===========================

    public DamageOverride thenMult(double multiplier) {
        return new DamageOverride(List.of(multiplier), this.modify, this.custom, this.configOverride);
    }

    public DamageOverride thenAdd(double modify) {
        return new DamageOverride(this.multiplier, List.of(modify), this.custom, this.configOverride);
    }

    public DamageOverride thenOverride(DamageTypeProperties properties) {
        return new DamageOverride(this.multiplier, this.modify, properties, this.configOverride);
    }

    /** Chain a config override onto this override. */
    public DamageOverride withConfig(Object config) {
        return new DamageOverride(this.multiplier, this.modify, this.custom, config);
    }

    // ===========================
    // PRESETS
    // ===========================

    /** Double damage (2x multiplier) */
    public static final DamageOverride DOUBLE_DAMAGE = mult(2.0);

    /** Half damage (0.5x multiplier) */
    public static final DamageOverride HALF_DAMAGE = mult(0.5);

    /** No damage (0x multiplier) */
    public static final DamageOverride NO_DAMAGE = mult(0.0);

    /** Damage type disabled */
    public static final DamageOverride DISABLED = disabled();

    // ===========================
    // ConfigTagWrapper INTERFACE
    // ===========================

    @Override
    public List<Double> getMultiplier() { return multiplier; }

    @Override
    public List<Double> getModify() { return modify; }

    @Override
    public DamageTypeProperties getCustom() { return custom; }
}

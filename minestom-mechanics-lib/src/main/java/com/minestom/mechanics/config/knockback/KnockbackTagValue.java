package com.minestom.mechanics.config.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.util.ConfigTagWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Combined tag value for knockback modifications - RECORD VERSION.
 * Works with Tag.Structure() for proper serialization.
 *
 * Usage:
 * <pre>
 * import static KnockbackTagValue.*;
 *
 * // Just multiplier
 * .withTag(KnockbackSystem.CUSTOM, kbMult(2.0, 1.5))
 *
 * // Just modify
 * .withTag(KnockbackSystem.CUSTOM, kbAdd(0.5, 0.3))
 *
 * // Combined (chainable)
 * .withTag(KnockbackSystem.CUSTOM, kbMult(2.0, 1.5).thenAdd(0.5, 0.3))
 *
 * // Presets
 * .withTag(KnockbackSystem.CUSTOM, KB_HEAVY)
 * </pre>
 */
public record KnockbackTagValue(
        @Nullable List<Double> multiplier,
        @Nullable List<Double> modify,
        @Nullable KnockbackConfig custom
) implements ConfigTagWrapper<KnockbackConfig> {

    // ===========================
    // STATIC FACTORY METHODS
    // ===========================

    /**
     * Multiplier for all 6 components: [h, v, sprintH, sprintV, airH, airV]
     */
    public static KnockbackTagValue kbMult(double h, double v, double sprintH, double sprintV, double airH, double airV) {
        return new KnockbackTagValue(List.of(h, v, sprintH, sprintV, airH, airV), null, null);
    }

    /** Multiplier for horizontal and vertical (others = 1.0) */
    public static KnockbackTagValue kbMult(double horizontal, double vertical) {
        return kbMult(horizontal, vertical, 1.0, 1.0, 1.0, 1.0);
    }

    /** Uniform multiplier */
    public static KnockbackTagValue kbMult(double scale) {
        return kbMult(scale, scale, scale, scale, scale, scale);
    }

    /**
     * Modify all 6 components: [h, v, sprintH, sprintV, airH, airV]
     */
    public static KnockbackTagValue kbAdd(double h, double v, double sprintH, double sprintV, double airH, double airV) {
        return new KnockbackTagValue(null, List.of(h, v, sprintH, sprintV, airH, airV), null);
    }

    /** Modify horizontal and vertical (others = 0) */
    public static KnockbackTagValue kbAdd(double horizontal, double vertical) {
        return kbAdd(horizontal, vertical, 0, 0, 0, 0);
    }

    /** Full config override */
    public static KnockbackTagValue kbSet(KnockbackConfig config) {
        return new KnockbackTagValue(null, null, config);
    }

    // ===========================
    // CHAINABLE METHODS (return new records)
    // ===========================

    /** Chain: add multiplier (returns new record) */
    public KnockbackTagValue thenMult(double h, double v, double sprintH, double sprintV, double airH, double airV) {
        return new KnockbackTagValue(List.of(h, v, sprintH, sprintV, airH, airV), this.modify, this.custom);
    }

    public KnockbackTagValue thenMult(double horizontal, double vertical) {
        return thenMult(horizontal, vertical, 1.0, 1.0, 1.0, 1.0);
    }

    /** Chain: add modify (returns new record) */
    public KnockbackTagValue thenAdd(double h, double v, double sprintH, double sprintV, double airH, double airV) {
        return new KnockbackTagValue(this.multiplier, List.of(h, v, sprintH, sprintV, airH, airV), this.custom);
    }

    public KnockbackTagValue thenAdd(double horizontal, double vertical) {
        return thenAdd(horizontal, vertical, 0, 0, 0, 0);
    }

    /** Chain: set full config (returns new record) */
    public KnockbackTagValue thenSet(KnockbackConfig config) {
        return new KnockbackTagValue(this.multiplier, this.modify, config);
    }

    // ===========================
    // COMMON PRESETS
    // ===========================

    /** Heavy knockback: 2x horizontal, 1.5x vertical */
    public static final KnockbackTagValue KB_HEAVY = kbMult(2.0, 1.5);

    /** Light knockback: 0.5x all */
    public static final KnockbackTagValue KB_LIGHT = kbMult(0.5);

    /** No knockback */
    public static final KnockbackTagValue KB_NONE = kbMult(0.0);

    /** Grapple hook: negative horizontal = pull toward source */
    public static final KnockbackTagValue KB_GRAPPLE = kbMult(-1.5, 1.0);

    /** Blocking: 40% of normal knockback */
    public static final KnockbackTagValue KB_BLOCKING = kbMult(0.4, 0.4, 0.0, 0.0, 1.0, 1.0);

    /** Launcher: huge vertical, low horizontal */
    public static final KnockbackTagValue KB_LAUNCHER = kbMult(0.5, 3.0);

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
    public KnockbackConfig getCustom() {
        return custom;
    }
}
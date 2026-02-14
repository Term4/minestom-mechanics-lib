package com.minestom.mechanics.systems.health.damage.util;

import com.minestom.mechanics.systems.ConfigTagWrapper;
import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Runtime damage type override. Used on entities/worlds via transient tags,
 * on items via {@code Tag.Structure} with {@link DamageOverrideSerializer}.
 *
 * <p><b>Override resolution:</b> Item &gt; Attacker &gt; Player &gt; World &gt; Server Default</p>
 *
 * <pre>
 * // Entity/world (transient)
 * player.setTag(HealthSystem.tag("fire"), DamageOverride.mult(2.0));
 *
 * // Item (serialized)
 * item.withTag(DamageType.get("melee").getItemTag(), DamageOverride.override(props))
 * </pre>
 */
public record DamageOverride(
        @Nullable List<Double> multiplier,
        @Nullable List<Double> modify,
        @Nullable DamageTypeProperties custom,
        @Nullable Object configOverride
) implements ConfigTagWrapper<DamageTypeProperties> {

    public DamageOverride(@Nullable List<Double> multiplier, @Nullable List<Double> modify, @Nullable DamageTypeProperties custom) {
        this(multiplier, modify, custom, null);
    }

    public static DamageOverride mult(double multiplier) { return new DamageOverride(List.of(multiplier), null, null, null); }
    public static DamageOverride add(double modify) { return new DamageOverride(null, List.of(modify), null, null); }
    public static DamageOverride override(DamageTypeProperties properties) { return new DamageOverride(null, null, properties, null); }
    public static DamageOverride config(Object config) { return new DamageOverride(null, null, null, config); }
    public static DamageOverride disabled() { return override(DamageTypeProperties.DISABLED); }

    public DamageOverride thenMult(double multiplier) { return new DamageOverride(List.of(multiplier), this.modify, this.custom, this.configOverride); }
    public DamageOverride thenAdd(double modify) { return new DamageOverride(this.multiplier, List.of(modify), this.custom, this.configOverride); }
    public DamageOverride thenOverride(DamageTypeProperties properties) { return new DamageOverride(this.multiplier, this.modify, properties, this.configOverride); }
    public DamageOverride withConfig(Object config) { return new DamageOverride(this.multiplier, this.modify, this.custom, config); }

    public static final DamageOverride DOUBLE_DAMAGE = mult(2.0);
    public static final DamageOverride HALF_DAMAGE = mult(0.5);
    public static final DamageOverride NO_DAMAGE = mult(0.0);
    public static final DamageOverride DISABLED = disabled();

    @Override public List<Double> getMultiplier() { return multiplier; }
    @Override public List<Double> getModify() { return modify; }
    @Override public DamageTypeProperties getCustom() { return custom; }
}

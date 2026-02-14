package com.minestom.mechanics.systems.knockback.tags;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.systems.knockback.KnockbackSystem;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializer for {@link KnockbackTagValue} on items.
 */
public class KnockbackTagSerializer implements TagSerializer<KnockbackTagValue> {

    /** Legacy ATTACKER_LOOK mapped to ATTACKER_POSITION; use lookWeight=1 for equivalent effect. */
    private static KnockbackSystem.KnockbackDirectionMode parseDirectionMode(String name, KnockbackSystem.KnockbackDirectionMode defaultValue) {
        if (name == null) return defaultValue;
        if ("ATTACKER_LOOK".equals(name)) return KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION;
        if ("SHOOTER_CURRENT".equals(name)) return KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION; // removed, same as ATTACKER_POSITION
        try {
            return KnockbackSystem.KnockbackDirectionMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private static KnockbackSystem.DegenerateFallback parseDegenerateFallback(String name, KnockbackSystem.DegenerateFallback defaultValue) {
        if (name == null) return defaultValue;
        if ("PROXIMITY_SCALE".equals(name)) return defaultValue; // removed, treat as LOOK
        try {
            return KnockbackSystem.DegenerateFallback.valueOf(name);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private static KnockbackSystem.DirectionBlendMode parseDirectionBlendMode(String name, KnockbackSystem.DirectionBlendMode defaultValue) {
        if (name == null) return defaultValue;
        try {
            return KnockbackSystem.DirectionBlendMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private static KnockbackSystem.VelocityApplyMode parseVelocityApplyMode(String name, KnockbackSystem.VelocityApplyMode defaultValue) {
        if (name == null) return defaultValue;
        switch (name) {
            case "FRICTION", "ZERO" -> { return KnockbackSystem.VelocityApplyMode.SET; }
            case "ZERO_ADD" -> { return KnockbackSystem.VelocityApplyMode.ADD; }
            default -> {
                try {
                    return KnockbackSystem.VelocityApplyMode.valueOf(name);
                } catch (IllegalArgumentException e) {
                    return defaultValue;
                }
            }
        }
    }

    private static final String PREFIX_GROUND = "sog_";
    private static final String PREFIX_AIR = "sia_";
    private static final String PREFIX_FALLING = "sfl_";

    private static Map<KnockbackSystem.KnockbackVictimState, KnockbackSystem.KnockbackStateOverride> parseStateOverrides(TagReadable r) {
        Map<KnockbackSystem.KnockbackVictimState, KnockbackSystem.KnockbackStateOverride> out = new HashMap<>();
        addStateOverride(r, out, KnockbackSystem.KnockbackVictimState.ON_GROUND, PREFIX_GROUND);
        addStateOverride(r, out, KnockbackSystem.KnockbackVictimState.IN_AIR, PREFIX_AIR);
        addStateOverride(r, out, KnockbackSystem.KnockbackVictimState.FALLING, PREFIX_FALLING);
        return out.isEmpty() ? Map.of() : out;
    }

    private static void addStateOverride(TagReadable r, Map<KnockbackSystem.KnockbackVictimState, KnockbackSystem.KnockbackStateOverride> out,
                                         KnockbackSystem.KnockbackVictimState state, String prefix) {
        Double hf = r.getTag(Tag.Double(prefix + "hf"));
        Double vf = r.getTag(Tag.Double(prefix + "vf"));
        KnockbackSystem.VelocityApplyMode vam = parseVelocityApplyMode(r.getTag(Tag.String(prefix + "vam")), null);
        Double hm = r.getTag(Tag.Double(prefix + "hm"));
        Double vm = r.getTag(Tag.Double(prefix + "vm"));
        if (hf != null || vf != null || vam != null || hm != null || vm != null) {
            out.put(state, new KnockbackSystem.KnockbackStateOverride(hf, vf, vam, hm, vm, null));
        }
    }

    @Override
    public KnockbackTagValue read(@NotNull TagReadable r) {
        List<Double> mult = r.getTag(Tag.Double("m").list());
        List<Double> mod = r.getTag(Tag.Double("d").list());
        KnockbackConfig custom = null;
        if (Boolean.TRUE.equals(r.getTag(Tag.Boolean("hc")))) {
            // Read direction modes (default to standard if not present)
            String meleeDir = r.getTag(Tag.String("cmd_m"));
            String projDir = r.getTag(Tag.String("cmd_p"));
            KnockbackSystem.KnockbackDirectionMode melee = parseDirectionMode(meleeDir, KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION);
            KnockbackSystem.KnockbackDirectionMode proj = parseDirectionMode(projDir, KnockbackSystem.KnockbackDirectionMode.SHOOTER_ORIGIN);
            KnockbackSystem.DegenerateFallback df = parseDegenerateFallback(r.getTag(Tag.String("cdf")), KnockbackSystem.DegenerateFallback.LOOK);
            KnockbackSystem.DirectionBlendMode dbm = parseDirectionBlendMode(r.getTag(Tag.String("cdbm")), KnockbackSystem.DirectionBlendMode.BLEND_DIRECTION);
            Double slw = r.getTag(Tag.Double("cslw"));
            Double chf = r.getTag(Tag.Double("chf"));
            Double cvf = r.getTag(Tag.Double("cvf"));
            KnockbackSystem.VelocityApplyMode vam = parseVelocityApplyMode(r.getTag(Tag.String("cvam")), KnockbackSystem.VelocityApplyMode.SET);

            Map<KnockbackSystem.KnockbackVictimState, KnockbackSystem.KnockbackStateOverride> stateOverrides = parseStateOverrides(r);
            Double csrd = r.getTag(Tag.Double("csrd"));  // legacy: same for both axes
            Double csrdh = r.getTag(Tag.Double("csrdh"));
            Double csrdv = r.getTag(Tag.Double("csrdv"));
            Double crfh = r.getTag(Tag.Double("crfh"));
            Double crfv = r.getTag(Tag.Double("crfv"));
            Double cmrh = r.getTag(Tag.Double("cmrh"));
            Double cmrv = r.getTag(Tag.Double("cmrv"));
            // Sprint range (if absent, use base)
            Double scsrdh = r.getTag(Tag.Double("scsrdh"));
            Double scsrdv = r.getTag(Tag.Double("scsrdv"));
            Double scrfh = r.getTag(Tag.Double("scrfh"));
            Double scrfv = r.getTag(Tag.Double("scrfv"));
            Double scmrh = r.getTag(Tag.Double("scmrh"));
            Double scmrv = r.getTag(Tag.Double("scmrv"));

            double startH = csrdh != null ? csrdh : (csrd != null ? csrd : 0);
            double startV = csrdv != null ? csrdv : (csrd != null ? csrd : 0);
            double maxH = cmrh != null ? cmrh : Double.POSITIVE_INFINITY;
            double maxV = cmrv != null ? cmrv : Double.POSITIVE_INFINITY;
            KnockbackSystem.RangeReductionConfig baseRange = new KnockbackSystem.RangeReductionConfig(startH, startV, crfh != null ? crfh : 0, crfv != null ? crfv : 0, maxH, maxV);
            double sStartH = scsrdh != null ? scsrdh : startH;
            double sStartV = scsrdv != null ? scsrdv : startV;
            double sFactorH = scrfh != null ? scrfh : (crfh != null ? crfh : 0);
            double sFactorV = scrfv != null ? scrfv : (crfv != null ? crfv : 0);
            double sMaxH = scmrh != null ? scmrh : maxH;
            double sMaxV = scmrv != null ? scmrv : maxV;
            KnockbackSystem.RangeReductionConfig sprintRange = new KnockbackSystem.RangeReductionConfig(sStartH, sStartV, sFactorH, sFactorV, sMaxH, sMaxV);
            Integer csbt = r.getTag(Tag.Integer("csbt"));

            custom = new KnockbackConfig(
                    r.getTag(Tag.Double("ch")), r.getTag(Tag.Double("cv")),
                    r.getTag(Tag.Double("cvl")), r.getTag(Tag.Double("csh")),
                    r.getTag(Tag.Double("csv")), r.getTag(Tag.Double("cah")),
                    r.getTag(Tag.Double("cav")), r.getTag(Tag.Double("clw")),
                    r.getTag(Tag.Boolean("cmd")), r.getTag(Tag.Boolean("csn")),
                    melee, proj, df, dbm, slw,
                    chf != null ? chf : 2.0, cvf != null ? cvf : 2.0, null, null, vam,
                    stateOverrides,
                    baseRange, sprintRange,
                    csbt != null ? csbt : 0
            );
        }
        if (mult == null && mod == null && custom == null) return null;
        return new KnockbackTagValue(mult, mod, custom);
    }

    @Override
    public void write(@NotNull TagWritable w, @NotNull KnockbackTagValue v) {
        if (v.multiplier() != null) w.setTag(Tag.Double("m").list(), v.multiplier());
        if (v.modify() != null) w.setTag(Tag.Double("d").list(), v.modify());
        if (v.custom() != null) {
            w.setTag(Tag.Boolean("hc"), true);
            KnockbackConfig c = v.custom();
            w.setTag(Tag.Double("ch"), c.horizontal());
            w.setTag(Tag.Double("cv"), c.vertical());
            w.setTag(Tag.Double("cvl"), c.verticalLimit());
            w.setTag(Tag.Double("csh"), c.sprintBonusHorizontal());
            w.setTag(Tag.Double("csv"), c.sprintBonusVertical());
            w.setTag(Tag.Double("cah"), c.airMultiplierHorizontal());
            w.setTag(Tag.Double("cav"), c.airMultiplierVertical());
            w.setTag(Tag.Double("clw"), c.lookWeight());
            w.setTag(Tag.Boolean("cmd"), c.modern());
            w.setTag(Tag.Boolean("csn"), c.knockbackSyncSupported());
            // Direction modes
            if (c.meleeDirection() != KnockbackSystem.KnockbackDirectionMode.ATTACKER_POSITION) {
                w.setTag(Tag.String("cmd_m"), c.meleeDirection().name());
            }
            if (c.projectileDirection() != KnockbackSystem.KnockbackDirectionMode.SHOOTER_ORIGIN) {
                w.setTag(Tag.String("cmd_p"), c.projectileDirection().name());
            }
            if (c.degenerateFallback() != KnockbackSystem.DegenerateFallback.LOOK) {
                w.setTag(Tag.String("cdf"), c.degenerateFallback().name());
            }
            if (c.directionBlendMode() != KnockbackSystem.DirectionBlendMode.BLEND_DIRECTION) {
                w.setTag(Tag.String("cdbm"), c.directionBlendMode().name());
            }
            if (c.sprintLookWeight() != null) {
                w.setTag(Tag.Double("cslw"), c.sprintLookWeight());
            }
            if (c.horizontalFriction() != 2.0) {
                w.setTag(Tag.Double("chf"), c.horizontalFriction());
            }
            if (c.verticalFriction() != 2.0) {
                w.setTag(Tag.Double("cvf"), c.verticalFriction());
            }
            if (c.velocityApplyMode() != KnockbackSystem.VelocityApplyMode.SET) {
                w.setTag(Tag.String("cvam"), c.velocityApplyMode().name());
            }
            var baseR = c.rangeReduction();
            var sprintR = c.sprintRangeReduction();
            if (baseR.factorHorizontal() > 0 || baseR.factorVertical() > 0
                    || baseR.startDistanceHorizontal() > 0 || baseR.startDistanceVertical() > 0) {
                w.setTag(Tag.Double("csrdh"), baseR.startDistanceHorizontal());
                w.setTag(Tag.Double("csrdv"), baseR.startDistanceVertical());
                w.setTag(Tag.Double("crfh"), baseR.factorHorizontal());
                w.setTag(Tag.Double("crfv"), baseR.factorVertical());
                if (baseR.maxHorizontal() != Double.POSITIVE_INFINITY) w.setTag(Tag.Double("cmrh"), baseR.maxHorizontal());
                if (baseR.maxVertical() != Double.POSITIVE_INFINITY) w.setTag(Tag.Double("cmrv"), baseR.maxVertical());
            }
            if (sprintR.startDistanceHorizontal() != baseR.startDistanceHorizontal() || sprintR.startDistanceVertical() != baseR.startDistanceVertical()
                    || sprintR.factorHorizontal() != baseR.factorHorizontal() || sprintR.factorVertical() != baseR.factorVertical()
                    || sprintR.maxHorizontal() != baseR.maxHorizontal() || sprintR.maxVertical() != baseR.maxVertical()) {
                w.setTag(Tag.Double("scsrdh"), sprintR.startDistanceHorizontal());
                w.setTag(Tag.Double("scsrdv"), sprintR.startDistanceVertical());
                w.setTag(Tag.Double("scrfh"), sprintR.factorHorizontal());
                w.setTag(Tag.Double("scrfv"), sprintR.factorVertical());
                if (sprintR.maxHorizontal() != Double.POSITIVE_INFINITY) w.setTag(Tag.Double("scmrh"), sprintR.maxHorizontal());
                if (sprintR.maxVertical() != Double.POSITIVE_INFINITY) w.setTag(Tag.Double("scmrv"), sprintR.maxVertical());
            }
            if (c.sprintBufferTicks() > 0) {
                w.setTag(Tag.Integer("csbt"), c.sprintBufferTicks());
            }
            if (c.stateOverrides() != null && !c.stateOverrides().isEmpty()) {
                for (var e : c.stateOverrides().entrySet()) {
                    String p = switch (e.getKey()) {
                        case KnockbackSystem.KnockbackVictimState.ON_GROUND -> PREFIX_GROUND;
                        case KnockbackSystem.KnockbackVictimState.IN_AIR -> PREFIX_AIR;
                        case KnockbackSystem.KnockbackVictimState.FALLING -> PREFIX_FALLING;
                    };
                    var o = e.getValue();
                    if (o.horizontalFriction() != null) w.setTag(Tag.Double(p + "hf"), o.horizontalFriction());
                    if (o.verticalFriction() != null) w.setTag(Tag.Double(p + "vf"), o.verticalFriction());
                    if (o.velocityApplyMode() != null) w.setTag(Tag.String(p + "vam"), o.velocityApplyMode().name());
                    if (o.horizontalMultiplier() != null) w.setTag(Tag.Double(p + "hm"), o.horizontalMultiplier());
                    if (o.verticalMultiplier() != null) w.setTag(Tag.Double(p + "vm"), o.verticalMultiplier());
                }
            }
        }
    }
}

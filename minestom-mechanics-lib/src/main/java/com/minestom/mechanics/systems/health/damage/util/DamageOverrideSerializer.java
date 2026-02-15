package com.minestom.mechanics.systems.health.damage.util;

import com.minestom.mechanics.config.health.DamageTypeProperties;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Serializer for {@link DamageOverride} on items.
 * Stores multiplier, modify, and custom properties. configOverride is NOT serialized (entity-only).
 */
public class DamageOverrideSerializer implements TagSerializer<DamageOverride> {

    private static final Tag<Double> MULT = Tag.Double("m");
    private static final Tag<Double> MOD = Tag.Double("a");
    private static final Tag<Boolean> HC = Tag.Boolean("hc");
    private static final Tag<Boolean> CE = Tag.Boolean("ce");
    private static final Tag<Float> CM = Tag.Float("cm");
    private static final Tag<Boolean> CB = Tag.Boolean("cb");
    private static final Tag<Boolean> CP = Tag.Boolean("cp");
    private static final Tag<Boolean> CI = Tag.Boolean("ci");
    private static final Tag<Boolean> CC = Tag.Boolean("cc");
    private static final Tag<Boolean> CR = Tag.Boolean("cr");
    private static final Tag<Boolean> CK = Tag.Boolean("ck");
    private static final Tag<Float> RCO = Tag.Float("rco");
    private static final Tag<Boolean> HE = Tag.Boolean("he");
    private static final Tag<Boolean> AOR = Tag.Boolean("aor"); // legacy: animationOnReplacement
    private static final Tag<Integer> CIB = Tag.Integer("cib");
    private static final Tag<Boolean> SIS = Tag.Boolean("sis"); // noReplacementSameItem

    @Override
    public DamageOverride read(@NotNull TagReadable r) {
        Double mult = r.getTag(MULT);
        Double mod = r.getTag(MOD);
        DamageTypeProperties custom = null;
        if (Boolean.TRUE.equals(r.getTag(HC))) {
            Integer ib = r.getTag(CIB);
            Float rco = r.getTag(RCO);
            Boolean he = r.getTag(HE);
            if (he == null) he = r.getTag(AOR); // backward compat
            Boolean sis = r.getTag(SIS);
            custom = new DamageTypeProperties(
                    Boolean.TRUE.equals(r.getTag(CE)),
                    r.getTag(CM) != null ? r.getTag(CM) : 1.0f,
                    Boolean.TRUE.equals(r.getTag(CB)),
                    Boolean.TRUE.equals(r.getTag(CP)),
                    Boolean.TRUE.equals(r.getTag(CI)),
                    Boolean.TRUE.equals(r.getTag(CC)),
                    Boolean.TRUE.equals(r.getTag(CR)),
                    Boolean.TRUE.equals(r.getTag(CK)),
                    rco != null ? rco : 0f,
                    Boolean.TRUE.equals(he),
                    ib != null ? ib : 0,
                    Boolean.TRUE.equals(sis)
            );
        }
        return new DamageOverride(
                mult != null ? List.of(mult) : null,
                mod != null ? List.of(mod) : null,
                custom, null
        );
    }

    @Override
    public void write(@NotNull TagWritable w, @NotNull DamageOverride v) {
        if (v.multiplier() != null && !v.multiplier().isEmpty()) w.setTag(MULT, v.multiplier().get(0));
        if (v.modify() != null && !v.modify().isEmpty()) w.setTag(MOD, v.modify().get(0));
        if (v.custom() != null) {
            w.setTag(HC, true);
            DamageTypeProperties p = v.custom();
            w.setTag(CE, p.enabled());
            w.setTag(CM, p.multiplier());
            w.setTag(CB, p.blockable());
            w.setTag(CP, p.penetratesArmor());
            w.setTag(CI, p.bypassInvulnerability());
            w.setTag(CC, p.bypassCreative());
            w.setTag(CR, p.damageReplacement());
            w.setTag(CK, p.knockbackOnReplacement());
            if (p.replacementCutoff() > 0) w.setTag(RCO, p.replacementCutoff());
            w.setTag(HE, p.hurtEffect());
            if (p.invulnerabilityBufferTicks() > 0) w.setTag(CIB, p.invulnerabilityBufferTicks());
            if (p.noReplacementSameItem()) w.setTag(SIS, true);
        }
    }
}

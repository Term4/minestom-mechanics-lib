package com.minestom.mechanics.systems.knockback.tags;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Serializer for {@link KnockbackTagValue} on items.
 */
public class KnockbackTagSerializer implements TagSerializer<KnockbackTagValue> {

    @Override
    public KnockbackTagValue read(@NotNull TagReadable r) {
        List<Double> mult = r.getTag(Tag.Double("m").list());
        List<Double> mod = r.getTag(Tag.Double("d").list());
        KnockbackConfig custom = null;
        if (Boolean.TRUE.equals(r.getTag(Tag.Boolean("hc")))) {
            custom = new KnockbackConfig(
                    r.getTag(Tag.Double("ch")), r.getTag(Tag.Double("cv")),
                    r.getTag(Tag.Double("cvl")), r.getTag(Tag.Double("csh")),
                    r.getTag(Tag.Double("csv")), r.getTag(Tag.Double("cah")),
                    r.getTag(Tag.Double("cav")), r.getTag(Tag.Double("clw")),
                    r.getTag(Tag.Boolean("cmd")), r.getTag(Tag.Boolean("csn"))
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
        }
    }
}

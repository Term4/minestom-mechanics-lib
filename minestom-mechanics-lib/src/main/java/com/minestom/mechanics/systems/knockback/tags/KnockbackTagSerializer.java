package com.minestom.mechanics.systems.knockback.tags;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.systems.knockback.KnockbackSystem;
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
            Double slw = r.getTag(Tag.Double("cslw"));

            custom = new KnockbackConfig(
                    r.getTag(Tag.Double("ch")), r.getTag(Tag.Double("cv")),
                    r.getTag(Tag.Double("cvl")), r.getTag(Tag.Double("csh")),
                    r.getTag(Tag.Double("csv")), r.getTag(Tag.Double("cah")),
                    r.getTag(Tag.Double("cav")), r.getTag(Tag.Double("clw")),
                    r.getTag(Tag.Boolean("cmd")), r.getTag(Tag.Boolean("csn")),
                    melee, proj, df, slw
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
            if (c.sprintLookWeight() != null) {
                w.setTag(Tag.Double("cslw"), c.sprintLookWeight());
            }
        }
    }
}

package com.minestom.mechanics.systems.knockback.tags;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Serializer for KnockbackTagValue - handles {@code List<Double>} and nested KnockbackConfig.
 */
public class KnockbackTagSerializer implements TagSerializer<KnockbackTagValue> {

    // Tag definitions for nested fields
    private static final String MULT_KEY = "m";
    private static final String MOD_KEY = "d";
    private static final String HAS_CUSTOM_KEY = "has_custom";

    // Custom config field keys (shortened for efficiency)
    private static final String C_H = "ch";   // horizontal
    private static final String C_V = "cv";   // vertical
    private static final String C_VL = "cvl"; // verticalLimit
    private static final String C_SH = "csh"; // sprintBonusHorizontal
    private static final String C_SV = "csv"; // sprintBonusVertical
    private static final String C_AH = "cah"; // airMultiplierHorizontal
    private static final String C_AV = "cav"; // airMultiplierVertical
    private static final String C_LW = "clw"; // lookWeight
    private static final String C_MOD = "cmd"; // modern
    private static final String C_SYNC = "csn"; // knockbackSyncSupported

    @Override
    public KnockbackTagValue read(@NotNull TagReadable reader) {
        // Read multiplier list
        List<Double> multiplier = reader.getTag(Tag.Double(MULT_KEY).list());

        // Read modify list
        List<Double> modify = reader.getTag(Tag.Double(MOD_KEY).list());

        // Read custom config if present
        KnockbackConfig custom = null;
        Boolean hasCustom = reader.getTag(Tag.Boolean(HAS_CUSTOM_KEY));
        if (Boolean.TRUE.equals(hasCustom)) {
            custom = new KnockbackConfig(
                    reader.getTag(Tag.Double(C_H)),
                    reader.getTag(Tag.Double(C_V)),
                    reader.getTag(Tag.Double(C_VL)),
                    reader.getTag(Tag.Double(C_SH)),
                    reader.getTag(Tag.Double(C_SV)),
                    reader.getTag(Tag.Double(C_AH)),
                    reader.getTag(Tag.Double(C_AV)),
                    reader.getTag(Tag.Double(C_LW)),
                    reader.getTag(Tag.Boolean(C_MOD)),
                    reader.getTag(Tag.Boolean(C_SYNC))
            );
        }

        return new KnockbackTagValue(multiplier, modify, custom);
    }

    @Override
    public void write(@NotNull TagWritable writer, @NotNull KnockbackTagValue value) {
        // Write multiplier list
        if (value.multiplier() != null) {
            writer.setTag(Tag.Double(MULT_KEY).list(), value.multiplier());
        }

        // Write modify list
        if (value.modify() != null) {
            writer.setTag(Tag.Double(MOD_KEY).list(), value.modify());
        }

        // Write custom config if present
        if (value.custom() != null) {
            writer.setTag(Tag.Boolean(HAS_CUSTOM_KEY), true);
            KnockbackConfig c = value.custom();
            writer.setTag(Tag.Double(C_H), c.horizontal());
            writer.setTag(Tag.Double(C_V), c.vertical());
            writer.setTag(Tag.Double(C_VL), c.verticalLimit());
            writer.setTag(Tag.Double(C_SH), c.sprintBonusHorizontal());
            writer.setTag(Tag.Double(C_SV), c.sprintBonusVertical());
            writer.setTag(Tag.Double(C_AH), c.airMultiplierHorizontal());
            writer.setTag(Tag.Double(C_AV), c.airMultiplierVertical());
            writer.setTag(Tag.Double(C_LW), c.lookWeight());
            writer.setTag(Tag.Boolean(C_MOD), c.modern());
            writer.setTag(Tag.Boolean(C_SYNC), c.knockbackSyncSupported());
        }
    }
}
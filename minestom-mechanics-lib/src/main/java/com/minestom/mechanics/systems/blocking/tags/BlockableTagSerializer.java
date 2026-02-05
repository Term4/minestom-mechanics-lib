package com.minestom.mechanics.systems.blocking.tags;

import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

/**
 * Serializer for BlockableTagValue. Reads/writes applyLegacySlowdown and the three nullable double fields.
 */
public class BlockableTagSerializer implements TagSerializer<BlockableTagValue> {

    private static final String LEGACY_SLOW_KEY = "legacy_slow";
    private static final String DAMAGE_REDUCTION_KEY = "dr";
    private static final String KB_H_KEY = "kbh";
    private static final String KB_V_KEY = "kbv";
    private static final String PRESENT_KEY = "p";

    @Override
    public BlockableTagValue read(@NotNull TagReadable reader) {
        Boolean applyLegacySlowdown = reader.getTag(Tag.Boolean(LEGACY_SLOW_KEY));
        Double damageReduction = reader.getTag(Tag.Double(DAMAGE_REDUCTION_KEY));
        Double knockbackH = reader.getTag(Tag.Double(KB_H_KEY));
        Double knockbackV = reader.getTag(Tag.Double(KB_V_KEY));
        return new BlockableTagValue(applyLegacySlowdown, damageReduction, knockbackH, knockbackV);
    }

    @Override
    public void write(@NotNull TagWritable writer, @NotNull BlockableTagValue value) {
        writer.setTag(Tag.Boolean(PRESENT_KEY), true);
        if (value.applyLegacySlowdown() != null) {
            writer.setTag(Tag.Boolean(LEGACY_SLOW_KEY), value.applyLegacySlowdown());
        }
        if (value.damageReduction() != null) {
            writer.setTag(Tag.Double(DAMAGE_REDUCTION_KEY), value.damageReduction());
        }
        if (value.knockbackHMultiplier() != null) {
            writer.setTag(Tag.Double(KB_H_KEY), value.knockbackHMultiplier());
        }
        if (value.knockbackVMultiplier() != null) {
            writer.setTag(Tag.Double(KB_V_KEY), value.knockbackVMultiplier());
        }
    }
}

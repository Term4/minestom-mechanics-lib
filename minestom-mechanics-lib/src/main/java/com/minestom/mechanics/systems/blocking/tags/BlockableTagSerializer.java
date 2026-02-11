package com.minestom.mechanics.systems.blocking.tags;

import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

/**
 * Serializer for {@link BlockableTagValue} on items.
 */
public class BlockableTagSerializer implements TagSerializer<BlockableTagValue> {

    private static final Tag<Boolean> SLOW = Tag.Boolean("ls");
    private static final Tag<Double> DR = Tag.Double("dr");
    private static final Tag<Double> KBH = Tag.Double("kh");
    private static final Tag<Double> KBV = Tag.Double("kv");

    @Override
    public BlockableTagValue read(@NotNull TagReadable r) {
        Boolean slow = r.getTag(SLOW);
        Double dr = r.getTag(DR);
        Double kbh = r.getTag(KBH);
        Double kbv = r.getTag(KBV);
        if (slow == null && dr == null && kbh == null && kbv == null) return null;
        return new BlockableTagValue(slow, dr, kbh, kbv);
    }

    @Override
    public void write(@NotNull TagWritable w, @NotNull BlockableTagValue v) {
        if (v.applyLegacySlowdown() != null) w.setTag(SLOW, v.applyLegacySlowdown());
        if (v.damageReduction() != null) w.setTag(DR, v.damageReduction());
        if (v.knockbackHMultiplier() != null) w.setTag(KBH, v.knockbackHMultiplier());
        if (v.knockbackVMultiplier() != null) w.setTag(KBV, v.knockbackVMultiplier());
    }
}

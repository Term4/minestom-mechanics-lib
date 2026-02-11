package com.minestom.mechanics.systems.health.damage.util;

import com.minestom.mechanics.systems.health.damage.DamageTypeProperties;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Serializer for {@link DamageOverride} so it can be stored on items via {@code Tag.Structure}.
 * Serializes multiplier, modify, and custom properties. configOverride is NOT serialized
 * (it's type-erased and only meaningful on entity/world tags).
 */
public class DamageOverrideSerializer implements TagSerializer<DamageOverride> {

    // Multiplier/modify — stored as single double (most common case is one value)
    private static final Tag<Double> MULT = Tag.Double("m");
    private static final Tag<Double> MOD = Tag.Double("a");

    // Custom properties — each field stored individually
    private static final Tag<Boolean> C_ENABLED = Tag.Boolean("ce");
    private static final Tag<Float> C_MULTIPLIER = Tag.Float("cm");
    private static final Tag<Boolean> C_BLOCKABLE = Tag.Boolean("cb");
    private static final Tag<Boolean> C_PENETRATES = Tag.Boolean("cp");
    private static final Tag<Boolean> C_BYPASS_INVULN = Tag.Boolean("ci");
    private static final Tag<Boolean> C_BYPASS_CREATIVE = Tag.Boolean("cc");
    private static final Tag<Boolean> C_REPLACEMENT = Tag.Boolean("cr");
    private static final Tag<Boolean> C_KB_REPLACE = Tag.Boolean("ck");
    private static final Tag<Boolean> HAS_CUSTOM = Tag.Boolean("hc");

    @Override
    public DamageOverride read(@NotNull TagReadable reader) {
        Double mult = reader.getTag(MULT);
        Double mod = reader.getTag(MOD);
        List<Double> multList = mult != null ? List.of(mult) : null;
        List<Double> modList = mod != null ? List.of(mod) : null;

        DamageTypeProperties custom = null;
        Boolean hasCustom = reader.getTag(HAS_CUSTOM);
        if (Boolean.TRUE.equals(hasCustom)) {
            custom = new DamageTypeProperties(
                    Boolean.TRUE.equals(reader.getTag(C_ENABLED)),
                    reader.getTag(C_MULTIPLIER) != null ? reader.getTag(C_MULTIPLIER) : 1.0f,
                    Boolean.TRUE.equals(reader.getTag(C_BLOCKABLE)),
                    Boolean.TRUE.equals(reader.getTag(C_PENETRATES)),
                    Boolean.TRUE.equals(reader.getTag(C_BYPASS_INVULN)),
                    Boolean.TRUE.equals(reader.getTag(C_BYPASS_CREATIVE)),
                    Boolean.TRUE.equals(reader.getTag(C_REPLACEMENT)),
                    Boolean.TRUE.equals(reader.getTag(C_KB_REPLACE))
            );
        }

        return new DamageOverride(multList, modList, custom, null);
    }

    @Override
    public void write(@NotNull TagWritable writer, @NotNull DamageOverride value) {
        if (value.multiplier() != null && !value.multiplier().isEmpty()) {
            writer.setTag(MULT, value.multiplier().get(0));
        }
        if (value.modify() != null && !value.modify().isEmpty()) {
            writer.setTag(MOD, value.modify().get(0));
        }
        if (value.custom() != null) {
            writer.setTag(HAS_CUSTOM, true);
            DamageTypeProperties p = value.custom();
            writer.setTag(C_ENABLED, p.enabled());
            writer.setTag(C_MULTIPLIER, p.multiplier());
            writer.setTag(C_BLOCKABLE, p.blockable());
            writer.setTag(C_PENETRATES, p.penetratesArmor());
            writer.setTag(C_BYPASS_INVULN, p.bypassInvulnerability());
            writer.setTag(C_BYPASS_CREATIVE, p.bypassCreative());
            writer.setTag(C_REPLACEMENT, p.damageReplacement());
            writer.setTag(C_KB_REPLACE, p.knockbackOnReplacement());
        }
        // configOverride is NOT serialized — only works on entity/world transient tags
    }
}

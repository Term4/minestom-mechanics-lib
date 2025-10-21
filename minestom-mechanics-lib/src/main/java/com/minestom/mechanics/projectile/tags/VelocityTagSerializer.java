package com.minestom.mechanics.projectile.tags;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Serializer for VelocityTagValue - handles {@code List<Double>} and nested ProjectileVelocityConfig.
 */
public class VelocityTagSerializer implements TagSerializer<VelocityTagValue> {

    // Tag definitions for nested fields
    private static final String MULT_KEY = "m";
    private static final String MOD_KEY = "d";
    private static final String HAS_CUSTOM_KEY = "has_custom";

    // Custom config field keys (shortened for efficiency)
    private static final String C_HM = "chm";  // horizontalMultiplier
    private static final String C_VM = "cvm";  // verticalMultiplier
    private static final String C_SM = "csm";  // spreadMultiplier
    private static final String C_G = "cg";    // gravity
    private static final String C_HAR = "char"; // horizontalAirResistance
    private static final String C_VAR = "cvar"; // verticalAirResistance

    @Override
    public VelocityTagValue read(@NotNull TagReadable reader) {
        // Read multiplier list
        List<Double> multiplier = reader.getTag(Tag.Double(MULT_KEY).list());

        // Read modify list
        List<Double> modify = reader.getTag(Tag.Double(MOD_KEY).list());

        // Read custom config if present
        ProjectileVelocityConfig custom = null;
        Boolean hasCustom = reader.getTag(Tag.Boolean(HAS_CUSTOM_KEY));
        if (Boolean.TRUE.equals(hasCustom)) {
            custom = new ProjectileVelocityConfig(
                    reader.getTag(Tag.Double(C_HM)),
                    reader.getTag(Tag.Double(C_VM)),
                    reader.getTag(Tag.Double(C_SM)),
                    reader.getTag(Tag.Double(C_G)),
                    reader.getTag(Tag.Double(C_HAR)),
                    reader.getTag(Tag.Double(C_VAR))
            );
        }

        return new VelocityTagValue(multiplier, modify, custom);
    }

    @Override
    public void write(@NotNull TagWritable writer, @NotNull VelocityTagValue value) {
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
            ProjectileVelocityConfig c = value.custom();
            writer.setTag(Tag.Double(C_HM), c.horizontalMultiplier());
            writer.setTag(Tag.Double(C_VM), c.verticalMultiplier());
            writer.setTag(Tag.Double(C_SM), c.spreadMultiplier());
            writer.setTag(Tag.Double(C_G), c.gravity());
            writer.setTag(Tag.Double(C_HAR), c.horizontalAirResistance());
            writer.setTag(Tag.Double(C_VAR), c.verticalAirResistance());
        }
    }
}
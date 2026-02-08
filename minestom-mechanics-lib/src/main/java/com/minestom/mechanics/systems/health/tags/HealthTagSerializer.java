package com.minestom.mechanics.systems.health.tags;

import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Serializer for HealthTagWrapper
 * Handles MULTIPLIER, MODIFY, and CUSTOM fields
 */
public class HealthTagSerializer implements TagSerializer<HealthTagWrapper> {
    
    private static final String MULT_KEY = "m";
    private static final String MOD_KEY = "d";
    private static final String HAS_CUSTOM_KEY = "has_custom";
    private static final String C_MULT_KEY = "cm";
    private static final String C_MOD_KEY = "cd";
    private static final String C_ENABLED_KEY = "ce";
    private static final String C_BLOCKABLE_KEY = "cb";
    private static final String C_BYPASS_INVULN_KEY = "cbi";

    @Override
    public HealthTagWrapper read(@NotNull TagReadable reader) {
        // Read multiplier list
        List<Double> multiplier = reader.getTag(Tag.Double(MULT_KEY).list());

        // Read modify list
        List<Double> modify = reader.getTag(Tag.Double(MOD_KEY).list());

        // Read custom config if present
        HealthTagValue custom = null;
        Boolean hasCustom = reader.getTag(Tag.Boolean(HAS_CUSTOM_KEY));
        if (Boolean.TRUE.equals(hasCustom)) {
            Float cMult = reader.getTag(Tag.Float(C_MULT_KEY));
            Float cMod = reader.getTag(Tag.Float(C_MOD_KEY));
            Boolean cEnabled = reader.getTag(Tag.Boolean(C_ENABLED_KEY));
            Boolean cBlockable = reader.getTag(Tag.Boolean(C_BLOCKABLE_KEY));
            Boolean cBypassInvuln = reader.getTag(Tag.Boolean(C_BYPASS_INVULN_KEY));
            custom = new HealthTagValue(cMult, cMod, cEnabled, cBlockable, cBypassInvuln);
        }

        return new HealthTagWrapper(multiplier, modify, custom);
    }
    
    @Override
    public void write(@NotNull TagWritable writer, @NotNull HealthTagWrapper wrapper) {
        // Write multiplier list
        if (wrapper.multiplier() != null && !wrapper.multiplier().isEmpty()) {
            writer.setTag(Tag.Double(MULT_KEY).list(), wrapper.multiplier());
        }
        
        // Write modify list
        if (wrapper.modify() != null && !wrapper.modify().isEmpty()) {
            writer.setTag(Tag.Double(MOD_KEY).list(), wrapper.modify());
        }
        
        // Write custom config if present
        if (wrapper.custom() != null) {
            writer.setTag(Tag.Boolean(HAS_CUSTOM_KEY), true);
            HealthTagValue custom = wrapper.custom();
            if (custom.multiplier() != null) {
                writer.setTag(Tag.Float(C_MULT_KEY), custom.multiplier());
            }
            if (custom.modify() != null) {
                writer.setTag(Tag.Float(C_MOD_KEY), custom.modify());
            }
            if (custom.enabled() != null) {
                writer.setTag(Tag.Boolean(C_ENABLED_KEY), custom.enabled());
            }
            if (custom.blockable() != null) {
                writer.setTag(Tag.Boolean(C_BLOCKABLE_KEY), custom.blockable());
            }
            if (custom.bypassInvulnerability() != null) {
                writer.setTag(Tag.Boolean(C_BYPASS_INVULN_KEY), custom.bypassInvulnerability());
            }
        }
    }
}


package com.minestom.mechanics.systems.projectile.tags;

import com.minestom.mechanics.config.projectiles.advanced.ProjectileVelocityConfig;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Serializer for {@link VelocityTagValue} on items.
 */
public class VelocityTagSerializer implements TagSerializer<VelocityTagValue> {

    @Override
    public VelocityTagValue read(@NotNull TagReadable r) {
        List<Double> mult = r.getTag(Tag.Double("m").list());
        List<Double> mod = r.getTag(Tag.Double("d").list());
        ProjectileVelocityConfig custom = null;
        if (Boolean.TRUE.equals(r.getTag(Tag.Boolean("hc")))) {
            custom = new ProjectileVelocityConfig(
                    r.getTag(Tag.Double("chm")), r.getTag(Tag.Double("cvm")),
                    r.getTag(Tag.Double("csm")), r.getTag(Tag.Double("cg")),
                    r.getTag(Tag.Double("char")), r.getTag(Tag.Double("cvar"))
            );
        }
        if (mult == null && mod == null && custom == null) return null;
        return new VelocityTagValue(mult, mod, custom);
    }

    @Override
    public void write(@NotNull TagWritable w, @NotNull VelocityTagValue v) {
        if (v.multiplier() != null) w.setTag(Tag.Double("m").list(), v.multiplier());
        if (v.modify() != null) w.setTag(Tag.Double("d").list(), v.modify());
        if (v.custom() != null) {
            w.setTag(Tag.Boolean("hc"), true);
            ProjectileVelocityConfig c = v.custom();
            w.setTag(Tag.Double("chm"), c.horizontalMultiplier());
            w.setTag(Tag.Double("cvm"), c.verticalMultiplier());
            w.setTag(Tag.Double("csm"), c.spreadMultiplier());
            w.setTag(Tag.Double("cg"), c.gravity());
            w.setTag(Tag.Double("char"), c.horizontalAirResistance());
            w.setTag(Tag.Double("cvar"), c.verticalAirResistance());
        }
    }
}

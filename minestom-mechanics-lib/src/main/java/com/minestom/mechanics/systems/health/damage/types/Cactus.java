package com.minestom.mechanics.systems.health.damage.types;

import com.minestom.mechanics.systems.health.damage.DamageTracker;
import com.minestom.mechanics.config.health.DamageTypeProperties;
import com.minestom.mechanics.util.BlockContactUtil;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.registry.RegistryKey;

/**
 * Cactus damage. Detects cactus contact via AABB intersection.
 */
public final class Cactus extends DamageTracker {

    public record Config(float damage) {
        public static final Config DEFAULT = new Config(1.0f);
        public Config withDamage(float v) { return new Config(v); }
    }

    @Override public String id() { return "cactus"; }
    @Override public DamageTypeProperties defaultProperties() { return DamageTypeProperties.ENVIRONMENTAL_DEFAULT; }
    @Override public RegistryKey<?>[] matchedTypes() { return new RegistryKey<?>[]{ DamageType.CACTUS }; }
    @Override public Object defaultConfig() { return Config.DEFAULT; }

    @Override
    public void tick(Player player, long currentTick) {
        if (!damageType.isEnabled(player)) return;
        var instance = player.getInstance();
        if (instance == null) return;

        Config config = damageType.getConfig();
        if (BlockContactUtil.isTouching(instance, player, b -> b.compare(Block.CACTUS), BlockFace.BOTTOM)) {
            player.damage(new Damage(DamageType.CACTUS, null, null, player.getPosition(), config.damage()));
        }
    }
}

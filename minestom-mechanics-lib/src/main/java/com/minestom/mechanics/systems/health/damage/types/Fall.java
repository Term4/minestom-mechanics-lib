package com.minestom.mechanics.systems.health.damage.types;

import com.minestom.mechanics.systems.health.damage.DamageTracker;
import com.minestom.mechanics.config.health.DamageTypeProperties;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;

/**
 * Fall damage. Tracks fall distance per player and applies damage on landing.
 */
public final class Fall extends DamageTracker {

    public record Config(float safeFallDistance) {
        public static final Config DEFAULT = new Config(3.0f);
        public Config withSafeFallDistance(float v) { return new Config(v); }
    }

    private static final Tag<Double> FALL_DIST = Tag.Transient("fall_distance");
    private static final Tag<Boolean> WAS_GROUNDED = Tag.Transient("was_on_ground");
    private static final Tag<Double> LAST_Y = Tag.Transient("last_y_pos");

    @Override public String id() { return "fall"; }
    @Override public DamageTypeProperties defaultProperties() { return DamageTypeProperties.ENVIRONMENTAL_DEFAULT; }
    @Override public RegistryKey<?>[] matchedTypes() { return new RegistryKey<?>[]{ DamageType.FALL }; }
    @Override public Object defaultConfig() { return Config.DEFAULT; }

    @Override
    public void tick(Player player, long currentTick) {
        if (!damageType.isEnabled(player)) return;

        Config config = damageType.getConfig();
        Pos pos = player.getPosition();
        double currentY = pos.y();
        double lastY = tagOr(player, LAST_Y, 0.0);
        boolean grounded = player.isOnGround();
        boolean wasGrounded = tagOr(player, WAS_GROUNDED, true);

        if (currentY < lastY && !grounded) {
            player.setTag(FALL_DIST, tagOr(player, FALL_DIST, 0.0) + (lastY - currentY));
        }
        if (grounded && !wasGrounded) {
            double dist = tagOr(player, FALL_DIST, 0.0);
            if (dist > config.safeFallDistance()) {
                float dmg = (float) (dist - config.safeFallDistance());
                if (player.hasEffect(PotionEffect.SLOW_FALLING)) dmg = 0;
                if (dmg > 0) player.damage(new Damage(DamageType.FALL, null, null, pos, dmg));
            }
            player.setTag(FALL_DIST, 0.0);
        }
        player.setTag(LAST_Y, currentY);
        player.setTag(WAS_GROUNDED, grounded);
    }

    @Override public void onPlayerDeath(Player player) { resetFallDistance(player); }
    @Override public void onPlayerSpawn(Player player) { resetFallDistance(player); }
    @Override public void resetFallDistance(Player player) { player.setTag(FALL_DIST, 0.0); }
    @Override public double getFallDistance(Player player) { return tagOr(player, FALL_DIST, 0.0); }

    @Override
    public void cleanup(LivingEntity entity) {
        entity.removeTag(FALL_DIST);
        entity.removeTag(WAS_GROUNDED);
        entity.removeTag(LAST_Y);
    }

    private static <T> T tagOr(Player p, Tag<T> tag, T def) {
        T v = p.getTag(tag);
        return v != null ? v : def;
    }
}

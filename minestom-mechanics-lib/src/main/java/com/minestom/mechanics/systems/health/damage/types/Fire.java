package com.minestom.mechanics.systems.health.damage.types;

import com.minestom.mechanics.systems.health.damage.DamageTracker;
import com.minestom.mechanics.config.health.DamageTypeProperties;
import com.minestom.mechanics.config.timing.TickScaler;
import com.minestom.mechanics.config.timing.TickScalingConfig;
import com.minestom.mechanics.util.BlockContactUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;

/**
 * Fire damage. Detects fire/lava contact, manages burning state, applies damage at intervals.
 *
 * <p>Two ignition modes (configurable via {@link IgnitionMode}):</p>
 * <ul>
 *   <li><b>VANILLA</b> — Deals contact damage immediately on first tick in fire,
 *       but doesn't set the player on fire (burning) until the second damage tick.
 *       This matches vanilla Minecraft behavior.</li>
 *   <li><b>DELAYED</b> — No damage or ignition until {@code ignitionDelayTicks} have passed.
 *       Useful for servers that want fire to be less punishing on brief contact.</li>
 * </ul>
 *
 * <p>Lava always ignites and damages instantly regardless of mode.</p>
 *
 * <p>Config is tag-overridable via {@code DamageOverride.config(new Fire.Config(...))}.</p>
 */
public final class Fire extends DamageTracker {

    public enum IgnitionMode {
        /** Vanilla: instant contact damage, ignition on second damage tick. */
        VANILLA,
        /** Delayed: no damage until ignition delay passes, then damage + ignition together. */
        DELAYED
    }

    public record Config(
            float fireDamage,
            float lavaDamage,
            float onFireDamage,
            int contactDamageIntervalTicks,
            int burnDamageIntervalTicks,
            int burnDurationTicks,
            int ignitionDelayTicks,
            IgnitionMode ignitionMode
    ) {
        public static final Config DEFAULT = new Config(1.0f, 4.0f, 1.0f, 10, 20, 8 * 20, 10, IgnitionMode.VANILLA);
        public Config withFireDamage(float v) { return new Config(v, lavaDamage, onFireDamage, contactDamageIntervalTicks, burnDamageIntervalTicks, burnDurationTicks, ignitionDelayTicks, ignitionMode); }
        public Config withLavaDamage(float v) { return new Config(fireDamage, v, onFireDamage, contactDamageIntervalTicks, burnDamageIntervalTicks, burnDurationTicks, ignitionDelayTicks, ignitionMode); }
        public Config withOnFireDamage(float v) { return new Config(fireDamage, lavaDamage, v, contactDamageIntervalTicks, burnDamageIntervalTicks, burnDurationTicks, ignitionDelayTicks, ignitionMode); }
        public Config withContactDamageIntervalTicks(int v) { return new Config(fireDamage, lavaDamage, onFireDamage, v, burnDamageIntervalTicks, burnDurationTicks, ignitionDelayTicks, ignitionMode); }
        public Config withBurnDamageIntervalTicks(int v) { return new Config(fireDamage, lavaDamage, onFireDamage, contactDamageIntervalTicks, v, burnDurationTicks, ignitionDelayTicks, ignitionMode); }
        public Config withBurnDurationTicks(int v) { return new Config(fireDamage, lavaDamage, onFireDamage, contactDamageIntervalTicks, burnDamageIntervalTicks, v, ignitionDelayTicks, ignitionMode); }
        public Config withIgnitionDelayTicks(int v) { return new Config(fireDamage, lavaDamage, onFireDamage, contactDamageIntervalTicks, burnDamageIntervalTicks, burnDurationTicks, v, ignitionMode); }
        public Config withIgnitionMode(IgnitionMode v) { return new Config(fireDamage, lavaDamage, onFireDamage, contactDamageIntervalTicks, burnDamageIntervalTicks, burnDurationTicks, ignitionDelayTicks, v); }
    }

    // State tags on the player
    private static final Tag<Long> LAST_CONTACT_TICK = Tag.Transient("fire_last_contact_tick");
    private static final Tag<Long> LAST_BURN_TICK = Tag.Transient("fire_last_burn_tick");
    private static final Tag<Integer> FIRE_TICKS = Tag.Transient("health_fire_ticks");
    private static final Tag<Integer> CONTACT_DAMAGE_COUNT = Tag.Transient("fire_contact_damage_count");

    @Override public String id() { return "fire"; }
    @Override public DamageTypeProperties defaultProperties() { return DamageTypeProperties.ENVIRONMENTAL_DEFAULT; }
    @Override public RegistryKey<?>[] matchedTypes() {
        return new RegistryKey<?>[]{ DamageType.ON_FIRE, DamageType.IN_FIRE, DamageType.LAVA };
    }
    @Override public Object defaultConfig() { return Config.DEFAULT; }

    @Override
    public void tick(Player player, long currentTick) {
        if (!damageType.isEnabled(player)) return;
        var instance = player.getInstance();
        if (instance == null) return;

        Config config = damageType.resolveConfig(player);
        Pos pos = player.getPosition();

        boolean inLava = BlockContactUtil.isTouching(instance, player, Fire::isLava);
        boolean inFire = !inLava && BlockContactUtil.isTouching(instance, player, Fire::isFire);

        if (inLava) {
            handleLava(player, config, pos, currentTick);
        } else if (inFire) {
            handleFire(player, config, pos, currentTick);
        } else {
            handleBurning(player, config, pos, currentTick);
        }
    }

    private void handleLava(Player player, Config config, Pos pos, long currentTick) {
        var mode = TickScalingConfig.getMode();
        int scaledBurnDuration = TickScaler.scale(config.burnDurationTicks(), mode);
        int scaledContactInterval = TickScaler.scale(config.contactDamageIntervalTicks(), mode);
        player.removeTag(CONTACT_DAMAGE_COUNT);
        setFireTicks(player, Math.max(getFireTicks(player), scaledBurnDuration));
        if (intervalElapsed(player, LAST_CONTACT_TICK, currentTick, scaledContactInterval)) {
            player.damage(new Damage(DamageType.LAVA, null, null, pos, config.lavaDamage()));
        }
    }

    private void handleFire(Player player, Config config, Pos pos, long currentTick) {
        var mode = TickScalingConfig.getMode();
        int scaledContactInterval = TickScaler.scale(config.contactDamageIntervalTicks(), mode);
        int scaledBurnDuration = TickScaler.scale(config.burnDurationTicks(), mode);
        int scaledIgnitionDelay = TickScaler.scale(config.ignitionDelayTicks(), mode);
        if (config.ignitionMode() == IgnitionMode.VANILLA) {
            // VANILLA: deal damage immediately, ignite on 2nd damage tick
            if (intervalElapsed(player, LAST_CONTACT_TICK, currentTick, scaledContactInterval)) {
                player.damage(new Damage(DamageType.IN_FIRE, null, null, pos, config.fireDamage()));
                int count = tagOr(player, CONTACT_DAMAGE_COUNT, 0) + 1;
                player.setTag(CONTACT_DAMAGE_COUNT, count);

                if (count >= 2) {
                    setFireTicks(player, Math.max(getFireTicks(player), scaledBurnDuration));
                }
            }
        } else {
            // DELAYED: no damage until ignition delay passes
            int count = tagOr(player, CONTACT_DAMAGE_COUNT, 0) + 1;
            player.setTag(CONTACT_DAMAGE_COUNT, count);

            if (count >= scaledIgnitionDelay) {
                setFireTicks(player, Math.max(getFireTicks(player), scaledBurnDuration));
                if (intervalElapsed(player, LAST_CONTACT_TICK, currentTick, scaledContactInterval)) {
                    player.damage(new Damage(DamageType.IN_FIRE, null, null, pos, config.fireDamage()));
                }
            }
        }
    }

    private void handleBurning(Player player, Config config, Pos pos, long currentTick) {
        player.removeTag(CONTACT_DAMAGE_COUNT);
        player.removeTag(LAST_CONTACT_TICK);

        int ticks = getFireTicks(player);
        if (ticks > 0) {
            setFireTicks(player, ticks - 1);
            int scaledBurnInterval = TickScaler.scale(config.burnDamageIntervalTicks(), TickScalingConfig.getMode());
            if (intervalElapsed(player, LAST_BURN_TICK, currentTick, scaledBurnInterval)) {
                player.damage(new Damage(DamageType.ON_FIRE, null, null, pos, config.onFireDamage()));
            }
        } else {
            player.removeTag(LAST_BURN_TICK);
        }
    }

    @Override
    public void onPlayerDeath(Player player) {
        setFireTicks(player, 0);
        player.removeTag(CONTACT_DAMAGE_COUNT);
        player.removeTag(LAST_CONTACT_TICK);
        player.removeTag(LAST_BURN_TICK);
    }

    @Override
    public void cleanup(LivingEntity entity) {
        entity.removeTag(LAST_CONTACT_TICK);
        entity.removeTag(LAST_BURN_TICK);
        entity.removeTag(FIRE_TICKS);
        entity.removeTag(CONTACT_DAMAGE_COUNT);
        if (entity.getEntityMeta() instanceof LivingEntityMeta meta) meta.setOnFire(false);
    }

    // ===========================
    // HELPERS
    // ===========================

    private boolean intervalElapsed(Player player, Tag<Long> tag, long currentTick, int interval) {
        long last = tagOr(player, tag, -1L);
        if (last < 0 || currentTick - last >= interval) {
            player.setTag(tag, currentTick);
            return true;
        }
        return false;
    }

    private static int getFireTicks(Player player) { return tagOr(player, FIRE_TICKS, 0); }

    private static void setFireTicks(Player player, int ticks) {
        player.setTag(FIRE_TICKS, Math.max(0, ticks));
        if (player.getEntityMeta() instanceof LivingEntityMeta meta) meta.setOnFire(ticks > 0);
    }

    private static boolean isFire(Block b) {
        if (b.compare(Block.FIRE) || b.compare(Block.SOUL_FIRE)) return true;
        String n = b.name();
        return n != null && n.toLowerCase().contains("fire");
    }

    private static boolean isLava(Block b) {
        if (b.compare(Block.LAVA)) return true;
        String n = b.name();
        return n != null && n.toLowerCase().contains("lava");
    }

    private static <T> T tagOr(Player p, Tag<T> tag, T def) {
        T v = p.getTag(tag);
        return v != null ? v : def;
    }
}

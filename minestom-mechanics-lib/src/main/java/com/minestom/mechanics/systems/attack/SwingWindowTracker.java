package com.minestom.mechanics.systems.attack;

import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks attacker-victim relationships for the swing hit window using tags on the attacker.
 * When an attacker hits a victim (melee or projectile only — not swing-window hits), the victim
 * is stored on the attacker. For the next N ticks, swings that ray-intersect that victim count
 * as attacks. Swing-window hits do not add victims (only the "initial" melee/projectile hit does).
 *
 * <p>Supports multiple victims when two hits land in the same tick (e.g. melee + projectile).</p>
 * <p>Tags auto-clear when the player disconnects.</p>
 */
public final class SwingWindowTracker {

    private record VictimMap(HashMap<UUID, Long> data) {
        VictimMap() { this(new HashMap<>()); }
    }
    private static final Tag<VictimMap> VICTIMS = Tag.Transient("attack_swing_window");
    private static final Tag<Long> LAST_SWING_TICK = Tag.Transient("attack_last_swing_tick");
    private static final Tag<Long> LAST_CONSUMED_SWING = Tag.Transient("attack_last_consumed_swing");

    private SwingWindowTracker() {}

    /**
     * Record that attacker swung at the given tick. Used for look-check window (poll look for N ticks after swing).
     */
    public static void recordSwing(Player attacker, long tick) {
        if (attacker == null) return;
        attacker.setTag(LAST_SWING_TICK, tick);
    }

    /**
     * Whether attacker has an unconsumed swing (swung recently, haven't yet processed a window-hit from this swing).
     */
    public static boolean hasUnconsumedSwing(Player attacker, long currentTick, int lookCheckTicks) {
        if (attacker == null || lookCheckTicks <= 0) return false;
        Long lastSwing = attacker.getTag(LAST_SWING_TICK);
        Long lastConsumed = attacker.getTag(LAST_CONSUMED_SWING);
        if (lastSwing == null) return false;
        if (lastConsumed != null && lastSwing <= lastConsumed) return false; // swing already consumed
        return currentTick - lastSwing <= lookCheckTicks;
    }

    /**
     * Mark the current swing as consumed (we processed a window-hit from it).
     */
    public static void consumeSwing(Player attacker) {
        if (attacker == null) return;
        Long lastSwing = attacker.getTag(LAST_SWING_TICK);
        if (lastSwing != null) attacker.setTag(LAST_CONSUMED_SWING, lastSwing);
    }

    /**
     * Record that attacker hit victim at the given tick (from melee or projectile).
     * Do NOT call when the hit came from a swing-window path.
     */
    public static void recordHit(Player attacker, LivingEntity victim, long tick) {
        if (attacker == null || victim == null) return;
        UUID vId = victim.getUuid();
        if (attacker.getUuid().equals(vId)) return;
        VictimMap vm = attacker.getTag(VICTIMS);
        if (vm == null) vm = new VictimMap();
        vm.data().put(vId, tick);
        attacker.setTag(VICTIMS, vm);
    }

    /**
     * Get victims the attacker recently hit (melee/projectile) that are still within the window.
     * Swing-window hits are excluded. Prunes expired entries when reading.
     */
    public static List<LivingEntity> getRecentVictims(Player attacker, long currentTick, int windowTicks) {
        if (windowTicks <= 0) return List.of();
        VictimMap vm = attacker.getTag(VICTIMS);
        if (vm == null || vm.data().isEmpty()) return List.of();

        Map<UUID, Long> map = vm.data();
        List<LivingEntity> result = new ArrayList<>();
        long cutoff = currentTick - windowTicks;
        map.entrySet().removeIf(e -> e.getValue() < cutoff); // prune lazily
        if (map.isEmpty()) {
            attacker.removeTag(VICTIMS);
            return List.of();
        }

        Instance instance = attacker.getInstance();
        if (instance == null) return List.of();
        for (Map.Entry<UUID, Long> e : map.entrySet()) {
            LivingEntity v = findLivingByUuid(instance, e.getKey());
            if (v != null && !v.isRemoved()) {
                result.add(v);
            }
        }
        return result;
    }

    private static LivingEntity findLivingByUuid(Instance instance, UUID uuid) {
        for (var entity : instance.getEntities()) {
            if (entity instanceof LivingEntity living && entity.getUuid().equals(uuid)) return living;
        }
        return null;
    }
}

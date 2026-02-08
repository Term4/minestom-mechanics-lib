package com.minestom.mechanics.systems.health.damagetypes;

import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.systems.health.HealthEvent;
import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;

/**
 * Fire damage type implementation.
 * Modifies fire damage based on configuration, and applies fire/lava damage when
 * the player is in fire or lava blocks (Minestom does not apply these by default).
 */
public class Fire extends AbstractDamageType {
    private static final LogUtil.SystemLogger log = LogUtil.system("FireDamageType");

    private static final int FIRE_LAVA_DAMAGE_INTERVAL_TICKS = 20; // 1 second
    private static final int ON_FIRE_DAMAGE_INTERVAL_TICKS = 20;
    private static final int FIRE_TICKS_WHEN_IN_FIRE = 15 * 20;   // 15 seconds burning
    private static final float BASE_FIRE_DAMAGE = 2.0f;
    private static final float BASE_LAVA_DAMAGE = 4.0f;
    private static final float BASE_ON_FIRE_DAMAGE = 1.0f;

    private static final Tag<Long> LAST_FIRE_LAVA_TICK = Tag.Long("fire_last_fire_lava_tick").defaultValue(-1L);
    private static final Tag<Long> LAST_ON_FIRE_TICK = Tag.Long("fire_last_on_fire_tick").defaultValue(-1L);

    public Fire(HealthConfig config) {
        super(config, "FIRE", HealthSystem.FIRE_DAMAGE);
    }
    
    @Override
    protected boolean isEnabledByDefault() {
        return config.fireDamageEnabled();
    }
    
    @Override
    protected float getDefaultMultiplier() {
        return config.fireDamageMultiplier();
    }
    
    @Override
    protected boolean getDefaultBlockable() {
        return config.fireDamageBlockable();
    }

    @Override
    protected boolean getDefaultBypassInvulnerability() {
        return config.fireBypassInvulnerability();
    }

    @Override
    public boolean shouldHandle(HealthEvent event) {
        // Fire damage is handled via EntityDamageEvent, not HealthEvent
        // This will be called from HealthSystem when processing EntityDamageEvent
        return false;
    }
    
    @Override
    public void processHealthEvent(HealthEvent event) {
        // Fire damage is handled via modifyFireDamage, not through health events
    }
    
    /**
     * Modify fire damage for an entity damage event
     */
    public void modifyFireDamage(EntityDamageEvent event) {
        var damageType = event.getDamage().getType();
        LivingEntity entity = event.getEntity();

        if (isFireDamage(damageType)) {
            if (!isEnabled(entity)) {
                event.setCancelled(true);
                log.debug("Fire damage cancelled for entity");
                return;
            }

            float originalDamage = event.getDamage().getAmount();
            float newDamage = calculateDamage(null, entity, null, originalDamage);
            
            if (newDamage != originalDamage) {
                event.getDamage().setAmount(newDamage);
                
                log.debug("Fire damage modified: {:.2f} -> {:.2f} (multiplier: {:.2f})",
                        originalDamage, newDamage, getMultiplier(null, entity, null));
            }
        }
    }
    
    /**
     * Check if a damage type is fire-related
     */
    private boolean isFireDamage(RegistryKey<DamageType> damageType) {
        return damageType.equals(DamageType.ON_FIRE) ||
                damageType.equals(DamageType.IN_FIRE) ||
                damageType.equals(DamageType.LAVA);
    }

    /**
     * Track and apply fire/lava damage when the player is in fire or lava blocks.
     * Also applies "on fire" burning damage and sets the entity on fire visually via entity metadata.
     * Called from HealthSystem each player tick.
     */
    public void trackAndApplyFireDamage(Player player, long currentTick) {
        if (!isEnabled(player)) return;

        Instance instance = player.getInstance();
        if (instance == null) return;

        Pos pos = player.getPosition();
        int bx = pos.blockX(), by = pos.blockY(), bz = pos.blockZ();
        // Check feet, body, and immediate horizontal neighbors (3x2x3) so we detect fire/lava reliably
        boolean inLava = false;
        boolean inFire = false;
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block b = instance.getBlock(bx + dx, by + dy, bz + dz);
                    inLava = inLava || isLava(b);
                    inFire = inFire || isFire(b);
                }
            }
        }

        if (inLava) {
            setOnFireTicks(player, Math.max(getOnFireTicks(player), FIRE_TICKS_WHEN_IN_FIRE));
            long last = player.getTag(LAST_FIRE_LAVA_TICK);
            if (last < 0 || currentTick - last >= FIRE_LAVA_DAMAGE_INTERVAL_TICKS) {
                player.setTag(LAST_FIRE_LAVA_TICK, currentTick);
                float damage = calculateDamage(player, BASE_LAVA_DAMAGE);
                if (damage > 0) {
                    player.damage(new Damage(DamageType.LAVA, null, null, pos, damage));
                }
            }
        } else if (inFire) {
            setOnFireTicks(player, Math.max(getOnFireTicks(player), FIRE_TICKS_WHEN_IN_FIRE));
            long last = player.getTag(LAST_FIRE_LAVA_TICK);
            if (last < 0 || currentTick - last >= FIRE_LAVA_DAMAGE_INTERVAL_TICKS) {
                player.setTag(LAST_FIRE_LAVA_TICK, currentTick);
                float damage = calculateDamage(player, BASE_FIRE_DAMAGE);
                if (damage > 0) {
                    player.damage(new Damage(DamageType.IN_FIRE, null, null, pos, damage));
                }
            }
        } else {
            player.removeTag(LAST_FIRE_LAVA_TICK);
            int fireTicks = getOnFireTicks(player);
            if (fireTicks > 0) {
                setOnFireTicks(player, fireTicks - 1);
                long last = player.getTag(LAST_ON_FIRE_TICK);
                if (last < 0 || currentTick - last >= ON_FIRE_DAMAGE_INTERVAL_TICKS) {
                    player.setTag(LAST_ON_FIRE_TICK, currentTick);
                    float damage = calculateDamage(player, BASE_ON_FIRE_DAMAGE);
                    if (damage > 0) {
                        player.damage(new Damage(DamageType.ON_FIRE, null, null, pos, damage));
                    }
                }
            } else {
                player.removeTag(LAST_ON_FIRE_TICK);
            }
        }
    }

    private static boolean isFire(Block block) {
        if (block.compare(Block.FIRE) || block.compare(Block.SOUL_FIRE)) return true;
        return blockNameContains(block, "fire");
    }

    private static boolean isLava(Block block) {
        if (block.compare(Block.LAVA)) return true;
        return blockNameContains(block, "lava");
    }

    /** Fallback when compare() fails (e.g. different block state). Checks registry namespace/name. */
    private static boolean blockNameContains(Block block, String substring) {
        try {
            String name = block.name();
            return name != null && name.toLowerCase().contains(substring.toLowerCase());
        } catch (Throwable ignored) {
            try {
                String s = block.toString().toLowerCase();
                return s.contains(substring.toLowerCase());
            } catch (Throwable ignored2) {
                return false;
            }
        }
    }

    private static final Tag<Integer> FIRE_TICKS_TAG = Tag.Integer("health_fire_ticks").defaultValue(0);

    private static int getOnFireTicks(Player player) {
        return player.getTag(FIRE_TICKS_TAG);
    }

    private static void setOnFireTicks(Player player, int ticks) {
        player.setTag(FIRE_TICKS_TAG, Math.max(0, ticks));
        if (player.getEntityMeta() instanceof LivingEntityMeta meta) {
            meta.setOnFire(ticks > 0);
        }
    }

    @Override
    public void cleanup(LivingEntity entity) {
        super.cleanup(entity);
        entity.removeTag(LAST_FIRE_LAVA_TICK);
        entity.removeTag(LAST_ON_FIRE_TICK);
        entity.removeTag(FIRE_TICKS_TAG);
        if (entity.getEntityMeta() instanceof LivingEntityMeta meta) {
            meta.setOnFire(false);
        }
    }
}


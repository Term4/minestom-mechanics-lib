package com.minestom.mechanics.systems.health.damagetypes;

import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.systems.health.HealthEvent;
import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.registry.RegistryKey;

/**
 * Cactus damage type implementation.
 * Modifies cactus damage based on configuration, and applies cactus damage when
 * the player is touching a cactus block (Minestom does not apply this by default).
 */
public class Cactus extends AbstractDamageType {
    private static final LogUtil.SystemLogger log = LogUtil.system("CactusDamageType");

    private static final float BASE_CACTUS_DAMAGE = 1.0f;

    public Cactus(HealthConfig config) {
        super(config, "CACTUS", HealthSystem.CACTUS_DAMAGE);
    }
    
    @Override
    protected boolean isEnabledByDefault() {
        return config.cactusDamageEnabled();
    }
    
    @Override
    protected float getDefaultMultiplier() {
        return config.cactusDamageMultiplier();
    }
    
    @Override
    protected boolean getDefaultBlockable() {
        return config.cactusDamageBlockable();
    }

    @Override
    protected boolean getDefaultBypassInvulnerability() {
        return config.cactusBypassInvulnerability();
    }

    @Override
    public boolean shouldHandle(HealthEvent event) {
        // Cactus damage is handled via EntityDamageEvent, not HealthEvent
        return false;
    }
    
    @Override
    public void processHealthEvent(HealthEvent event) {
        // Cactus damage is handled via modifyCactusDamage, not through health events
    }
    
    /**
     * Modify cactus damage for an entity damage event
     */
    public void modifyCactusDamage(EntityDamageEvent event) {
        var damageType = event.getDamage().getType();
        LivingEntity entity = event.getEntity();

        if (isCactusDamage(damageType)) {
            if (!isEnabled(entity)) {
                event.setCancelled(true);
                log.debug("Cactus damage cancelled for entity");
                return;
            }

            float originalDamage = event.getDamage().getAmount();
            float newDamage = calculateDamage(null, entity, null, originalDamage);
            
            if (newDamage != originalDamage) {
                event.getDamage().setAmount(newDamage);
                
                log.debug("Cactus damage modified: {:.2f} -> {:.2f} (multiplier: {:.2f})",
                        originalDamage, newDamage, getMultiplier(null, entity, null));
            }
        }
    }
    
    /**
     * Check if a damage type is cactus-related
     */
    private boolean isCactusDamage(RegistryKey<DamageType> damageType) {
        return damageType.equals(DamageType.CACTUS);
    }

    /**
     * Track and apply cactus damage when the player is touching a cactus block.
     * Called from HealthSystem each player tick.
     */
    public void trackAndApplyCactusDamage(Player player, long currentTick) {
        if (!isEnabled(player)) return;

        Instance instance = player.getInstance();
        if (instance == null) return;

        Pos pos = player.getPosition();
        // Damage when hitbox actually intersects a cactus (top or sides). Invulnerability is bypassed for cactus by default.
        if (isTouchingCactus(instance, player)) {
            float damage = calculateDamage(player, BASE_CACTUS_DAMAGE);
            if (damage > 0) {
                player.damage(new Damage(DamageType.CACTUS, null, null, pos, damage));
            }
        }
    }

    /**
     * World AABB of the player from center-bottom position and bounding box.
     */
    private static record PlayerAABB(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        static PlayerAABB from(Pos pos, BoundingBox box) {
            double halfW = box.width() / 2;
            double halfD = box.depth() / 2;
            return new PlayerAABB(
                    pos.x() - halfW, pos.x() + halfW,
                    pos.y(), pos.y() + box.height(),
                    pos.z() - halfD, pos.z() + halfD);
        }
    }

    /**
     * True if the player's hitbox intersects a cactus block on the block's top or sides.
     * Uses player AABB vs full block AABB (cactus collision shape can be inset, so we use full block for damage).
     * No damage when only the block's BOTTOM face is touched (e.g. standing under a cactus).
     * Block range is derived from the full player AABB so e.g. head touching a cactus on a slab works.
     */
    private static boolean isTouchingCactus(Instance instance, Player player) {
        Pos pos = player.getPosition();
        BoundingBox box = player.getBoundingBox();
        PlayerAABB aabb = PlayerAABB.from(pos, box);

        int minBx = (int) Math.floor(aabb.minX());
        int maxBx = (int) Math.floor(aabb.maxX());
        int minBy = (int) Math.floor(aabb.minY());
        int maxBy = (int) Math.floor(aabb.maxY());
        int minBz = (int) Math.floor(aabb.minZ());
        int maxBz = (int) Math.floor(aabb.maxZ());

        for (int bx = minBx; bx <= maxBx; bx++) {
            for (int by = minBy; by <= maxBy; by++) {
                for (int bz = minBz; bz <= maxBz; bz++) {
                    if (!instance.getBlock(bx, by, bz).compare(Block.CACTUS)) continue;

                    if (!aabbsIntersect(aabb.minX(), aabb.maxX(), aabb.minY(), aabb.maxY(), aabb.minZ(), aabb.maxZ(),
                            bx, bx + 1, by, by + 1, bz, bz + 1)) continue;

                    BlockFace touched = touchedBlockFace(pos, box, bx, by, bz);
                    if (touched != BlockFace.BOTTOM) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean aabbsIntersect(
            double aMinX, double aMaxX, double aMinY, double aMaxY, double aMinZ, double aMaxZ,
            double bMinX, double bMaxX, double bMinY, double bMaxY, double bMinZ, double bMaxZ) {
        return aMinX < bMaxX && aMaxX > bMinX
                && aMinY < bMaxY && aMaxY > bMinY
                && aMinZ < bMaxZ && aMaxZ > bMinZ;
    }

    /**
     * Which face of the block (bx,by,bz) the player is touching, using smallest overlap axis.
     * Block AABB is [bx,bx+1] x [by,by+1] x [bz,bz+1]; player uses center-bottom pos + box.
     */
    private static BlockFace touchedBlockFace(Pos pos, BoundingBox box, int bx, int by, int bz) {
        PlayerAABB aabb = PlayerAABB.from(pos, box);
        double bMinX = bx, bMaxX = bx + 1;
        double bMinY = by, bMaxY = by + 1;
        double bMinZ = bz, bMaxZ = bz + 1;

        double overlapX = Math.max(0, Math.min(aabb.maxX(), bMaxX) - Math.max(aabb.minX(), bMinX));
        double overlapY = Math.max(0, Math.min(aabb.maxY(), bMaxY) - Math.max(aabb.minY(), bMinY));
        double overlapZ = Math.max(0, Math.min(aabb.maxZ(), bMaxZ) - Math.max(aabb.minZ(), bMinZ));

        double centerX = pos.x();
        double centerY = pos.y() + box.height() * 0.5;
        double centerZ = pos.z();
        double blockCenterX = bx + 0.5;
        double blockCenterY = by + 0.5;
        double blockCenterZ = bz + 0.5;

        if (overlapY <= overlapX && overlapY <= overlapZ) {
            return centerY < blockCenterY ? BlockFace.BOTTOM : BlockFace.TOP;
        }
        if (overlapX <= overlapZ) {
            return centerX < blockCenterX ? BlockFace.WEST : BlockFace.EAST;
        }
        return centerZ < blockCenterZ ? BlockFace.NORTH : BlockFace.SOUTH;
    }

    @Override
    public void cleanup(LivingEntity entity) {
        super.cleanup(entity);
    }
}


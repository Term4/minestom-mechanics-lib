package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.systems.knockback.KnockbackApplicator;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.item.SnowballMeta;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Snowball projectile using unified tag system.
 * Knockback is resolved automatically from tags by KnockbackSystem!
 */
public class Snowball extends CustomEntityProjectile implements ItemHoldingProjectile {

    public Snowball(@Nullable Entity shooter) {
        super(shooter, EntityType.SNOWBALL);
    }

    @Override
    public boolean onHit(Entity entity) {
        triggerStatus((byte) 3); // Snowball particles

        int damage = entity.getEntityType() == EntityType.BLAZE ? 3 : 0;

        // Only apply knockback if damage actually went through
        if (((LivingEntity) entity).damage(new Damage(DamageType.THROWN, this, getShooter(), null, damage))) {
            if (isUseKnockbackHandler()) {
                try {
                    var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
                    KnockbackApplicator applicator = projectileManager.getKnockbackApplicator();

                    // ✅ Applicator resolves config from tags automatically!
                    // Just pass 0 for enchantment level (projectiles don't have knockback enchants)
                    applicator.applyProjectileKnockback(
                            (LivingEntity) entity,
                            this,
                            shooterOriginPos,
                            0  // No knockback enchantment for projectiles
                    );
                } catch (Exception e) {
                    // Fallback: no knockback if applicator fails
                }
            }
        }
        return true;
    }

    @Override
    public boolean onStuck() {
        triggerStatus((byte) 3); // Snowball particles on block collision
        return true; // Remove snowball when it hits a block
    }

    @Override
    public void setItem(@NotNull ItemStack item) {
        ((SnowballMeta) getEntityMeta()).setItem(item);
    }
}
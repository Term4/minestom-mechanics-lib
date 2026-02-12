package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.systems.health.HealthSystem;
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
        LivingEntity living = (LivingEntity) entity;

        HealthSystem.applyDamage(living, new Damage(DamageType.THROWN, this, getShooter(), null, damage));
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
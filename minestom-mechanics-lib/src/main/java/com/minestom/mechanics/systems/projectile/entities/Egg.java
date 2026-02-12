package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.systems.health.HealthSystem;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.item.ThrownEggMeta;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO: Probably update to extend an abstract projectile

// TODO: Add boolean for if the eggs should spawn chickens

public class Egg extends CustomEntityProjectile implements ItemHoldingProjectile {

    public Egg(@Nullable Entity shooter) {
        super(shooter, EntityType.EGG);
    }

    @Override
    public boolean onHit(Entity entity) {
        triggerStatus((byte) 3);
        HealthSystem.applyDamage((LivingEntity) entity, new Damage(DamageType.THROWN, this, getShooter(), null, 0));
        return true;
    }

    @Override
    public boolean onStuck() {
        triggerStatus((byte) 3); // Egg particles on block collision
        return true; // Remove egg when it hits a block
    }

    @Override
    public void setItem(@NotNull ItemStack item) {
        ((ThrownEggMeta) getEntityMeta()).setItem(item);
    }

}
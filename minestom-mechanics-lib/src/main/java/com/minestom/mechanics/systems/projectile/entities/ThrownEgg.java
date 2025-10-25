package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.systems.knockback.KnockbackApplicator;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackPresets;
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

public class ThrownEgg extends CustomEntityProjectile implements ItemHoldingProjectile {

    private ProjectileKnockbackConfig knockbackConfig;

    public ThrownEgg(@Nullable Entity shooter) {
        super(shooter, EntityType.EGG);
        this.knockbackConfig = ProjectileKnockbackPresets.EGG;
    }

    @Override
    public boolean onHit(Entity entity) {
        triggerStatus((byte) 3); // Egg particles

        // Only apply knockback if damage actually went through
        if (((LivingEntity) entity).damage(new Damage(DamageType.THROWN, this, getShooter(), null, 0))) {
            if (isUseKnockbackHandler()) {
                try {
                    // Get applicator from ProjectileManager
                    var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
                    KnockbackApplicator applicator = projectileManager.getKnockbackApplicator();

                    applicator.applyProjectileKnockback((LivingEntity) entity, this, shooterOriginPos, 0);
                } catch (Exception e) {
                    // Fallback: no knockback if applicator fails
                }
            }
        }

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

    public void setKnockbackConfig(ProjectileKnockbackConfig config) {
        this.knockbackConfig = config;
    }

    public ProjectileKnockbackConfig getKnockbackConfig() {
        return knockbackConfig;
    }
}
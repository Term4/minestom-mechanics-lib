package com.minestom.mechanics.projectile.entities;

import com.minestom.mechanics.features.knockback.KnockbackSystem;
import com.minestom.mechanics.features.knockback.components.KnockbackApplicator;
import com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig;
import com.minestom.mechanics.projectile.config.ProjectileKnockbackPresets;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.item.SnowballMeta;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO: Probably update to extend an abstract projectile

public class Snowball extends CustomEntityProjectile implements ItemHoldingProjectile {

    private ProjectileKnockbackConfig knockbackConfig;

    public Snowball(@Nullable Entity shooter) {
        super(shooter, EntityType.SNOWBALL);
        // Initialize with default snowball knockback config
        this.knockbackConfig = ProjectileKnockbackPresets.SNOWBALL;
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
        triggerStatus((byte) 3); // Snowball particles on block collision
        return true; // Remove snowball when it hits a block
    }

    @Override
    public void setItem(@NotNull ItemStack item) {
        ((SnowballMeta) getEntityMeta()).setItem(item);
    }

    public void setKnockbackConfig(ProjectileKnockbackConfig config) {
        this.knockbackConfig = config;
    }

    public ProjectileKnockbackConfig getKnockbackConfig() {
        return knockbackConfig;
    }
}
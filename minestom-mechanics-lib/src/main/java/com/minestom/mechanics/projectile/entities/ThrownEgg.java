package com.minestom.mechanics.projectile.entities;

import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.item.ThrownEggMeta;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThrownEgg extends CustomEntityProjectile implements ItemHoldingProjectile {
	
	private ProjectileKnockbackConfig knockbackConfig;
	
	public ThrownEgg(@Nullable Entity shooter) {
		super(shooter, EntityType.EGG);
		// Initialize with default egg knockback config
		this.knockbackConfig = ProjectileKnockbackConfig.defaultEggKnockback();
	}
	
	@Override
	public boolean onHit(Entity entity) {
		triggerStatus((byte) 3); // Egg particles
		
		// Only apply knockback if damage actually went through
		if (((LivingEntity) entity).damage(new Damage(DamageType.THROWN, this, getShooter(), null, 0))) {
			// Apply knockback using integrated KnockbackHandler system
			if (isUseKnockbackHandler()) {
				try {
					KnockbackHandler knockbackHandler = KnockbackHandler.getInstance();
					if (knockbackHandler != null) {
						// Use config values for egg knockback
						double horizontalKnockback = knockbackConfig.horizontalKnockback();
						double verticalKnockback = knockbackConfig.verticalKnockback();
						
						knockbackHandler.applyProjectileKnockback((LivingEntity) entity, this, shooterOriginPos, 
							horizontalKnockback, verticalKnockback, 0);
					}
				} catch (Exception e) {
					// Fallback: no knockback if KnockbackHandler fails
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
}

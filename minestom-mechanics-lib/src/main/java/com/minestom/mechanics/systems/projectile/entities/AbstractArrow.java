package com.minestom.mechanics.systems.projectile.entities;

import com.minestom.mechanics.config.constants.ProjectileConstants;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackPresets;
import com.minestom.mechanics.systems.knockback.KnockbackApplicator;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.projectile.AbstractArrowMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.CollectItemPacket;
import net.minestom.server.utils.MathUtils;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * IMPROVED arrow with proper water physics and better damage/knockback
 */
public abstract class AbstractArrow extends CustomEntityProjectile {

	protected int pickupDelay;
	protected int stuckTime;
	protected PickupMode pickupMode = PickupMode.DISALLOWED;
	protected int ticks;
	private double baseDamage = ProjectileConstants.ARROW_BASE_DAMAGE;
	private int knockback;
	
	// Knockback configuration - now uses the main KnockbackHandler
	private boolean useKnockbackHandler = true;
	private ProjectileKnockbackConfig knockbackConfig;

	private final Set<Integer> piercingIgnore = new HashSet<>();
	private int fireTicksLeft = 0;

	// Track if we were in water last tick
	private boolean wasInWater = false;

	public AbstractArrow(@Nullable Entity shooter, @NotNull EntityType entityType) {
		super(shooter, entityType);

		if (shooter instanceof Player) {
			pickupMode = ((Player) shooter).getGameMode() == GameMode.CREATIVE ?
					PickupMode.CREATIVE_ONLY : PickupMode.ALLOWED;
		}

		// ✅ FIX: Prevent immediate pickup (vanilla 1.8 behavior)
		pickupDelay = ProjectileConstants.ARROW_PICKUP_DELAY_TICKS;

		// Initialize with default arrow knockback config
        this.knockbackConfig = ProjectileKnockbackPresets.ARROW;
    }

	@Override
	public void tick(long time) {
		// Check if arrow just entered water
		boolean inWaterNow = isInWater();

		// Play water splash sound if entering water
		if (!wasInWater && inWaterNow) {
			onEnterWater();
		}

		wasInWater = inWaterNow;

		super.tick(time);
	}

	@Override
	public void update(long time) {
		if (onGround) {
			stuckTime++;
		} else {
			stuckTime = 0;
		}

		if (pickupDelay > 0) {
			pickupDelay--;
		}

		if (fireTicksLeft > 0) {
			// Extinguish fire in water
			if (isInWater()) {
				fireTicksLeft = 0;
				entityMeta.setOnFire(false);
			} else if (entityMeta.isOnFire()) {
				fireTicksLeft--;
				if (fireTicksLeft == 0) {
					entityMeta.setOnFire(false);
				}
			} else {
				fireTicksLeft = 0;
			}
		}

		// Pickup logic
		if (canBePickedUp(null)) {
			instance.getEntityTracker().nearbyEntities(position, 5,
					net.minestom.server.instance.EntityTracker.Target.PLAYERS, player -> {
						if (!player.canPickupItem()) return;
						if (!isViewer(player)) return;
						if (isRemoved() || !canBePickedUp(player)) return;
						
						// Don't allow dead players to pick up arrows
						if (Boolean.TRUE.equals(player.getTag(com.minestom.mechanics.systems.health.HealthSystem.IS_DEAD))) {
							return;
						}

						if (player.getBoundingBox().expand(1, 0.5f, 1)
								.intersectEntity(player.getPosition(), this)) {
							if (pickup(player)) {
								player.sendPacketToViewersAndSelf(new CollectItemPacket(
										getEntityId(), player.getEntityId(), 1
								));
								remove();
							}
						}
					});
		}

		tickRemoval();
	}

	// Called when arrow enters water
	protected void onEnterWater() {
		// Could play splash sound here if you want
		// The water drag will automatically slow the arrow significantly
	}

	// Check if arrow is currently in water
	private boolean isInWater() {
		var block = instance.getBlock(position);
		return block.compare(net.minestom.server.instance.block.Block.WATER);
	}

	public void setFireTicksLeft(int fireTicksLeft) {
		this.fireTicksLeft = fireTicksLeft;
		if (fireTicksLeft > 0) entityMeta.setOnFire(true);
	}

	protected void tickRemoval() {
		ticks++;
		if (ticks >= 1200) {
			remove();
		}
	}

	@Override
	public void onUnstuck() {
		((AbstractArrowMeta) getEntityMeta()).setInGround(false);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		setVelocity(velocity.mul(
				random.nextDouble() * 0.2,
				random.nextDouble() * 0.2,
				random.nextDouble() * 0.2
		));
		ticks = 0;
	}

	@Override
	public boolean canHit(Entity entity) {
		return super.canHit(entity) && !piercingIgnore.contains(entity.getEntityId());
	}

	@Override
	public boolean onHit(@NotNull Entity entity) {
		if (piercingIgnore.contains(entity.getEntityId())) return false;
		if (!(entity instanceof LivingEntity living)) return false;

		ThreadLocalRandom random = ThreadLocalRandom.current();

		// Calculate damage based on velocity
		double movementSpeed = getVelocity().length() / ServerFlag.SERVER_TICKS_PER_SECOND;
		int damage = (int) Math.ceil(MathUtils.clamp(
				movementSpeed * baseDamage, 0.0, 2.147483647E9D));

		if (getPiercingLevel() > 0) {
			if (piercingIgnore.size() >= getPiercingLevel() + 1) {
				return true;
			}
			piercingIgnore.add(entity.getEntityId());
		}

		if (isCritical()) {
			int randomDamage = random.nextInt(damage / 2 + 2);
			damage = (int) Math.min(randomDamage + damage, 2147483647L);
		}

		Entity shooter = getShooter();
		Damage damageObj = new Damage(
				DamageType.ARROW,
				this, Objects.requireNonNullElse(shooter, this),
				null, damage
		);

		if (living.damage(damageObj)) {
			if (entity.getEntityType() == EntityType.ENDERMAN) return false;

			if (isOnFire()) {
				living.setFireTicks(5 * ServerFlag.SERVER_TICKS_PER_SECOND);
			}

			if (getPiercingLevel() <= 0) {
				living.setArrowCount(living.getArrowCount() + 1);
			}

		// Apply knockback using integrated KnockbackHandler system
            if (useKnockbackHandler) {
                try {
                    var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
                    KnockbackApplicator applicator = projectileManager.getKnockbackApplicator();

                    applicator.applyProjectileKnockback(living, this, shooterOriginPos, knockback);
                } catch (Exception e) {
                    // Fallback to old system if applicator fails
                    if (knockback > 0) {
                        Vec knockbackVec = getVelocity()
                                .mul(1, 0, 1)
                                .normalize().mul(knockback * 0.6);
                        knockbackVec = knockbackVec.add(0, 0.1, 0)
                                .mul(ServerFlag.SERVER_TICKS_PER_SECOND / 2.0);

                        if (knockbackVec.lengthSquared() > 0) {
                            Vec newVel = living.getVelocity().add(knockbackVec);
                            living.setVelocity(newVel);
                        }
                    }
                }
            }

			onHurt(living);

			return getPiercingLevel() <= 0;
		} else {
			// Bounce off if hit was blocked
			Pos position = getPosition();
			setVelocity(getVelocity().mul(-0.5 * 0.2));
			refreshPosition(position.withYaw(position.yaw() + 170.0f + 20.0f *
					ThreadLocalRandom.current().nextFloat()));

			if (getVelocity().lengthSquared() < 1.0E-7D) {
				if (pickupMode == PickupMode.ALLOWED) {
					spawnItemAtLocation(getPickupItem());
				}
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean onStuck() {
		pickupDelay = 7;
		((AbstractArrowMeta) getEntityMeta()).setInGround(true);
		setCritical(false);
		setPiercingLevel((byte) 0);
		piercingIgnore.clear();

		return false;
	}

	public boolean canBePickedUp(@Nullable Player player) {
		if (pickupDelay > 0) return false;

		return switch (pickupMode) {
			case ALLOWED -> true;
			case CREATIVE_ONLY -> player != null && player.getGameMode() == GameMode.CREATIVE;
			case DISALLOWED -> false;
		};
	}

    // TODO: for legacy clients, they may need their inventory force updated
    //  Could also be an issue when I add normal pickups of dropped items
	private boolean pickup(Player player) {
		if (!canBePickedUp(player)) return false;

		ItemStack pickupItem = getPickupItem();
		if (pickupItem.isAir()) return false;

		// Try to add to inventory
		return player.getInventory().addItemStack(pickupItem);
	}

    private void spawnItemAtLocation(ItemStack stack) {
        if (stack.isAir()) return;

        // NOTE: Intentionally not spawning item entity
        // In 1.8 style PvP, arrows that can't be picked up are removed to reduce clutter
        // This matches vanilla behavior when pickup mode is DISALLOWED
        // If item spawning is needed in the future, create an ItemEntity here
    }

	protected abstract ItemStack getPickupItem();

	protected void onHurt(LivingEntity entity) {
		// Override in subclasses to add potion effects, etc.
	}

	// Getters and setters
	public double getBaseDamage() {
		return baseDamage;
	}

	public void setBaseDamage(double baseDamage) {
		this.baseDamage = baseDamage;
	}

	public int getKnockback() {
		return knockback;
	}

	public void setKnockback(int knockback) {
		this.knockback = knockback;
	}
	
	public void setUseKnockbackHandler(boolean useKnockbackHandler) {
		this.useKnockbackHandler = useKnockbackHandler;
	}

	public void setKnockbackConfig(ProjectileKnockbackConfig config) {
		this.knockbackConfig = config;
	}

	public boolean isCritical() {
		return ((AbstractArrowMeta) getEntityMeta()).isCritical();
	}

	public void setCritical(boolean critical) {
		((AbstractArrowMeta) getEntityMeta()).setCritical(critical);
	}

	public byte getPiercingLevel() {
		return ((AbstractArrowMeta) getEntityMeta()).getPiercingLevel();
	}

	public void setPiercingLevel(byte level) {
		((AbstractArrowMeta) getEntityMeta()).setPiercingLevel(level);
	}

	public PickupMode getPickupMode() {
		return pickupMode;
	}

	public void setPickupMode(PickupMode pickupMode) {
		this.pickupMode = pickupMode;
	}

	public boolean isOnFire() {
		return fireTicksLeft > 0;
	}

	public enum PickupMode {
		DISALLOWED,
		ALLOWED,
		CREATIVE_ONLY
	}
}

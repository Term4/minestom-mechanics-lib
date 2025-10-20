package com.minestom.mechanics.projectile.entities;

import com.minestom.mechanics.damage.DamageFeature;
import com.minestom.mechanics.systems.knockback.KnockbackApplicator;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackConfig;
import com.minestom.mechanics.config.projectiles.advanced.ProjectileKnockbackPresets;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.item.ThrownEnderPearlMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

// TODO: Probably update to extend an abstract projectile. Make sure to maintain teleportation functionality.
//  ALSO make teleporting a toggleable boolean feature.

/**
 * Ender pearl projectile
 * FIXED: Uses DamageSystem instead of FallFeature
 */
public class ThrownEnderpearl extends CustomEntityProjectile implements ItemHoldingProjectile {
    private Pos prevPos = Pos.ZERO;
    private ProjectileKnockbackConfig knockbackConfig;

    public ThrownEnderpearl(@Nullable Entity shooter) {
        super(shooter, EntityType.ENDER_PEARL);
        // Initialize with default ender pearl knockback config (no knockback)
        this.knockbackConfig = ProjectileKnockbackPresets.ENDER_PEARL;
        // Ender pearls don't knockback by default
        setUseKnockbackHandler(false);
    }

    private void teleportOwner() {
        Pos position = prevPos;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < 32; i++) {
            sendPacketToViewersAndSelf(new ParticlePacket(
                    Particle.PORTAL, false, false,
                    position.x(), position.y() + random.nextDouble() * 2, position.z(),
                    (float) random.nextGaussian(), 0.0F, (float) random.nextGaussian(),
                    0, 1
            ));
        }

        if (isRemoved()) return;

        Entity shooter = getShooter();
        if (shooter != null) {
            Pos shooterPos = shooter.getPosition();
            position = position.withPitch(shooterPos.pitch()).withYaw(shooterPos.yaw());
        }

        if (shooter instanceof Player player) {
            if (player.isOnline() && player.getInstance() == getInstance()
                    && player.getPlayerMeta().getBedInWhichSleepingPosition() == null) {
                if (player.getVehicle() != null) {
                    player.getVehicle().removePassenger(player);
                }

                player.teleport(position);

                // Reset fall distance using DamageFeature
                DamageFeature.getInstance().resetFallDistance(player);

                player.damage(DamageType.FALL, 5.0F);
            }
        } else if (shooter != null) {
            shooter.teleport(position);
        }
    }

    @Override
    public boolean onHit(Entity entity) {
        // Only apply knockback if damage actually went through
        if (((LivingEntity) entity).damage(DamageType.THROWN, 0)) {
            // Apply knockback (disabled by default for ender pearls)
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

        teleportOwner();
        return true;
    }

    @Override
    public boolean onStuck() {
        teleportOwner();
        return true;
    }

    @Override
    public void tick(long time) {
        Entity shooter = getShooter();
        if (shooter instanceof Player && ((Player) shooter).isDead()) {
            remove();
        } else {
            prevPos = getPosition();
            super.tick(time);
        }
    }

    @Override
    public void setItem(@NotNull ItemStack item) {
        ((ThrownEnderPearlMeta) getEntityMeta()).setItem(item);
    }

    public void setKnockbackConfig(ProjectileKnockbackConfig config) {
        this.knockbackConfig = config;
    }

    public ProjectileKnockbackConfig getKnockbackConfig() {
        return knockbackConfig;
    }
}
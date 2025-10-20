package com.minestom.mechanics.projectile.entities;

import com.minestom.mechanics.config.projectiles.ProjectileConfig;
import com.minestom.mechanics.features.knockback.components.KnockbackApplicator;
import com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig;
import com.minestom.mechanics.projectile.config.ProjectileKnockbackPresets;
import com.minestom.mechanics.projectile.ProjectileBehavior;
import com.minestom.mechanics.features.knockback.KnockbackSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.other.FishingHookMeta;
import org.jetbrains.annotations.Nullable;

/**
 * Fishing bobber entity with consolidated architecture.
 * ✅ CONSOLIDATED: Merged 4 component classes into private methods for better maintainability.
 */
public class FishingBobber extends CustomEntityProjectile implements ProjectileBehavior {

    // State enum
    public enum State {
        IN_AIR,
        HOOKED_ENTITY,
        BOBBING
    }

    // State tracking
    private State currentState = State.IN_AIR;
    private Entity hookedEntity;
    private int stuckTime = 0;

    // Vision obstruction tracking
    private boolean shouldFallAfterHit = false;
    private int fallTicks = 0;
    private boolean hasAppliedFallVelocity = false;

    private final boolean legacy;
    private final double customGravity;

    // Knockback configuration
    private ProjectileKnockbackConfig knockbackConfig;
    private ProjectileConfig.FishingRodKnockbackMode knockbackMode;

    // Logging
    private static final LogUtil.SystemLogger log = LogUtil.system("FishingBobber");

    public FishingBobber(@Nullable Entity shooter, boolean legacy) {
        super(shooter, EntityType.FISHING_BOBBER);
        this.legacy = legacy;
        setOwnerEntity(shooter);

        // Custom gravity logic: gravity is applied before movement
        this.customGravity = legacy ?
                com.minestom.mechanics.projectile.ProjectileConstants.FISHING_BOBBER_LEGACY_GRAVITY :
                com.minestom.mechanics.projectile.ProjectileConstants.FISHING_BOBBER_MODERN_GRAVITY;

        // Initialize with default knockback config and mode
        this.knockbackConfig = ProjectileKnockbackPresets.FISHING_ROD;
        this.knockbackMode = ProjectileConfig.FishingRodKnockbackMode.BOBBER_RELATIVE;

        // Custom aerodynamics for fishing bobber
        // Set gravity to 0 because we apply custom gravity in tick()
        setAerodynamics(getAerodynamics().withGravity(0));
        // Lower air resistance for fishing bobber feel
        setAerodynamics(getAerodynamics().withHorizontalAirResistance(0.92).withVerticalAirResistance(0.92));
    }

    @Override
    public void tick(long time) {
        // Apply custom gravity before physics
        velocity = velocity.add(0, -customGravity * ServerFlag.SERVER_TICKS_PER_SECOND, 0);
        super.tick(time);
    }

    @Override
    protected void movementTick() {
        // Handle fishing-specific physics before calling parent
        if (currentState == State.HOOKED_ENTITY) {
            // When hooked to an entity, skip normal physics and pull the entity instead
            if (hookedEntity != null && isHookedEntityValid(hookedEntity, getInstance())) {
                pullEntity(hookedEntity);
            }
            return; // Skip normal physics when hooked
        }

        // Call parent for standard projectile physics (gravity, air resistance, water drag)
        super.movementTick();
    }

    @Override
    public void update(long time) {
        if (!shouldFallAfterHit && currentState == State.IN_AIR) {
            // Vision obstruction tracking for fall after hitting player face
            Entity shooter = getShooter();
            if (shooter instanceof Player player) {
                Pos bobberPos = getPosition();
                Pos eyePos = player.getPosition().add(0, player.getEyeHeight(), 0);

                Vec direction = new Vec(
                        bobberPos.x() - eyePos.x(),
                        bobberPos.y() - eyePos.y(),
                        bobberPos.z() - eyePos.z()
                );

                double distance = direction.length();

                if (distance < 0.5) { // Very close to face
                    // Check if bobber hit while close to face and moving toward player
                    if (getVelocity().length() > 0.1) {
                        startFallSequence();
                    }
                }
            }
        }

        if (shouldFallAfterHit) {
            fallTicks++;

            if (!hasAppliedFallVelocity && shouldStartFallSequence(fallTicks, 3)) {
                // Apply fall velocity after delay
                Entity shooter = getShooter();
                if (shooter != null) {
                    Pos bobberPos = getPosition();
                    Pos shooterPos = shooter.getPosition();

                    // Use calculateFallVelocity for better fall direction
                    Vec fallVelocity = calculateFallVelocity(bobberPos, shooterPos);
                    setVelocity(fallVelocity);
                    hasAppliedFallVelocity = true;
                    log.debug("Applied fall velocity to bobber");
                }
            }
        }

        // Remove if stuck too long
        if (onGround) {
            stuckTime++;
            if (isStuckTooLong(stuckTime, 1200)) { // 60 seconds
                remove();
                return;
            }
        } else {
            stuckTime = 0;
        }
    }

    @Override
    public boolean onHit(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;

        // Check if entity can be hooked
        if (!canHookEntity(entity, (Player) getShooter())) {
            return false;
        }

        // TODO: Find a way to hook players and NOT have the bobber in their face
        // For players: apply knockback and don't hook
        if (entity instanceof Player) {
            // Apply damage first
            if (((LivingEntity) entity).damage(new Damage(DamageType.GENERIC, null, null, null, 0))) {
                // Use the integrated knockback system for consistent knockback
                if (isUseKnockbackHandler()) {
                    try {
                        var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
                        KnockbackApplicator applicator = projectileManager.getKnockbackApplicator();

                        // Determine knockback origin based on mode
                        Pos knockbackOrigin;
                        if (knockbackMode == ProjectileConfig.FishingRodKnockbackMode.BOBBER_RELATIVE) {
                            // Knockback away from bobber position (vanilla behavior)
                            knockbackOrigin = this.getPosition();
                        } else {
                            // Knockback from shooter position (like normal projectiles)
                            knockbackOrigin = getShooter() != null ? getShooter().getPosition() : this.getPosition();
                        }

                        applicator.applyProjectileKnockback((LivingEntity) entity, this, knockbackOrigin, 0);
                    } catch (Exception e) {
                        // No knockback if applicator fails
                        log.debug("KnockbackApplicator failed, no knockback applied");
                    }
                }
            }
            // Don't hook players, just apply knockback
            return false;
        }

        // For other entities: hook them (fish, items, etc.)
        setHookedEntity(entity);
        return false;
    }

    private void setHookedEntity(@Nullable Entity entity) {
        this.hookedEntity = entity;
        ((FishingHookMeta) getEntityMeta()).setHookedEntity(entity);
    }

    private void setOwnerEntity(@Nullable Entity entity) {
        ((FishingHookMeta) getEntityMeta()).setOwnerEntity(entity);
    }

    public int retrieve() {
        if (!(getShooter() instanceof Player)) return 0;

        int durability = 0;
        if (hookedEntity != null) {
            if (!legacy) {
                pullEntity(hookedEntity);
                triggerStatus((byte) 31);
            }
            durability = hookedEntity instanceof ItemEntity ? 3 : 5;
        }

        remove();

        return durability;
    }

    /**
     * Pull hooked entity towards the shooter.
     * Configurable to allow/disallow pulling players.
     */
    private void pullEntity(Entity entity) {
        Entity shooter = getShooter();
        if (shooter == null) return;

        // Check if we should pull players (configurable)
        if (entity instanceof Player) {
            var projectileManager = com.minestom.mechanics.manager.ProjectileManager.getInstance();
            var config = projectileManager.getProjectileConfig();

            if (!config.isFishingRodPullPlayers()) {
                return; // Don't pull players if disabled
            }
        }

        // Calculate pull velocity towards shooter
        Pos shooterPos = shooter.getPosition();
        Pos pos = getPosition();
        Vec velocity = new Vec(
                shooterPos.x() - pos.x(),
                shooterPos.y() - pos.y(),
                shooterPos.z() - pos.z()
        ).mul(0.1);

        velocity = velocity.mul(ServerFlag.SERVER_TICKS_PER_SECOND);
        entity.setVelocity(entity.getVelocity().add(velocity));
    }

    // ===========================
    // KNOCKBACK CONFIGURATION
    // ===========================

    public void setKnockbackConfig(ProjectileKnockbackConfig config) {
        this.knockbackConfig = config;
    }

    public ProjectileKnockbackConfig getKnockbackConfig() {
        return knockbackConfig;
    }

    public void setKnockbackMode(ProjectileConfig.FishingRodKnockbackMode mode) {
        this.knockbackMode = mode;
    }

    public ProjectileConfig.FishingRodKnockbackMode getKnockbackMode() {
        return knockbackMode;
    }

    @Override
    public void remove() {
        // Tag cleanup is handled by FishingRodFeature.cleanup()
        // when player disconnects/dies via ProjectileCleanupHandler
        super.remove();
    }

    // ===========================
    // PRIVATE HELPER METHODS
    // ===========================

    private Vec calculateFallVelocity(Pos bobberPos, Pos shooterPos) {
        // Calculate direction away from player
        double dx = bobberPos.x() - shooterPos.x();
        double dz = bobberPos.z() - shooterPos.z();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance > 0.01) { // Avoid division by zero
            // Normalize and add horizontal velocity away from player
            double awayX = (dx / distance) * 0.8;
            double awayZ = (dz / distance) * 0.8;
            return new Vec(awayX, -0.4 * ServerFlag.SERVER_TICKS_PER_SECOND, awayZ);
        } else {
            // Fall straight down if too close
            return new Vec(0, -0.4 * ServerFlag.SERVER_TICKS_PER_SECOND, 0);
        }
    }

    private Vec calculateStraightFallVelocity() {
        return new Vec(0, -0.4 * ServerFlag.SERVER_TICKS_PER_SECOND, 0);
    }

    private boolean isHookedEntityValid(Entity hookedEntity, net.minestom.server.instance.Instance currentInstance) {
        return hookedEntity != null &&
                !hookedEntity.isRemoved() &&
                hookedEntity.getInstance() == currentInstance;
    }

    private boolean canHookEntity(Entity entity, Player shooter) {
        if (entity == null) {
            return false;
        }

        // Can't hook the shooter
        if (entity == shooter) {
            return false;
        }

        // Can't hook creative players
        if (entity instanceof Player player && player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }

        return true;
    }

    private boolean isStuckTooLong(int stuckTime, int maxStuckTime) {
        return stuckTime >= maxStuckTime;
    }

    private boolean shouldStartFallSequence(int fallTicks, int fallDelay) {
        return fallTicks >= fallDelay;
    }

    private void startFallSequence() {
        this.shouldFallAfterHit = true;
        this.fallTicks = 0;
        this.hasAppliedFallVelocity = false;
    }

    // ===========================
    // PROJECTILE BEHAVIOR INTERFACE
    // ===========================

    // TODO: Maybe create a general projectile interface for this kinda thing?
    //  Then we can have a more generic projectile behavior that can be used for
    //  all projectiles, not just fishing rods. (OnHit, OnStuck, etc)

    @Override
    public boolean onHit(Entity projectile, Entity hit) {
        return onHit(hit); // Delegate to existing method
    }

    @Override
    public boolean onBlockHit(Entity projectile, Point position) {
        // Fishing bobbers don't typically interact with blocks
        return false;
    }

    @Override
    public void onExpire(Entity projectile) {
        remove(); // Remove the bobber when it expires
    }
}
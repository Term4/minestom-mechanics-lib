package com.minestom.mechanics.projectile.entities;

import com.minestom.mechanics.projectile.config.FishingRodKnockbackConfig;
import com.minestom.mechanics.projectile.ProjectileBehavior;
import com.minestom.mechanics.features.knockback.KnockbackHandler;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.MechanicsConstants;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.other.FishingHookMeta;
import org.jetbrains.annotations.Nullable;

/**
 * Fishing bobber entity with consolidated architecture.
 * ✅ CONSOLIDATED: Merged 4 component classes into private methods for better maintainability.
 */
public class FishingBobber extends CustomEntityProjectile implements ProjectileBehavior {
    
    // State enum (moved from FishingBobberStateManager)
    public enum State {
        IN_AIR,
        HOOKED_ENTITY,
        BOBBING
    }
    
    // State tracking (consolidated from FishingBobberStateManager)
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
    private FishingRodKnockbackConfig knockbackConfig;
    
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
        
        // Initialize with default knockback config
        this.knockbackConfig = FishingRodKnockbackConfig.defaultConfig();

        // Custom aerodynamics for fishing bobber (matches OLD implementation)
        // Set gravity to 0 because we apply custom gravity in tick()
        setAerodynamics(getAerodynamics().withGravity(0));
        // Lower air resistance for fishing bobber feel
        setAerodynamics(getAerodynamics().withHorizontalAirResistance(0.92).withVerticalAirResistance(0.92));
    }

    @Override
    public void tick(long time) {
        // Apply custom gravity before physics (matches OLD implementation)
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
        if (!(getShooter() instanceof Player shooter)) {
            remove();
            return;
        }
        
        // ✅ FIXED: Auto-retract at 30-33 block distance
        Pos shooterPos = shooter.getPosition();
        Pos bobberPos = getPosition();
        double distance = shooterPos.distance(bobberPos);
        
                // Auto-retract if too far (30-33 blocks)
                if (distance > MechanicsConstants.AUTO_RETRACT_DISTANCE) {
            log.debug("Auto-retracting fishing bobber for {} at {:.1f}b distance", shooter.getUsername(), distance);
            remove();
            return;
        }
        
        // ✅ REMOVED: Client handles fishing rod retraction automatically
        // The client sends retrieve packets when switching items, so we don't need server-side checks

        // Vision obstruction fix: handle falling after hit
        if (shouldFallAfterHit && !hasAppliedFallVelocity) {
            fallTicks++;
            // After 3 ticks (0.15 seconds), start falling away from player's face
            // Reduced time for faster response to vision obstruction
            if (shouldStartFallSequence(fallTicks, 3)) {
                setNoGravity(false);
                // Fall down and away from player to get out of vision
                Entity bobberShooter = getShooter();
                if (bobberShooter != null) {
                    Pos fallShooterPos = bobberShooter.getPosition();
                    Pos fallBobberPos = getPosition();
                    velocity = velocity.add(calculateFallVelocity(fallBobberPos, fallShooterPos));
                } else {
                    // Fall straight down if no shooter
                    velocity = velocity.add(calculateStraightFallVelocity());
                }
                hasAppliedFallVelocity = true;
                shouldFallAfterHit = false;
            }
        }

        if (onGround) {
            stuckTime++;
            // ✅ FIXED: Remove timeout altogether - bobbers should persist indefinitely
                    // Only remove if stuck for an extremely long time (1 hour = 72000 ticks)
                    if (isStuckTooLong(stuckTime, MechanicsConstants.ONE_HOUR_TICKS)) {
                remove();
                return;
            }
        } else {
            stuckTime = 0;
        }

        if (currentState == State.IN_AIR) {
            if (hookedEntity != null) {
                velocity = Vec.ZERO;
                setNoGravity(true);
                currentState = State.HOOKED_ENTITY;
                // Vision obstruction fix: start fall timer after hitting
                startFallSequence();
            }
        } else {
            if (currentState == State.HOOKED_ENTITY) {
                if (hookedEntity != null) {
                    if (!isHookedEntityValid(hookedEntity, getInstance())) {
                        setHookedEntity(null);
                        setNoGravity(false);
                        currentState = State.IN_AIR;
                    } else {
                        // Pull the hooked entity toward the player instead of teleporting bobber
                        pullEntity(hookedEntity);
                    }
                }
            }
        }
    }

    @Override
    public boolean onHit(Entity entity) {
        if (hookedEntity != null) return false;
        
        // Check if entity can be hooked
        if (!canHookEntity(entity, (Player) getShooter())) {
            return false;
        }
        
        // For players: apply knockback and don't hook
        if (entity instanceof Player) {
            // Apply damage first
            if (((LivingEntity) entity).damage(new Damage(DamageType.GENERIC, null, null, null, 0))) {
                // Use the integrated KnockbackHandler system for consistent knockback
                if (isUseKnockbackHandler()) {
                    try {
                        KnockbackHandler knockbackHandler = KnockbackHandler.getInstance();
                        if (knockbackHandler != null) {
                        // Use fishing rod-specific knockback values from config
                        double horizontalKnockback = knockbackConfig.getHorizontalKnockback();
                        double verticalKnockback = knockbackConfig.getVerticalKnockback();
                            
                            knockbackHandler.applyProjectileKnockback((LivingEntity) entity, this, getShooter().getPosition(), 
                                horizontalKnockback, verticalKnockback, 0);
                        }
                    } catch (Exception e) {
                        // Fallback: apply simple knockback if KnockbackHandler fails
                        applySimpleKnockback(entity);
                    }
                } else {
                    // Fallback: apply simple knockback
                    applySimpleKnockback(entity);
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
    
    /**
     * Apply simple knockback as fallback when KnockbackHandler is not available
     */
    private void applySimpleKnockback(Entity entity) {
        // Apply knockback away from bobber (not toward shooter)
        Pos entityPos = entity.getPosition();
        Pos bobberPos = getPosition();
        Vec knockbackDirection = new Vec(
            entityPos.x() - bobberPos.x(),
            entityPos.y() - bobberPos.y(),
            entityPos.z() - bobberPos.z()
        );
        // Normalize the direction vector
        double length = Math.sqrt(knockbackDirection.x() * knockbackDirection.x() + 
            knockbackDirection.y() * knockbackDirection.y() + 
            knockbackDirection.z() * knockbackDirection.z());
        if (length > 0) {
            knockbackDirection = knockbackDirection.div(length);
        }
        Vec knockbackVelocity = knockbackDirection.mul(0.4).add(0, 0.4, 0);
        entity.setVelocity(knockbackVelocity);
    }

    private void setOwnerEntity(@Nullable Entity entity) {
        ((FishingHookMeta) getEntityMeta()).setOwnerEntity(entity);
    }

    public int retrieve() {
        if (!(getShooter() instanceof Player)) return 0;
        // ✅ REMOVED: Client handles fishing rod retraction automatically

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

    private void pullEntity(Entity entity) {
        Entity shooter = getShooter();
        if (shooter == null) return;

        // Only pull non-player entities (fish, items, etc.)
        // Players should not be pulled by fishing rods
        if (entity instanceof Player) {
            return;
        }

        Pos shooterPos = shooter.getPosition();
        Pos pos = getPosition();
        Vec velocity = new Vec(shooterPos.x() - pos.x(), shooterPos.y() - pos.y(),
                shooterPos.z() - pos.z()).mul(0.1);
        velocity = velocity.mul(ServerFlag.SERVER_TICKS_PER_SECOND);
        entity.setVelocity(entity.getVelocity().add(velocity));
    }
    
    // ===========================
    // KNOCKBACK CONFIGURATION
    // ===========================
    
    public void setKnockbackConfig(FishingRodKnockbackConfig config) {
        this.knockbackConfig = config;
    }
    
    public FishingRodKnockbackConfig getKnockbackConfig() {
        return knockbackConfig;
    }

    @Override
    public void remove() {
        Entity shooter = getShooter();
        if (shooter != null) {
            // ✅ FIXED: Use FishingBobberManager instead of FishingRodFeature
            if (shooter.getTag(com.minestom.mechanics.projectile.components.FishingBobberManager.FISHING_BOBBER) == this) {
                shooter.removeTag(com.minestom.mechanics.projectile.components.FishingBobberManager.FISHING_BOBBER);
            }
        }

        super.remove();
    }

    // ===========================
    // CONSOLIDATED COMPONENT METHODS
    // ===========================
    
    // Physics methods (from FishingBobberPhysics)
    private Vec calculateFallVelocity(Pos bobberPos, Pos shooterPos) {
        // Calculate direction away from player
        double dx = bobberPos.x() - shooterPos.x();
        double dz = bobberPos.z() - shooterPos.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        if (distance > 0.01) { // Avoid division by zero
            // Normalize and add horizontal velocity away from player (increased velocity)
            // Use stronger horizontal force to ensure it moves away from face
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
    
    // State management methods (from FishingBobberStateManager)
    private void startFallSequence() {
        this.shouldFallAfterHit = true;
        this.fallTicks = 0;
        this.hasAppliedFallVelocity = false;
    }

    // ===========================
    // PROJECTILE BEHAVIOR INTERFACE
    // ===========================

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

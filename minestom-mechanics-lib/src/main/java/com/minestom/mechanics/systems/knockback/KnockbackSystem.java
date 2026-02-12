package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.systems.ConfigTagWrapper;
import com.minestom.mechanics.systems.knockback.tags.KnockbackTagSerializer;
import com.minestom.mechanics.systems.knockback.tags.KnockbackTagValue;
import com.minestom.mechanics.ConfigurableSystem;
import com.minestom.mechanics.systems.misc.VelocityEstimator;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.systems.projectile.tags.ProjectileTagRegistry;
import net.minestom.server.entity.*;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import static com.minestom.mechanics.config.constants.CombatConstants.FALLING_VELOCITY_THRESHOLD;

/**
 * Knockback configuration system using unified tag approach.
 * For applying knockback, use {@link KnockbackApplicator}.
 *
 * Usage (items use Mechanics component):
 * <pre>
 * import static KnockbackTagValue.*;
 *
 * Mechanics.builder().knockback(kbMult(2.0, 1.5)).build()
 * Mechanics.builder().projectileKnockback(KB_HEAVY).build()
 * </pre>
 */
public class KnockbackSystem extends ConfigurableSystem<KnockbackConfig> {

    private static KnockbackSystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackSystem");

    private KnockbackSystem(KnockbackConfig config) {
        super(config);
    }

    // ===========================
    // INITIALIZATION
    // ===========================
    // TODO: Clean this up, move to initializablesystem
    public static KnockbackSystem initialize(KnockbackConfig config) {
        if (instance != null && instance.initialized) {
            LogUtil.logAlreadyInitialized("KnockbackSystem");
            return instance;
        }

        instance = new KnockbackSystem(config);
        instance.markInitialized();

        ProjectileTagRegistry.register(KnockbackSystem.class);
        LogUtil.logInit("KnockbackSystem");
        return instance;
    }

    public static KnockbackSystem getInstance() {
        if (instance == null || !instance.initialized) {
            throw new IllegalStateException("KnockbackSystem not initialized!");
        }
        return instance;
    }

    // ===========================
    // UNIFIED TAG SYSTEM
    // ===========================

    /** Transient knockback tag for entities/worlds (melee). */
    public static final Tag<KnockbackTagValue> CUSTOM = Tag.Transient("knockback_custom");
    /** Transient knockback tag for entities/worlds (projectile). */
    public static final Tag<KnockbackTagValue> PROJECTILE_CUSTOM = Tag.Transient("knockback_projectile_custom");
    /** Serialized knockback tag for items (melee). */
    public static final Tag<KnockbackTagValue> ITEM_CUSTOM = Tag.Structure("knockback_custom", new KnockbackTagSerializer());
    /** Serialized knockback tag for items (projectile). */
    public static final Tag<KnockbackTagValue> ITEM_PROJECTILE_CUSTOM = Tag.Structure("knockback_projectile_custom", new KnockbackTagSerializer());

    @Override
    @SuppressWarnings("unchecked")
    protected Tag<ConfigTagWrapper<KnockbackConfig>> getWrapperTag(Entity attacker) {
        return (Tag<ConfigTagWrapper<KnockbackConfig>>) (Tag<?>)
                (isProjectileAttacker(attacker) ? PROJECTILE_CUSTOM : CUSTOM);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ConfigTagWrapper<KnockbackConfig> getItemWrapper(@org.jetbrains.annotations.Nullable net.minestom.server.item.ItemStack item, Entity attacker) {
        if (item == null || item.isAir()) return null;
        return (ConfigTagWrapper<KnockbackConfig>) (ConfigTagWrapper<?>)
                item.getTag(isProjectileAttacker(attacker) ? ITEM_PROJECTILE_CUSTOM : ITEM_CUSTOM);
    }

    @Override
    protected int getComponentCount() {
        return 6; // [horizontal, vertical, sprintH, sprintV, airH, airV]
    }

    // ===========================
    // PUBLIC API
    // ===========================

    public KnockbackConfig resolveConfig(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed) {
        // For melee: derive item from hand
        ItemStack item = null;
        if (attacker instanceof Player p && handUsed != null) {
            item = handUsed == EquipmentSlot.MAIN_HAND ? p.getItemInMainHand() : p.getItemInOffHand();
        }

        KnockbackConfig base = resolveBaseConfig(attacker, victim, item);

        double[] components = resolveComponents(
                attacker,
                victim,
                item,
                config -> new double[]{
                        config.horizontal(),
                        config.vertical(),
                        config.sprintBonusHorizontal(),
                        config.sprintBonusVertical(),
                        config.airMultiplierHorizontal(),
                        config.airMultiplierVertical()
                }
        );

        return new KnockbackConfig(
                components[0], components[1],
                base.verticalLimit(),
                components[2], components[3],
                components[4], components[5],
                base.lookWeight(),
                base.modern(),
                base.knockbackSyncSupported(),
                base.meleeDirection(),
                base.projectileDirection(),
                base.degenerateFallback(),
                base.sprintLookWeight(),
                base.horizontalFriction(),
                base.verticalFriction(),
                base.velocityApplyMode(),
                base.stateOverrides()
        );
    }

    public KnockbackConfig getConfig() {
        requireInitialized();
        return serverDefaultConfig;
    }

    /**
     * Resolve config for victim state. Applies state overrides from base config.
     * Called before applying knockback to use state-specific friction, mode, or strength multipliers.
     */
    public static KnockbackConfig resolveConfigForVictim(KnockbackConfig base, LivingEntity victim) {
        var overrides = base.stateOverrides();
        if (overrides == null || overrides.isEmpty()) return base;

        KnockbackVictimState state = determineVictimState(victim);
        KnockbackStateOverride override = overrides.get(state);
        if (override == null) return base;

        if (override.fullProfile() != null) return override.fullProfile();

        // Merge partial overrides
        double hFric = override.horizontalFriction() != null ? override.horizontalFriction() : base.horizontalFriction();
        double vFric = override.verticalFriction() != null ? override.verticalFriction() : base.verticalFriction();
        VelocityApplyMode vam = override.velocityApplyMode() != null ? override.velocityApplyMode() : base.velocityApplyMode();
        Double hMult = override.horizontalMultiplier();
        Double vMult = override.verticalMultiplier();

        KnockbackConfig merged = base.withHorizontalFriction(hFric).withVerticalFriction(vFric).withVelocityApplyMode(vam);
        if (hMult != null || vMult != null) {
            merged = merged.withHorizontal(hMult != null ? base.horizontal() * hMult : base.horizontal())
                    .withVertical(vMult != null ? base.vertical() * vMult : base.vertical());
        }
        return merged;
    }

    private static KnockbackVictimState determineVictimState(LivingEntity victim) {
        if (victim.isOnGround()) return KnockbackVictimState.ON_GROUND;
        var vel = VelocityEstimator.getVelocity(victim);
        return vel.y() < FALLING_VELOCITY_THRESHOLD ? KnockbackVictimState.FALLING : KnockbackVictimState.IN_AIR;
    }

    /**
     * Simple record holding knockback strength values.
     */
    public record KnockbackStrength(double horizontal, double vertical) {}

    /**
     * Types of knockback that can be applied.
     */
    public enum KnockbackType {
        ATTACK, DAMAGE, SWEEPING, EXPLOSION, PROJECTILE
    }

    /**
     * Determines how knockback direction is calculated.
     * Configurable per server default and overridable via tags.
     * <p>
     * For position-based modes, {@code lookWeight} blends position-direction with the attacker's
     * look direction. Pure attacker look is achieved by using ATTACKER_POSITION with lookWeight=1.
     * VICTIM_FACING replaces the look source with the victim's facing; lookWeight controls how
     * much victim-facing contributes vs. the position-based component.
     */
    public enum KnockbackDirectionMode {
        /** Direction from attacker's current position to victim (melee default). */
        ATTACKER_POSITION,
        /** Direction from shooter's position at projectile launch to victim (projectile default). */
        SHOOTER_ORIGIN,
        /** Direction from projectile impact position to victim. */
        PROJECTILE_POSITION,
        /** Victim's own facing direction (knocked "forward"). */
        VICTIM_FACING
    }

    /**
     * Configurable behavior when victim and knockback origin are very close (degenerate geometry).
     */
    public enum DegenerateFallback {
        /** Use look direction when position is degenerate. */
        LOOK,
        /** Random direction when degenerate (legacy). */
        RANDOM
    }

    /**
     * How to apply computed knockback velocity.
     * Friction (including 0 = ignore old velocity) is always used when computing the result;
     * this only controls whether we SET or ADD that result.
     */
    public enum VelocityApplyMode {
        /** Replace velocity: victim.setVelocity(result) */
        SET,
        /** Add to current: victim.setVelocity(current.add(result)) */
        ADD
    }

    /**
     * Victim state when taking knockback. Used for per-state config overrides.
     */
    public enum KnockbackVictimState {
        ON_GROUND,
        IN_AIR,
        FALLING
    }

    /**
     * Optional overrides for a specific victim state. Null fields use base config.
     * If fullProfile is non-null, it replaces the entire config for that state.
     */
    public record KnockbackStateOverride(
            Double horizontalFriction,
            Double verticalFriction,
            VelocityApplyMode velocityApplyMode,
            Double horizontalMultiplier,
            Double verticalMultiplier,
            KnockbackConfig fullProfile
    ) {}
}
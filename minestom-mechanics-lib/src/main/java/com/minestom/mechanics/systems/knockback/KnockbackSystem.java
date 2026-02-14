package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.systems.ConfigTagWrapper;
import com.minestom.mechanics.systems.knockback.tags.KnockbackTagSerializer;
import com.minestom.mechanics.systems.knockback.tags.KnockbackTagValue;
import com.minestom.mechanics.ConfigurableSystem;
import com.minestom.mechanics.systems.misc.VelocityEstimator;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.systems.projectile.tags.ProjectileTagRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
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

    /** Last tick we observed player sprinting. Updated each tick when sprinting. */
    public static final Tag<Long> LAST_SPRINT_TICK = Tag.Transient("knockback_last_sprint_tick");

    private long currentTick = 0;

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

        // Tick: update currentTick and last sprint tick for players
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            instance.currentTick++;
            for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                if (p.isSprinting()) p.setTag(LAST_SPRINT_TICK, instance.currentTick);
            }
        }).repeat(TaskSchedule.tick(1)).schedule();

        ProjectileTagRegistry.register(KnockbackSystem.class);
        LogUtil.logInit("KnockbackSystem");
        return instance;
    }

    /** Whether to treat this as a sprint hit for knockback: currently sprinting or within buffer. */
    public static boolean isSprintHit(Player attacker, int sprintBufferTicks, long currentTick) {
        if (attacker.isSprinting()) return true;
        if (sprintBufferTicks <= 0) return false;
        Long last = attacker.getTag(LAST_SPRINT_TICK);
        return last != null && (currentTick - last) <= sprintBufferTicks;
    }

    public long getCurrentTick() { return currentTick; }

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

        // Scale vertical limit when item/entity mult or modify amplifies vertical strength.
        // Otherwise high-vertical items (Sky Ball, Cannon Bow, KB_LAUNCHER) get crushed by base limit.
        double baseLimit = base.verticalLimit();
        double[] mults = getMultipliers(attacker, victim, item);
        double verticalMult = mults.length > 1 ? mults[1] : 1.0;
        double verticalModify = getModifyValue(attacker, victim, item, 1);
        double effectiveVerticalLimit = baseLimit * Math.max(1.0, verticalMult) + Math.max(0, verticalModify);

        return new KnockbackConfig(
                components[0], components[1],
                effectiveVerticalLimit,
                components[2], components[3],
                components[4], components[5],
                base.lookWeight(),
                base.modern(),
                base.knockbackSyncSupported(),
                base.meleeDirection(),
                base.projectileDirection(),
                base.degenerateFallback(),
                base.directionBlendMode(),
                base.sprintLookWeight(),
                base.horizontalFriction(),
                base.verticalFriction(),
                base.sprintHorizontalFriction(),
                base.sprintVerticalFriction(),
                base.velocityApplyMode(),
                base.stateOverrides(),
                base.rangeReduction(),
                base.sprintRangeReduction(),
                base.sprintBufferTicks()
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
     * Debug info for knockback tuning. Populated when debug is enabled.
     */
    public record KnockbackDebugInfo(
            Vec oldVelocity,
            Vec postKnockbackVelocity,
            Vec preSprintBonusVector,
            Double verticalPreLimit,
            Double verticalLimitApplied,
            Double rangeDistance,
            Double rangeReductionH,
            Double rangeReductionV
    ) {}

    private static volatile boolean debugToChat = false;

    /** Enable/disable knockback velocity debug output (prints to attacker and victim chat). */
    public static void setDebugToChat(boolean enabled) {
        debugToChat = enabled;
    }

    public static boolean isDebugToChat() {
        return debugToChat;
    }

    /**
     * Range reduction config: start distances, factors, and max reduction per axis.
     * maxHorizontal/maxVertical cap the reduction; use Double.POSITIVE_INFINITY for no cap.
     */
    public record RangeReductionConfig(
            double startDistanceHorizontal,
            double startDistanceVertical,
            double factorHorizontal,
            double factorVertical,
            double maxHorizontal,
            double maxVertical
    ) {
        public static RangeReductionConfig none() {
            return new RangeReductionConfig(0, 0, 0, 0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }
    }

    /**
     * Full context for a knockback event. Resolved by the applicator, consumed by the calculator.
     */
    public record KnockbackContext(
            LivingEntity victim,
            @Nullable Entity attacker,
            @Nullable Entity source,
            @Nullable Pos shooterOriginPos,
            KnockbackType type,
            boolean wasSprinting,
            int kbEnchantLevel,
            KnockbackConfig config
    ) {}

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
     * How to combine look and position directions with horizontal magnitude.
     * BLEND_DIRECTION: blend unit vectors by weight, then multiply by scalar (current behavior).
     * ADD_VECTORS: scale each direction by its fraction of magnitude, add vectors (different feel).
     */
    public enum DirectionBlendMode {
        /** Blend direction vectors, then multiply by horizontal scalar. */
        BLEND_DIRECTION,
        /** Add (lookMag * lookDir) + (positionMag * positionDir) where magnitudes sum to horizontal. */
        ADD_VECTORS
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
package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.systems.knockback.tags.KnockbackTagSerializer;
import com.minestom.mechanics.systems.knockback.tags.KnockbackTagValue;
import com.minestom.mechanics.systems.util.ConfigTagWrapper;
import com.minestom.mechanics.systems.util.ConfigurableSystem;
import com.minestom.mechanics.systems.util.LogUtil;
import com.minestom.mechanics.systems.util.ProjectileTagRegistry;
import net.minestom.server.entity.*;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Knockback configuration system using unified tag approach.
 * For applying knockback, use {@link KnockbackApplicator}.
 *
 * Usage:
 * <pre>
 * import static KnockbackTagValue.*;
 *
 * // Simple
 * item.withTag(KnockbackSystem.CUSTOM, kbMult(2.0, 1.5))
 *
 * // Combined
 * item.withTag(KnockbackSystem.CUSTOM, kbMult(2.0).thenAdd(0.5))
 *
 * // Presets
 * item.withTag(KnockbackSystem.CUSTOM, KB_HEAVY)
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

    /** Unified knockback tag for melee/direct damage - USING CUSTOM SERIALIZER */
    public static final Tag<KnockbackTagValue> CUSTOM =
            Tag.Structure("knockback_custom", new KnockbackTagSerializer());

    /** Unified knockback tag for projectile damage - USING CUSTOM SERIALIZER */
    public static final Tag<KnockbackTagValue> PROJECTILE_CUSTOM =
            Tag.Structure("knockback_projectile_custom", new KnockbackTagSerializer());

    @Override
    @SuppressWarnings("unchecked")
    protected Tag<ConfigTagWrapper<KnockbackConfig>> getWrapperTag(Entity attacker) {
        return (Tag<ConfigTagWrapper<KnockbackConfig>>) (Tag<?>)
                (isProjectileAttacker(attacker) ? PROJECTILE_CUSTOM : CUSTOM);
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
                serverDefaultConfig.verticalLimit(),
                components[2], components[3],
                components[4], components[5],
                serverDefaultConfig.lookWeight(),
                serverDefaultConfig.modern(),
                serverDefaultConfig.knockbackSyncSupported()
        );
    }

    public KnockbackConfig getConfig() {
        requireInitialized();
        return serverDefaultConfig;
    }
}
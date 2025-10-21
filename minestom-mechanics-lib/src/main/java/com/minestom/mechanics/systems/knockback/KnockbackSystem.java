package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.config.knockback.KnockbackTagValue;
import com.minestom.mechanics.util.ConfigTagWrapper;
import com.minestom.mechanics.util.ConfigurableSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.util.ProjectileTagRegistry;
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
 * item.withTag(KnockbackSystem.CUSTOM, mult(2.0, 1.5))
 *
 * // Combined
 * item.withTag(KnockbackSystem.CUSTOM, mult(2.0).thenAdd(0.5))
 *
 * // Presets
 * item.withTag(KnockbackSystem.CUSTOM, HEAVY)
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

    /** Unified knockback tag for melee/direct damage - USING STRUCTURE */
    public static final Tag<KnockbackTagValue> CUSTOM =
            Tag.Structure("knockback_custom", KnockbackTagValue.class);

    /** Unified knockback tag for projectile damage - USING STRUCTURE */
    public static final Tag<KnockbackTagValue> PROJECTILE_CUSTOM =
            Tag.Structure("knockback_projectile_custom", KnockbackTagValue.class);

    @Override
    protected Tag<ConfigTagWrapper<KnockbackConfig>> getWrapperTag(Entity attacker) {
        return isProjectileAttacker(attacker) ? PROJECTILE_CUSTOM : CUSTOM;
    }

    @Override
    protected int getComponentCount() {
        return 6; // [horizontal, vertical, sprintH, sprintV, airH, airV]
    }

    // ===========================
    // OVERRIDE: Use registry for projectile base config
    // ===========================

    @Override
    protected KnockbackConfig resolveBaseConfig(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed) {
        Tag<ConfigTagWrapper<KnockbackConfig>> wrapperTag = getWrapperTag(attacker);

        // DEBUG: Check item tag
        if (attacker instanceof Player p && handUsed != null) {
            ItemStack item = handUsed == EquipmentSlot.MAIN_HAND ? p.getItemInMainHand() : p.getItemInOffHand();
            ConfigTagWrapper<KnockbackConfig> wrapper = item.getTag(wrapperTag);

            System.out.println("DEBUG - Item: " + item.material());
            System.out.println("DEBUG - Wrapper: " + wrapper);
            System.out.println("DEBUG - Wrapper mult: " + (wrapper != null ? wrapper.getMultiplier() : "null"));
        }

        return super.resolveBaseConfig(attacker, victim, handUsed);
    }

    // ===========================
    // PUBLIC API
    // ===========================

    public KnockbackConfig resolveConfig(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed) {
        double[] components = resolveComponents(
                attacker,
                victim,
                handUsed,
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
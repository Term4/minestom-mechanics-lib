package com.minestom.mechanics.util;

import net.minestom.server.entity.*;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract base for mechanics systems using the Universal Config pattern.
 * <p>
 * Systems provide a unified ConfigTagWrapper that can hold:
 * <ul>
 *   <li><b>MULTIPLIER</b> - Multiplicative scaling (item × player × world)</li>
 *   <li><b>MODIFY</b> - Additive component changes (item + player + world)</li>
 *   <li><b>CUSTOM</b> - Complete config override (item > player > world > server)</li>
 * </ul>
 *
 * <p><b>Priority Chain:</b> Item > Player > World > Server Default</p>
 *
 * <p><b>Application Order:</b></p>
 * <ol>
 *   <li>Resolve base config (check CUSTOM from wrapper: item > player > world > server)</li>
 *   <li>Apply MODIFY from wrapper (additive)</li>
 *   <li>Apply MULTIPLIER from wrapper (multiplicative)</li>
 * </ol>
 *
 * @param <TConfig> The configuration type for this system (e.g., KnockbackConfig)
 */
public abstract class ConfigurableSystem<TConfig> extends InitializableSystem {

    /** Server default configuration (fallback when no custom configs are set) */
    protected final TConfig serverDefaultConfig;

    protected ConfigurableSystem(TConfig serverDefaultConfig) {
        this.serverDefaultConfig = serverDefaultConfig;
    }

    // ===========================
    // SUBCLASSES MUST IMPLEMENT
    // ===========================

    /**
     * Get the wrapper tag for this system based on attacker type.
     * Return the appropriate tag (normal or projectile).
     *
     * Example:
     * return isProjectileAttacker(attacker) ? PROJECTILE_CUSTOM : CUSTOM;
     */
    protected abstract Tag<ConfigTagWrapper<TConfig>> getWrapperTag(Entity attacker);

    /**
     * Get the number of components in arrays for this system.
     * Example: 6 for knockback (horizontal, vertical, sprintBonusH, sprintBonusV, airMultH, airMultV)
     */
    protected abstract int getComponentCount();

    /**
     * Check if attacker is a projectile entity
     */
    protected boolean isProjectileAttacker(Entity attacker) {
        EntityType type = attacker.getEntityType();
        return type == EntityType.ARROW
                || type == EntityType.SPECTRAL_ARROW
                || type == EntityType.TRIDENT
                || type == EntityType.EGG
                || type == EntityType.SNOWBALL
                || type == EntityType.ENDER_PEARL;
    }

    // ===========================
    // CORE RESOLUTION LOGIC
    // ===========================

    /**
     * Resolve base config from wrapper's CUSTOM field with priority chain.
     * <p>Priority: Item (specified hand) > Player > World > Server Default</p>
     */
    protected TConfig resolveBaseConfig(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed) {
        Tag<ConfigTagWrapper<TConfig>> wrapperTag = getWrapperTag(attacker);
        ConfigTagWrapper<TConfig> wrapper;

        // 1. Check attacker entity
        wrapper = attacker.getTag(wrapperTag);
        if (wrapper != null && wrapper.getCustom() != null) return wrapper.getCustom();

        // 2. Check item (if player melee)
        if (attacker instanceof Player p && handUsed != null) {
            ItemStack item = handUsed == EquipmentSlot.MAIN_HAND ? p.getItemInMainHand() : p.getItemInOffHand();
            if (!item.isAir()) {
                wrapper = item.getTag(wrapperTag);
                if (wrapper != null && wrapper.getCustom() != null) return wrapper.getCustom();
            }
        }

        // 3. Check player
        if (attacker instanceof Player p) {
            wrapper = p.getTag(wrapperTag);
            if (wrapper != null && wrapper.getCustom() != null) return wrapper.getCustom();
        }

        // 4. Check world
        wrapper = victim.getInstance().getTag(wrapperTag);
        if (wrapper != null && wrapper.getCustom() != null) return wrapper.getCustom();

        // 5. Server default
        return serverDefaultConfig;
    }

    /**
     * Get MODIFY value for a specific component index from all sources.
     * Values stack additively: attacker + item + player + world
     */
    protected double getModifyValue(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed, int index) {
        if (index >= getComponentCount()) {
            throw new IllegalArgumentException("Invalid component index " + index + " (max: " + (getComponentCount() - 1) + ")");
        }

        Tag<ConfigTagWrapper<TConfig>> wrapperTag = getWrapperTag(attacker);
        double total = 0.0;
        ConfigTagWrapper<TConfig> wrapper;
        List<Double> modify;

        // 1. Attacker entity
        wrapper = attacker.getTag(wrapperTag);
        if (wrapper != null && (modify = wrapper.getModify()) != null && modify.size() > index) {
            total += modify.get(index);
        }

        // 2. Item (if player melee)
        if (attacker instanceof Player p && handUsed != null) {
            ItemStack item = handUsed == EquipmentSlot.MAIN_HAND ? p.getItemInMainHand() : p.getItemInOffHand();
            if (!item.isAir()) {
                wrapper = item.getTag(wrapperTag);
                if (wrapper != null && (modify = wrapper.getModify()) != null && modify.size() > index) {
                    total += modify.get(index);
                }
            }
        }

        // 3. Player
        if (attacker instanceof Player p) {
            wrapper = p.getTag(wrapperTag);
            if (wrapper != null && (modify = wrapper.getModify()) != null && modify.size() > index) {
                total += modify.get(index);
            }
        }

        // 4. World
        wrapper = victim.getInstance().getTag(wrapperTag);
        if (wrapper != null && (modify = wrapper.getModify()) != null && modify.size() > index) {
            total += modify.get(index);
        }

        return total;
    }

    /**
     * Get combined MULTIPLIER from all sources.
     * Multipliers stack multiplicatively: attacker × item × player × world
     */
    protected double[] getMultipliers(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed) {
        Tag<ConfigTagWrapper<TConfig>> wrapperTag = getWrapperTag(attacker);
        int componentCount = getComponentCount();
        double[] multipliers = new double[componentCount];
        Arrays.fill(multipliers, 1.0);

        ConfigTagWrapper<TConfig> wrapper;
        List<Double> mults;

        // 1. Attacker entity
        wrapper = attacker.getTag(wrapperTag);
        if (wrapper != null && (mults = wrapper.getMultiplier()) != null) {
            for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                multipliers[i] *= mults.get(i);
            }
        }

        // 2. Item (if player melee)
        if (attacker instanceof Player p && handUsed != null) {
            ItemStack item = handUsed == EquipmentSlot.MAIN_HAND ? p.getItemInMainHand() : p.getItemInOffHand();
            if (!item.isAir()) {
                wrapper = item.getTag(wrapperTag);
                if (wrapper != null && (mults = wrapper.getMultiplier()) != null) {
                    for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                        multipliers[i] *= mults.get(i);
                    }
                }
            }
        }

        // 3. Player
        if (attacker instanceof Player p) {
            wrapper = p.getTag(wrapperTag);
            if (wrapper != null && (mults = wrapper.getMultiplier()) != null) {
                for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                    multipliers[i] *= mults.get(i);
                }
            }
        }

        // 4. World
        wrapper = victim.getInstance().getTag(wrapperTag);
        if (wrapper != null && (mults = wrapper.getMultiplier()) != null) {
            for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                multipliers[i] *= mults.get(i);
            }
        }

        return multipliers;
    }

    // ===========================
    // CONVENIENCE METHODS
    // ===========================

    /**
     * Apply MODIFY values to a component array (in-place).
     */
    protected void applyModifyToComponents(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed, double[] components) {
        for (int i = 0; i < components.length && i < getComponentCount(); i++) {
            components[i] += getModifyValue(attacker, victim, handUsed, i);
        }
    }

    /**
     * Apply MULTIPLIER to a component array (in-place).
     */
    protected void applyMultiplierToComponents(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed, double[] components) {
        double[] multipliers = getMultipliers(attacker, victim, handUsed);
        for (int i = 0; i < components.length; i++) {
            components[i] *= multipliers[i];
        }
    }

    /**
     * Complete resolution: extract components from config, apply MODIFY, apply MULTIPLIER.
     * <p>This is a helper method that combines all resolution steps:</p>
     * <ol>
     *   <li>Resolve base config</li>
     *   <li>Extract components via the provided extractor</li>
     *   <li>Apply MODIFY values</li>
     *   <li>Apply MULTIPLIER</li>
     * </ol>
     */
    protected double[] resolveComponents(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed,
                                         ComponentExtractor<TConfig> componentExtractor) {
        // 1. Resolve base config
        TConfig config = resolveBaseConfig(attacker, victim, handUsed);

        // 2. Extract components
        double[] components = componentExtractor.extract(config);

        // 3. Apply MODIFY
        applyModifyToComponents(attacker, victim, handUsed, components);

        // 4. Apply MULTIPLIER
        applyMultiplierToComponents(attacker, victim, handUsed, components);

        return components;
    }

    /**
     * Functional interface for extracting component arrays from configs.
     */
    @FunctionalInterface
    protected interface ComponentExtractor<T> {
        double[] extract(T config);
    }
}
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
 * Provides tag resolution logic following the 3-tag pattern:
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
 *   <li>Resolve base config (check CUSTOM tag: item > player > world > server)</li>
 *   <li>Apply MODIFY from all sources (additive)</li>
 *   <li>Apply MULTIPLIER from all sources (multiplicative)</li>
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
     * Get the MULTIPLIER tag for this system.
     * Example: {@code Tag.Double("knockback_multiplier")}
     */
    protected abstract Tag<List<Double>> getMultiplierTag();

    /**
     * Get the MODIFY tag for this system.
     * Example: {@code Tag.Double("knockback_modify").list()}
     */
    protected abstract Tag<List<Double>> getModifyTag();

    /**
     * Get the CUSTOM tag for this system.
     * Example: {@code Tag.Transient("knockback_custom")}
     */
    protected abstract Tag<TConfig> getCustomTag();

    /**
     * Abstract methods for projectile tags
     */
    protected abstract Tag<List<Double>> getProjectileMultiplierTag();
    protected abstract Tag<List<Double>> getProjectileModifyTag();
    protected abstract Tag<TConfig> getProjectileCustomTag();

    /**
     * Check if attacker is a projectile entity
     */
    protected boolean isProjectileAttacker(Entity attacker) {
        EntityType type = attacker.getEntityType();

        // ✅ ADD DEBUG
        System.out.println("Checking if projectile: " + type + " (" + attacker.getClass().getSimpleName() + ")");

        return type == EntityType.ARROW
                || type == EntityType.SPECTRAL_ARROW
                || type == EntityType.TRIDENT
                || type == EntityType.EGG
                || type == EntityType.SNOWBALL
                || type == EntityType.ENDER_PEARL;
    }

    /**
     * Get the appropriate tag based on attacker type.
     * Uses PROJECTILE_* tags if attacker is a projectile, otherwise uses regular tags.
     */
    protected Tag<List<Double>> getEffectiveMultiplierTag(Entity attacker) {
        return isProjectileAttacker(attacker) ? getProjectileMultiplierTag() : getMultiplierTag();
    }

    protected Tag<List<Double>> getEffectiveModifyTag(Entity attacker) {
        return isProjectileAttacker(attacker) ? getProjectileModifyTag() : getModifyTag();
    }

    protected Tag<TConfig> getEffectiveCustomTag(Entity attacker) {
        return isProjectileAttacker(attacker) ? getProjectileCustomTag() : getCustomTag();
    }

    /**
     * Get the number of components in the MODIFY array for this system.
     * Example: 6 for knockback (horizontal, vertical, sprintBonusH, sprintBonusV, airMultH, airMultV)
     */
    protected abstract int getModifyComponentCount();

    // ===========================
    // CORE RESOLUTION LOGIC
    // ===========================

    /**
     * Resolve base config from CUSTOM tag with priority chain.
     * <p>Priority: Item (specified hand) > Player > World > Server Default</p>
     *
     * @param attacker The attacking entity (for item/player tags)
     * @param victim The victim entity (for world tag)
     * @param handUsed The hand that was used for this action (null for non-item actions)
     * @return The resolved base configuration
     */
    protected TConfig resolveBaseConfig(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed) {
        Tag<TConfig> customTag = getEffectiveCustomTag(attacker);
        TConfig custom;

        // ✅ ADD DEBUG
        System.out.println("=== RESOLVE DEBUG ===");
        System.out.println("Attacker: " + attacker.getEntityType());
        System.out.println("Is projectile? " + isProjectileAttacker(attacker));
        System.out.println("Using tag: " + (isProjectileAttacker(attacker) ? "PROJECTILE" : "NORMAL"));

        // Check attacker entity CUSTOM (highest priority - works for projectiles!)
        custom = attacker.getTag(customTag);
        if (custom != null) return custom;

        // Check item CUSTOM - only the hand that was actually used
        if (attacker instanceof Player p && handUsed != null) {
            ItemStack item = handUsed == EquipmentSlot.MAIN_HAND
                    ? p.getItemInMainHand()
                    : p.getItemInOffHand();

            if (!item.isAir()) {
                custom = item.getTag(customTag);
                if (custom != null) return custom;
            }
        }

        // Check player CUSTOM
        if (attacker instanceof Player p) {
            custom = p.getTag(customTag);
            if (custom != null) return custom;
        }

        // Check world CUSTOM
        custom = victim.getInstance().getTag(customTag);
        if (custom != null) return custom;

        // Server default (lowest priority)
        return serverDefaultConfig;
    }

    /**
     * Get MODIFY value for a specific component index from all sources.
     * <p>Values stack additively: item (specified hand) + player + world</p>
     *
     * @param attacker The attacking entity (for item/player tags)
     * @param victim The victim entity (for world tag)
     * @param handUsed The hand that was used for this action (null for non-item actions)
     * @param index Component index in the MODIFY array
     * @return Sum of all MODIFY values at the given index
     * @throws IllegalArgumentException if index >= getModifyComponentCount()
     */
    protected double getModifyValue(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed, int index) {
        if (index >= getModifyComponentCount()) {
            throw new IllegalArgumentException(
                    "Invalid component index " + index + " (max: " + (getModifyComponentCount() - 1) + ")"
            );
        }

        Tag<List<Double>> modifyTag = getEffectiveModifyTag(attacker);
        double total = 0.0;
        List<Double> modify; // ✅ Declare once

        // Check attacker entity MODIFY first (works for projectiles!)
        modify = attacker.getTag(modifyTag); // ✅ Reuse
        if (modify != null && modify.size() > index) {
            total += modify.get(index);
        }

        // Item MODIFY - only the hand that was actually used
        if (attacker instanceof Player p && handUsed != null) {
            ItemStack item = handUsed == EquipmentSlot.MAIN_HAND
                    ? p.getItemInMainHand()
                    : p.getItemInOffHand();

            if (!item.isAir()) {
                modify = item.getTag(modifyTag); // ✅ Reuse
                if (modify != null && modify.size() > index) {
                    total += modify.get(index);
                }
            }
        }

        // Player MODIFY
        if (attacker instanceof Player p) {
            modify = p.getTag(modifyTag); // ✅ Reuse
            if (modify != null && modify.size() > index) {
                total += modify.get(index);
            }
        }

        // World MODIFY
        modify = victim.getInstance().getTag(modifyTag); // ✅ Reuse
        if (modify != null && modify.size() > index) {
            total += modify.get(index);
        }

        return total;
    }

    /**
     * Get combined MULTIPLIER from all sources.
     * <p>Multipliers stack multiplicatively: item (specified hand) × player × world</p>
     *
     * @param attacker The attacking entity (for item/player tags)
     * @param victim The victim entity (for world tag)
     * @param handUsed The hand that was used for this action (null for non-item actions)
     * @return Product of all multipliers (1.0 if none set)
     */
    protected double[] getMultipliers(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed, int componentCount) {
        Tag<List<Double>> multiplierTag = getEffectiveMultiplierTag(attacker); // Use effective tag based on attacker type
        double[] multipliers = new double[componentCount];
        Arrays.fill(multipliers, 1.0); // Default to 1.0 (no change)

        // 1. Attacker entity MULTIPLIER
        List<Double> mults = attacker.getTag(multiplierTag);
        if (mults != null) {
            for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                multipliers[i] *= mults.get(i);
            }
        }

        // 2. Item MULTIPLIER (only for Player melee)
        if (attacker instanceof Player p && handUsed != null) {
            ItemStack item = handUsed == EquipmentSlot.MAIN_HAND
                    ? p.getItemInMainHand()
                    : p.getItemInOffHand();

            if (!item.isAir()) {
                mults = item.getTag(multiplierTag);
                if (mults != null) {
                    for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                        multipliers[i] *= mults.get(i);
                    }
                }
            }
        }

        // 3. Player MULTIPLIER
        if (attacker instanceof Player p) {
            mults = p.getTag(multiplierTag);
            if (mults != null) {
                for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                    multipliers[i] *= mults.get(i);
                }
            }
        }

        // 4. World MULTIPLIER
        mults = victim.getInstance().getTag(multiplierTag);
        if (mults != null) {
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
     * Apply MODIFY values to a component array.
     * <p>Modifies the array in-place by adding MODIFY values for each component.</p>
     *
     * @param attacker The attacking entity
     * @param victim The victim entity
     * @param handUsed The hand that was used for this action (null for non-item actions)
     * @param components Array of component values to modify
     */
    protected void applyModifyToComponents(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed, double[] components) {
        for (int i = 0; i < components.length && i < getModifyComponentCount(); i++) {
            components[i] += getModifyValue(attacker, victim, handUsed, i);
        }
    }

    /**
     * Apply MULTIPLIER to a component array.
     * <p>Modifies the array in-place by multiplying all components by the combined multiplier.</p>
     *
     * @param attacker The attacking entity
     * @param victim The victim entity
     * @param handUsed The hand that was used for this action (null for non-item actions)
     * @param components Array of component values to multiply
     */
    protected void applyMultiplierToComponents(Entity attacker, LivingEntity victim, @Nullable EquipmentSlot handUsed, double[] components) {
        double[] multipliers = getMultipliers(attacker, victim, handUsed, components.length);
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
     *
     * @param attacker The attacking entity
     * @param victim The victim entity
     * @param handUsed The hand that was used for this action (null for non-item actions)
     * @param componentExtractor Function to extract component array from config
     * @return Modified component array
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
     *
     * @param <T> The configuration type
     */
    @FunctionalInterface
    protected interface ComponentExtractor<T> {
        /**
         * Extract component values from a config as an array.
         *
         * @param config The configuration object
         * @return Array of component values
         */
        double[] extract(T config);
    }
}
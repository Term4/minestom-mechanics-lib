package com.minestom.mechanics;

import com.minestom.mechanics.systems.ConfigTagWrapper;
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
 *   <li><b>MULTIPLIER</b> - Multiplicative scaling (item × attacker × player × world)</li>
 *   <li><b>MODIFY</b> - Additive component changes (item + attacker + player + world)</li>
 *   <li><b>CUSTOM</b> - Complete config override (item > attacker > player > world > server)</li>
 * </ul>
 *
 * <p><b>Priority Chain:</b> Item > Attacker Entity > Player (if attacker is player) > World > Server Default</p>
 *
 * <p><b>Application Order:</b></p>
 * <ol>
 *   <li>Resolve base config (check CUSTOM from wrapper: item > attacker > player > world > server)</li>
 *   <li>Apply MODIFY from wrapper (additive)</li>
 *   <li>Apply MULTIPLIER from wrapper (multiplicative)</li>
 * </ol>
 *
 * @param <TConfig> The configuration type for this system (e.g., KnockbackConfig, ProjectileVelocityConfig)
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
     * <p>Priority: Item > Attacker Entity > Player (if attacker is player) > World > Server Default</p>
     *
     * @param attacker The attacking entity (player for melee, projectile for ranged, null for environmental damage)
     * @param victim The victim entity (null during projectile spawn/velocity calculation)
     * @param item The item being used (weapon for melee, bow/throwable for projectiles, null if none)
     */
    protected TConfig resolveBaseConfig(Entity attacker, @Nullable LivingEntity victim, @Nullable ItemStack item) {
        // For environmental damage (no attacker), we need to get the wrapper tag differently
        Tag<ConfigTagWrapper<TConfig>> wrapperTag;
        if (attacker != null) {
            wrapperTag = getWrapperTag(attacker);
        } else {
            // For environmental damage, use victim to determine tag (or a default)
            // We'll need to handle this case - for now, use victim if it's a player
            if (victim instanceof Player) {
                wrapperTag = getWrapperTag(victim);
            } else if (victim != null) {
                // Use victim as the entity to determine tag type
                wrapperTag = getWrapperTag(victim);
            } else {
                // Fallback - this shouldn't happen in practice
                wrapperTag = getWrapperTag(null);
            }
        }
        
        ConfigTagWrapper<TConfig> wrapper;

        // 1. Check item FIRST (highest priority)
        if (item != null && !item.isAir()) {
            wrapper = item.getTag(wrapperTag);
            if (wrapper != null && wrapper.getCustom() != null) return wrapper.getCustom();
        }

        // 2. Check attacker entity (if attacker exists)
        if (attacker != null) {
            wrapper = attacker.getTag(wrapperTag);
            if (wrapper != null && wrapper.getCustom() != null) return wrapper.getCustom();

            // 3. Check player (if attacker is player)
            if (attacker instanceof Player p) {
                wrapper = p.getTag(wrapperTag);
                if (wrapper != null && wrapper.getCustom() != null) return wrapper.getCustom();
            }
        }

        // 4. Check victim entity (for environmental damage, victim is the source of config)
        if (victim != null) {
            wrapper = victim.getTag(wrapperTag);
            if (wrapper != null && wrapper.getCustom() != null) return wrapper.getCustom();
        }

        // 5. Check world (only if victim exists)
        if (victim != null && victim.getInstance() != null) {
            wrapper = victim.getInstance().getTag(wrapperTag);
            if (wrapper != null && wrapper.getCustom() != null) return wrapper.getCustom();
        }

        // 6. Server default
        return serverDefaultConfig;
    }

    /**
     * Get MODIFY value for a specific component index from all sources.
     * Values stack additively: item + attacker + player + world
     *
     * @param attacker The attacking entity (null for environmental damage)
     * @param victim The victim entity (null during projectile spawn/velocity calculation)
     * @param item The item being used (null if none)
     * @param index Component index to retrieve
     */
    protected double getModifyValue(Entity attacker, @Nullable LivingEntity victim, @Nullable ItemStack item, int index) {
        if (index >= getComponentCount()) {
            throw new IllegalArgumentException("Invalid component index " + index + " (max: " + (getComponentCount() - 1) + ")");
        }

        Tag<ConfigTagWrapper<TConfig>> wrapperTag;
        if (attacker != null) {
            wrapperTag = getWrapperTag(attacker);
        } else if (victim != null) {
            wrapperTag = getWrapperTag(victim);
        } else {
            wrapperTag = getWrapperTag(null);
        }
        
        double total = 0.0;
        ConfigTagWrapper<TConfig> wrapper;
        List<Double> modify;

        // 1. Item
        if (item != null && !item.isAir()) {
            wrapper = item.getTag(wrapperTag);
            if (wrapper != null && (modify = wrapper.getModify()) != null && modify.size() > index) {
                total += modify.get(index);
            }
        }

        // 2. Attacker entity (if attacker exists)
        if (attacker != null) {
            wrapper = attacker.getTag(wrapperTag);
            if (wrapper != null && (modify = wrapper.getModify()) != null && modify.size() > index) {
                total += modify.get(index);
            }

            // 3. Player (if attacker is player)
            if (attacker instanceof Player p) {
                wrapper = p.getTag(wrapperTag);
                if (wrapper != null && (modify = wrapper.getModify()) != null && modify.size() > index) {
                    total += modify.get(index);
                }
            }
        }

        // 4. Victim entity (for environmental damage)
        if (victim != null) {
            wrapper = victim.getTag(wrapperTag);
            if (wrapper != null && (modify = wrapper.getModify()) != null && modify.size() > index) {
                total += modify.get(index);
            }
        }

        // 5. World (only if victim exists)
        if (victim != null && victim.getInstance() != null) {
            wrapper = victim.getInstance().getTag(wrapperTag);
            if (wrapper != null && (modify = wrapper.getModify()) != null && modify.size() > index) {
                total += modify.get(index);
            }
        }

        return total;
    }

    /**
     * Get combined MULTIPLIER from all sources.
     * Multipliers stack multiplicatively: item × attacker × player × world
     *
     * @param attacker The attacking entity (null for environmental damage)
     * @param victim The victim entity (null during projectile spawn/velocity calculation)
     * @param item The item being used (null if none)
     */
    protected double[] getMultipliers(Entity attacker, @Nullable LivingEntity victim, @Nullable ItemStack item) {
        Tag<ConfigTagWrapper<TConfig>> wrapperTag;
        if (attacker != null) {
            wrapperTag = getWrapperTag(attacker);
        } else if (victim != null) {
            wrapperTag = getWrapperTag(victim);
        } else {
            wrapperTag = getWrapperTag(null);
        }
        
        int componentCount = getComponentCount();
        double[] multipliers = new double[componentCount];
        Arrays.fill(multipliers, 1.0);

        ConfigTagWrapper<TConfig> wrapper;
        List<Double> mults;

        // 1. Item
        if (item != null && !item.isAir()) {
            wrapper = item.getTag(wrapperTag);
            if (wrapper != null && (mults = wrapper.getMultiplier()) != null) {
                for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                    multipliers[i] *= mults.get(i);
                }
            }
        }

        // 2. Attacker entity (if attacker exists)
        if (attacker != null) {
            wrapper = attacker.getTag(wrapperTag);
            if (wrapper != null && (mults = wrapper.getMultiplier()) != null) {
                for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                    multipliers[i] *= mults.get(i);
                }
            }

            // 3. Player (if attacker is player)
            if (attacker instanceof Player p) {
                wrapper = p.getTag(wrapperTag);
                if (wrapper != null && (mults = wrapper.getMultiplier()) != null) {
                    for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                        multipliers[i] *= mults.get(i);
                    }
                }
            }
        }

        // 4. Victim entity (for environmental damage)
        if (victim != null) {
            wrapper = victim.getTag(wrapperTag);
            if (wrapper != null && (mults = wrapper.getMultiplier()) != null) {
                for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                    multipliers[i] *= mults.get(i);
                }
            }
        }

        // 5. World (only if victim exists)
        if (victim != null && victim.getInstance() != null) {
            wrapper = victim.getInstance().getTag(wrapperTag);
            if (wrapper != null && (mults = wrapper.getMultiplier()) != null) {
                for (int i = 0; i < Math.min(mults.size(), componentCount); i++) {
                    multipliers[i] *= mults.get(i);
                }
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
    protected void applyModifyToComponents(Entity attacker, @Nullable LivingEntity victim, @Nullable ItemStack item, double[] components) {
        for (int i = 0; i < components.length && i < getComponentCount(); i++) {
            components[i] += getModifyValue(attacker, victim, item, i);
        }
    }

    /**
     * Apply MULTIPLIER to a component array (in-place).
     */
    protected void applyMultiplierToComponents(Entity attacker, @Nullable LivingEntity victim, @Nullable ItemStack item, double[] components) {
        double[] multipliers = getMultipliers(attacker, victim, item);
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
     * @param attacker The attacking entity (player for melee, projectile for ranged)
     * @param victim The victim entity (null during projectile spawn/velocity calculation)
     * @param item The item being used (weapon/bow/throwable, null if none)
     * @param componentExtractor Function to extract component array from config
     */
    protected double[] resolveComponents(Entity attacker, @Nullable LivingEntity victim, @Nullable ItemStack item,
                                         ComponentExtractor<TConfig> componentExtractor) {
        // 1. Resolve base config
        TConfig config = resolveBaseConfig(attacker, victim, item);

        // 2. Extract components
        double[] components = componentExtractor.extract(config);

        // 3. Apply MODIFY
        applyModifyToComponents(attacker, victim, item, components);

        // 4. Apply MULTIPLIER
        applyMultiplierToComponents(attacker, victim, item, components);

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
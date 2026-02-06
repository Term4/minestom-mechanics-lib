package com.minestom.mechanics.systems.health.damagetypes;

import com.minestom.mechanics.ConfigurableSystem;
import com.minestom.mechanics.systems.ConfigTagWrapper;
import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.systems.health.HealthEvent;
import com.minestom.mechanics.systems.health.tags.HealthTagValue;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for all damage types.
 * Extends ConfigurableSystem to use the unified tag resolution pattern.
 * Provides common functionality for damage calculation, enable/disable, multipliers, etc.
 */
public abstract class AbstractDamageType extends ConfigurableSystem<HealthTagValue> {
    
    protected final HealthConfig config;
    protected final String damageTypeName;
    protected final Tag<com.minestom.mechanics.systems.health.tags.HealthTagWrapper> wrapperTag;
    
    protected AbstractDamageType(HealthConfig config, String damageTypeName, Tag<com.minestom.mechanics.systems.health.tags.HealthTagWrapper> wrapperTag) {
        // Create default config from HealthConfig
        super(createDefaultConfig(config, damageTypeName));
        this.config = config;
        this.damageTypeName = damageTypeName;
        this.wrapperTag = wrapperTag;
    }
    
    /**
     * Create default HealthTagValue from HealthConfig
     */
    private static HealthTagValue createDefaultConfig(HealthConfig config, String damageTypeName) {
        float multiplier = switch (damageTypeName) {
            case "FALL" -> config.fallDamageMultiplier();
            case "FIRE" -> config.fireDamageMultiplier();
            case "CACTUS" -> config.cactusDamageMultiplier();
            default -> 1.0f;
        };
        boolean enabled = switch (damageTypeName) {
            case "FALL" -> config.fallDamageEnabled();
            case "FIRE" -> config.fireDamageEnabled();
            case "CACTUS" -> config.cactusDamageEnabled();
            default -> true;
        };
        boolean blockable = switch (damageTypeName) {
            case "FALL" -> config.fallDamageBlockable();
            case "FIRE" -> config.fireDamageBlockable();
            case "CACTUS" -> config.cactusDamageBlockable();
            default -> true;
        };
        return new HealthTagValue(multiplier, null, enabled, blockable);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected Tag<ConfigTagWrapper<HealthTagValue>> getWrapperTag(Entity attacker) {
        // For damage types, we always use the same tag (no projectile distinction)
        // Cast is safe because HealthTagWrapper implements ConfigTagWrapper<HealthTagValue>
        return (Tag<ConfigTagWrapper<HealthTagValue>>) (Tag<?>) wrapperTag;
    }
    
    @Override
    protected int getComponentCount() {
        // Health damage has 1 component: the damage amount
        return 1;
    }
    
    /**
     * Get the name of this damage type (for logging/debugging)
     */
    public String getDamageTypeName() {
        return damageTypeName;
    }
    
    /**
     * Resolve health tag configuration using ConfigurableSystem's resolution logic
     * For environmental damage (no attacker), pass null for attacker and item
     */
    protected HealthTagValue resolveConfig(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        return resolveBaseConfig(attacker, victim, item);
    }
    
    /**
     * Get the damage multiplier for an entity using ConfigurableSystem's resolution
     */
    protected float getMultiplier(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        // Resolve config
        HealthTagValue resolved = resolveConfig(attacker, victim, item);
        
        // Get multiplier from config
        float baseMultiplier = resolved.multiplier() != null ? resolved.multiplier() : getDefaultMultiplier();
        
        // Apply multiplier from tag wrappers (item × attacker × player × world)
        double[] multipliers = getMultipliers(attacker, victim, item);
        if (multipliers.length > 0 && multipliers[0] != 1.0) {
            baseMultiplier *= (float) multipliers[0];
        }
        
        return baseMultiplier;
    }
    
    /**
     * Get modify (additive) value for an entity using ConfigurableSystem's resolution
     */
    protected float getModify(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        // Get modify from tag wrappers (item + attacker + player + world)
        double modify = getModifyValue(attacker, victim, item, 0);
        return (float) modify;
    }
    
    /**
     * Check if this damage type is enabled for an entity
     */
    protected boolean isEnabled(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item) {
        HealthTagValue resolved = resolveConfig(attacker, victim, item);
        if (resolved.enabled() != null) {
            return resolved.enabled();
        }
        return isEnabledByDefault();
    }
    
    /**
     * Calculate final damage amount with all modifiers applied using ConfigurableSystem
     * Formula: (baseDamage * multiplier) + modify
     */
    protected float calculateDamage(@Nullable Entity attacker, LivingEntity victim, @Nullable ItemStack item, float baseDamage) {
        if (!isEnabled(attacker, victim, item)) {
            return 0.0f;
        }
        
        float multiplier = getMultiplier(attacker, victim, item);
        float modify = getModify(attacker, victim, item);
        float damage = (baseDamage * multiplier) + modify;
        
        // Ensure damage is non-negative
        if (damage < 0.0f) {
            damage = 0.0f;
        }
        
        return damage;
    }
    
    /**
     * Convenience method for environmental damage (no attacker/item)
     */
    protected float calculateDamage(LivingEntity victim, float baseDamage) {
        return calculateDamage(null, victim, null, baseDamage);
    }
    
    /**
     * Convenience method to check if enabled (no attacker/item)
     */
    protected boolean isEnabled(LivingEntity victim) {
        return isEnabled(null, victim, null);
    }
    
    /**
     * Check if blocking applies to this damage type for the victim.
     * Resolves tag (victim, world, server default). Used by BlockingModifiers.
     */
    public boolean isBlockingApplicable(LivingEntity victim) {
        HealthTagValue resolved = resolveConfig(null, victim, null);
        return resolved.blockable() != null ? resolved.blockable() : getDefaultBlockable();
    }
    
    /**
     * Get the default blockable state from config (server default when no tag is set).
     */
    protected abstract boolean getDefaultBlockable();
    
    /**
     * Process a health event for this damage type
     * Subclasses should override this to handle specific damage logic
     */
    public abstract void processHealthEvent(HealthEvent event);
    
    /**
     * Check if this damage type should handle the given health event
     */
    public abstract boolean shouldHandle(HealthEvent event);
    
    /**
     * Get the default enabled state from config
     */
    protected abstract boolean isEnabledByDefault();
    
    /**
     * Get the default multiplier from config
     */
    protected abstract float getDefaultMultiplier();
    
    /**
     * Initialize this damage type (called when health system initializes)
     */
    public void initialize() {
        // Override in subclasses if needed
    }
    
    /**
     * Cleanup this damage type (called when entity is removed)
     */
    public void cleanup(LivingEntity entity) {
        // Override in subclasses if needed
        entity.removeTag(wrapperTag);
    }
}


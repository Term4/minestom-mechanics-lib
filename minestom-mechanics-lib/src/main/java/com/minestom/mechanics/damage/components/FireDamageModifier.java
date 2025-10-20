package com.minestom.mechanics.damage.components;

import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.config.gameplay.DamageConfig;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.registry.RegistryKey;

import static com.minestom.mechanics.constants.CombatConstants.DEFAULT_FIRE_DAMAGE_MULTIPLIER;

// TODO: Probably introduce a GENERAL damage modifier (abstract class), and extend.
//  OR better yet, just add the damage modification method to the original
//  damage type abstract class. NOTE could be weird with tick damage, but
//  probably not too bad

/**
 * Modifies fire damage based on configuration.
 * Handles fire damage enable/disable and damage multiplier.
 */
public class FireDamageModifier {
    private static final LogUtil.SystemLogger log = LogUtil.system("FireDamageModifier");
    
    private final DamageConfig config;

    public FireDamageModifier(DamageConfig config) {
        this.config = config;
    }

    /**
     * Modify fire damage for an entity damage event
     */
    public void modifyFireDamage(EntityDamageEvent event) {
        var damageType = event.getDamage().getType();

        if (isFireDamage(damageType)) {
            if (!config.isFireDamageEnabled()) {
                event.setCancelled(true);
                log.debug("Fire damage cancelled for entity");
                return;
            }

            if (config.getFireDamageMultiplier() != DEFAULT_FIRE_DAMAGE_MULTIPLIER) {
                float originalDamage = event.getDamage().getAmount();
                float newDamage = originalDamage * config.getFireDamageMultiplier();
                event.getDamage().setAmount(newDamage);
                
                log.debug("Fire damage modified: {:.2f} -> {:.2f} (multiplier: {:.2f})",
                        originalDamage, newDamage, config.getFireDamageMultiplier());
            }
        }
    }

    /**
     * Check if a damage type is fire-related
     */
    private boolean isFireDamage(RegistryKey<DamageType> damageType) {
        return damageType.equals(DamageType.ON_FIRE) ||
                damageType.equals(DamageType.IN_FIRE) ||
                damageType.equals(DamageType.LAVA);
    }
}

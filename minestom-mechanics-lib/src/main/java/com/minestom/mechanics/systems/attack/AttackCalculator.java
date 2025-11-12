package com.minestom.mechanics.systems.attack;

import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import static com.minestom.mechanics.config.constants.CombatConstants.*;

/**
 * Consolidated attack calculator that handles both damage calculation and critical hit detection.
 * ✅ CONSOLIDATED: Merged CriticalHitDetector + DamageCalculator into single class.
 */
public class AttackCalculator {
    private static final LogUtil.SystemLogger log = LogUtil.system("AttackCalculator");

    private final CombatConfig config;

    public AttackCalculator(CombatConfig config) {
        this.config = config;
    }
    
    // ===========================
    // DAMAGE CALCULATION
    // ===========================
    
    /**
     * Calculate base damage for a player's attack.
     */
    public float calculateBaseDamage(Player attacker) {
        ItemStack weapon = attacker.getItemInMainHand();
        if (weapon.isAir()) return DAMAGE_FIST;
        
        Material material = weapon.material();

        // TODO: this seems extremely inefficient. There has got to be a better
        //  way to do this lmao.

        // Swords
        if (material == Material.WOODEN_SWORD) return DAMAGE_WOODEN_SWORD;
        if (material == Material.STONE_SWORD) return DAMAGE_STONE_SWORD;
        if (material == Material.IRON_SWORD) return DAMAGE_IRON_SWORD;
        if (material == Material.GOLDEN_SWORD) return DAMAGE_GOLDEN_SWORD;
        if (material == Material.DIAMOND_SWORD) return DAMAGE_DIAMOND_SWORD;
        if (material == Material.NETHERITE_SWORD) return DAMAGE_NETHERITE_SWORD;
        
        // Axes
        if (material == Material.WOODEN_AXE) return DAMAGE_WOODEN_AXE;
        if (material == Material.STONE_AXE) return DAMAGE_STONE_AXE;
        if (material == Material.IRON_AXE) return DAMAGE_IRON_AXE;
        if (material == Material.GOLDEN_AXE) return DAMAGE_GOLDEN_AXE;
        if (material == Material.DIAMOND_AXE) return DAMAGE_DIAMOND_AXE;
        if (material == Material.NETHERITE_AXE) return DAMAGE_NETHERITE_AXE;
        
        // Pickaxes
        if (material == Material.WOODEN_PICKAXE) return DAMAGE_WOODEN_PICKAXE;
        if (material == Material.STONE_PICKAXE) return DAMAGE_STONE_PICKAXE;
        if (material == Material.IRON_PICKAXE) return DAMAGE_IRON_PICKAXE;
        if (material == Material.GOLDEN_PICKAXE) return DAMAGE_GOLDEN_PICKAXE;
        if (material == Material.DIAMOND_PICKAXE) return DAMAGE_DIAMOND_PICKAXE;
        if (material == Material.NETHERITE_PICKAXE) return DAMAGE_NETHERITE_PICKAXE;
        
        // Shovels
        if (material == Material.WOODEN_SHOVEL) return DAMAGE_WOODEN_SHOVEL;
        if (material == Material.STONE_SHOVEL) return DAMAGE_STONE_SHOVEL;
        if (material == Material.IRON_SHOVEL) return DAMAGE_IRON_SHOVEL;
        if (material == Material.GOLDEN_SHOVEL) return DAMAGE_GOLDEN_SHOVEL;
        if (material == Material.DIAMOND_SHOVEL) return DAMAGE_DIAMOND_SHOVEL;
        if (material == Material.NETHERITE_SHOVEL) return DAMAGE_NETHERITE_SHOVEL;
        
        return DAMAGE_FIST;
    }
    
    /**
     * Calculate final damage with critical hit multiplier.
     */
    public float calculateFinalDamage(float baseDamage, boolean isCritical) {
        if (isCritical) {
            float finalDamage = baseDamage * config.criticalMultiplier();
            log.debug("Critical hit: {:.2f} -> {:.2f} (multiplier: {:.2f})",
                    baseDamage, finalDamage, config.criticalMultiplier());
            return finalDamage;
        }
        return baseDamage;
    }
    
    // ===========================
    // CRITICAL HIT DETECTION
    // ===========================
    
    /**
     * Check if the attack should be a critical hit.
     */
    public boolean isCriticalHit(Player attacker) {
        if (attacker.isOnGround()) {
            return false;
        }
        
        // Check sprint requirement based on config
        if (!config.allowSprintCrits() && attacker.isSprinting()) {
            return false;
        }
        
        // Use fall distance to detect actual falling (not just "in air")
        double fallDistance = getFallDistance(attacker);
        boolean isCritical = fallDistance > 0;
        
        if (isCritical) {
            log.debug("{} critical hit detected (fall distance: {:.2f})",
                    attacker.getUsername(), fallDistance);
        }
        
        return isCritical;
    }
    
    /**
     * Get fall distance for critical hit detection.
     */
    private double getFallDistance(Player player) {
        try {
            return HealthSystem.getInstance().getFallDistance(player);
        } catch (IllegalStateException e) {
            return 0.0; // HealthSystem not initialized
        }
    }
}


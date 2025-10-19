package com.minestom.mechanics.projectile.components;

import com.minestom.mechanics.projectile.config.ProjectileKnockbackConfig;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.component.DataComponents;
import com.minestom.mechanics.projectile.entities.AbstractArrow;
import com.minestom.mechanics.projectile.entities.Arrow;

import java.util.Objects;

/**
 * Creates and configures arrows for bow shooting.
 * Handles arrow creation, enchantment application, and spawning.
 */
public class BowArrowCreator {
    private static final LogUtil.SystemLogger log = LogUtil.system("BowArrowCreator");
    
    /**
     * Create and configure an arrow for shooting
     * @param arrowStack The arrow item stack
     * @param player The shooting player
     * @param bowStack The bow item stack (for enchantments)
     * @param power The calculated bow power
     * @return The configured arrow
     */
    public AbstractArrow createArrow(ItemStack arrowStack, Player player, ItemStack bowStack, double power) {
        // Create arrow
        AbstractArrow arrow = new Arrow(player);
        
        // Apply critical hit
        if (power >= 1.0) {
            arrow.setCritical(true);
        }
        
        // Apply enchantments
        applyEnchantments(arrow, bowStack, player);
        
        // Set pickup mode based on infinity
        setPickupMode(arrow, arrowStack, player, bowStack);
        
        // Configure arrow knockback
        arrow.setUseKnockbackHandler(true);
        arrow.setKnockbackConfig(getArrowKnockbackConfig());
        
        log.debug("Created arrow for {} with power {:.2f}", player.getUsername(), power);
        return arrow;
    }

    // TODO: Could probably use one general spawn method for all projectiles?
    /**
     * Spawn the arrow in the world
     * Fixed for 1.8 compatibility: spawn from player's face using legacy eye height
     * @param arrow The arrow to spawn
     * @param player The shooting player
     * @param power The bow power
     */
    public void spawnArrow(AbstractArrow arrow, Player player, double power) {
        // Calculate spawn position using configured eye height
        Pos playerPos = player.getPosition();
        Pos eyePos = com.minestom.mechanics.features.gameplay.EyeHeightSystem.getInstance().getEyePosition(player);
        Pos spawnPos = eyePos.add(0D, -com.minestom.mechanics.projectile.ProjectileConstants.ARROW_SPAWN_HEIGHT_OFFSET, 0D);
        
        // Set velocity BEFORE spawning - this calculates the correct view direction
        arrow.shootFromRotation(playerPos.pitch(), playerPos.yaw(), 0f, power * 3, 1.0);
        
        // Add player momentum if configured (modern feature, disabled by default for legacy 1.8)
        if (shouldInheritPlayerMomentum()) {
            Vec playerVel = player.getVelocity();
            arrow.setVelocity(arrow.getVelocity().add(playerVel.x(),
                player.isOnGround() ? 0.0D : playerVel.y(), playerVel.z()));
        }
        
        // Spawn arrow with view direction copied from arrow entity (fixes wrong direction)
        arrow.setInstance(Objects.requireNonNull(player.getInstance()), 
            spawnPos.withView(arrow.getPosition()));
        
        log.debug("Spawned arrow for {} at {}", player.getUsername(), spawnPos);
    }
    
    /**
     * Apply bow enchantments to the arrow
     */
    private void applyEnchantments(AbstractArrow arrow, ItemStack bowStack, Player player) {
        EnchantmentList enchantments = bowStack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) enchantments = EnchantmentList.EMPTY;
        
        // Power enchantment
        int powerLevel = enchantments.level(Enchantment.POWER);
        if (powerLevel > 0) {
            arrow.setBaseDamage(arrow.getBaseDamage() + (double) powerLevel * 0.5 + 0.5);
        }
        
        // Punch enchantment
        int punchLevel = enchantments.level(Enchantment.PUNCH);
        if (punchLevel > 0) {
            arrow.setKnockback(punchLevel);
        }
        
        // Flame enchantment
        if (enchantments.level(Enchantment.FLAME) > 0) {
            arrow.setFireTicksLeft(100 * ServerFlag.SERVER_TICKS_PER_SECOND);
        }
        
        log.debug("Applied enchantments to arrow for {}", player.getUsername());
    }
    
    /**
     * Set arrow pickup mode based on infinity enchantment
     */
    private void setPickupMode(AbstractArrow arrow, ItemStack arrowStack, Player player, ItemStack bowStack) {
        EnchantmentList enchantments = bowStack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) enchantments = EnchantmentList.EMPTY;
        
        boolean infinite = player.getGameMode() == GameMode.CREATIVE
                || enchantments.level(Enchantment.INFINITY) > 0;
        
        boolean reallyInfinite = infinite && arrowStack.material() == net.minestom.server.item.Material.ARROW;
        
        if (reallyInfinite || player.getGameMode() == GameMode.CREATIVE) {
            arrow.setPickupMode(AbstractArrow.PickupMode.CREATIVE_ONLY);
        }
    }
    
    // ===========================
    // CONFIGURATION HELPERS
    // ===========================
    
    private ProjectileKnockbackConfig getArrowKnockbackConfig() {
        try {
            return com.minestom.mechanics.manager.ProjectileManager.getInstance().getArrowKnockbackConfig();
        } catch (IllegalStateException e) {
            return ProjectileKnockbackConfig.defaultArrowKnockback();
        }
    }
    
    private boolean shouldInheritPlayerMomentum() {
        try {
            return com.minestom.mechanics.manager.ProjectileManager.getInstance()
                .getProjectileConfig().shouldInheritPlayerMomentum();
        } catch (IllegalStateException e) {
            return false; // Default to false for legacy 1.8 compatibility
        }
    }
}

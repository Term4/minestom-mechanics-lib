package com.minestom.mechanics.projectile.components;

import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.projectile.utils.VelocityCalculator;
import com.minestom.mechanics.projectile.config.FishingRodConfig;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;

/**
 * Calculates fishing rod bobber velocity.
 * Handles the complex velocity calculation for fishing rod casting.
 */
public class FishingVelocityCalculator {
    private static final LogUtil.SystemLogger log = LogUtil.system("FishingVelocityCalculator");
    
    private final FishingRodConfig config;
    
    public FishingVelocityCalculator(FishingRodConfig config) {
        this.config = config;
    }
    
    /**
     * Calculate fishing rod bobber velocity
     * @param playerPos The player's position
     * @param playerPitch The player's pitch
     * @param playerYaw The player's yaw
     * @return The calculated velocity vector
     */
    public Vec calculateVelocity(Pos playerPos, float playerPitch, float playerYaw) {
        // Legacy (1.8) velocity calculation using shared utility
        double maxVelocity = 0.4F;
        Vec velocity = VelocityCalculator.calculateDirectionalVelocity(playerPitch, playerYaw, maxVelocity);
        
        // Apply horizontal and vertical multipliers
        velocity = new Vec(
            velocity.x() * config.horizontalMultiplier(),
            velocity.y() * config.verticalMultiplier(),
            velocity.z() * config.horizontalMultiplier()
        );
        
        // Apply spread using shared utility with configurable multiplier
        double spread = 0.0075 * config.spreadMultiplier(); // Legacy spread with multiplier
        velocity = VelocityCalculator.applySpread(velocity, spread);
        velocity = velocity.mul(config.powerMultiplier());
        
        // Convert to per-tick velocity using shared utility
        Vec finalVelocity = VelocityCalculator.toPerTickVelocity(velocity, 0.75);
        
        log.debug("Calculated fishing velocity: {} (config: h={}, v={}, s={}, p={})", 
                 finalVelocity, config.horizontalMultiplier(), config.verticalMultiplier(),
                 config.spreadMultiplier(), config.powerMultiplier());
        return finalVelocity;
    }
    
    /**
     * Calculate spawn position for fishing bobber using configured eye height
     * @param player The player
     * @return The spawn position
     */
    public Pos calculateSpawnPosition(net.minestom.server.entity.Player player) {
        return VelocityCalculator.calculateSpawnOffset(player);
    }
    
    /**
     * Calculate spawn position for fishing bobber with hard-coded eye height
     * @deprecated Use calculateSpawnPosition(Player) for configured eye heights
     * @param playerPos The player's position
     * @param playerYaw The player's yaw
     * @return The spawn position
     */
    @Deprecated
    public Pos calculateSpawnPosition(Pos playerPos, float playerYaw) {
        return VelocityCalculator.calculateSpawnOffset(playerPos, playerYaw);
    }
}

package com.minestom.mechanics.projectile.components;

import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.ServerFlag;

/**
 * Calculates bow power based on drawing time.
 * Handles the power calculation formula for bow shooting.
 */
@Deprecated
public class BowPowerCalculator {
    private static final LogUtil.SystemLogger log = LogUtil.system("BowPowerCalculator");
    
    /**
     * Calculate bow power based on drawing time in ticks
     * @param ticks The number of ticks the bow was drawn
     * @return Power value between 0.0 and 1.0
     */
    public double calculatePower(long ticks) {
        double seconds = ticks / (double) ServerFlag.SERVER_TICKS_PER_SECOND;
        double power = (seconds * seconds + seconds * 2.0) / 3.0;
        double finalPower = Math.min(power, 1.0);
        
        log.debug("Bow power calculated: {} ticks -> {:.2f} power", ticks, finalPower);
        return finalPower;
    }

    // TODO: Maybe increase the necessary power to shoot? Seems like people can spam arrows very easily.
    //  ALSO could make configurable? at least move to a constants class...
    /**
     * Check if the power is sufficient to shoot an arrow
     * @param power The calculated power
     * @return true if power is sufficient (>= 0.1)
     */
    public boolean isPowerSufficient(double power) {
        return power >= 0.1;
    }

    // TODO: We have this method, yet we don't use it in BowArrowCreator.
    //  Either remove it here, or use it in arrow creation.
    //  ALSO are we haulting the power counter at 1? or do we keep increasing it
    //  if players keep holding the bow..? Could hit int limit if someone held their bow for a long time lmao
    /**
     * Check if the power is maximum (critical hit)
     * @param power The calculated power
     * @return true if power is maximum (>= 1.0)
     */
    public boolean isCriticalHit(double power) {
        return power >= 1.0;
    }
}

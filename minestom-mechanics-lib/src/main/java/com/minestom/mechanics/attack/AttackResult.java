package com.minestom.mechanics.attack;

// TODO: Is it possible to get the attack result without manually
//  inserting a new method into the event flow? Do I need to add a getAttackResult method here?
//  Would be very useful for server development

/**
 * Result of an attack calculation.
 * Contains all the data needed for processing the attack.
 * 
 * @param damage the damage amount
 * @param wasCritical whether this was a critical hit
 * @param hadSprintBonus whether sprint bonus was applied
 */
public record AttackResult(
        float damage,
        boolean wasCritical,
        boolean hadSprintBonus
) {
    /**
     * Create a new attack result.
     */
    public static AttackResult of(float damage, boolean wasCritical, boolean hadSprintBonus) {
        return new AttackResult(damage, wasCritical, hadSprintBonus);
    }
    
    /**
     * Get formatted damage string for logging.
     */
    public String getFormattedDamage() {
        return String.format("%.2f", damage);
    }
    
    /**
     * Get attack description for logging.
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(getFormattedDamage()).append(" damage");
        
        if (wasCritical) {
            desc.append(" (CRIT)");
        }
        
        if (hadSprintBonus) {
            desc.append(" (SPRINT)");
        }
        
        return desc.toString();
    }
}

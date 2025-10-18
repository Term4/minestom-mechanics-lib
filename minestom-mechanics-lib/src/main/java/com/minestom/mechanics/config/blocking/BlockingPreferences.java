package com.minestom.mechanics.config.blocking;

import net.minestom.server.particle.Particle;
import net.minestom.server.item.Material;

import static com.minestom.mechanics.config.combat.CombatConstants.DEFAULT_BLOCKING_PARTICLE_COUNT;

/**
 * BlockingPreferences - Player visual preferences for blocking.
 * ✅ REFACTORED: Default values use constants
 */
public class BlockingPreferences {

    // Visual preferences for OTHER players blocking
    public boolean showShieldOnOthers = true;
    public boolean showParticlesOnOthers = false;
    public ParticleType particleType = ParticleType.CRIT;

    // ✅ REFACTORED: Using constant for default particle count
    public int particleCount = DEFAULT_BLOCKING_PARTICLE_COUNT;

    // Note: PARTICLE_RADIUS is now in CombatConstants

    // Visual preferences for SELF when blocking
    public boolean showShieldOnSelf = true;
    public boolean showParticlesOnSelf = false;

    // Notification preferences
    public boolean showActionBarOnBlock = false;
    public boolean playAnvilSoundOnSuccessfulBlock = false;

    public enum ParticleType {
        CRIT("Critical Hit", Particle.CRIT, Material.IRON_SWORD),
        BLOCK("Block Crumble", Particle.BLOCK, Material.COBBLESTONE),
        ITEM("Slime", Particle.ITEM, Material.SLIME_BALL),
        RAIN("Rain", Particle.RAIN, Material.WATER_BUCKET),
        DUST("Dust", Particle.DUST, Material.REDSTONE);

        public final String displayName;
        public final Particle particle;
        public final Material icon;

        ParticleType(String displayName, Particle particle, Material icon) {
            this.displayName = displayName;
            this.particle = particle;
            this.icon = icon;
        }
    }
}

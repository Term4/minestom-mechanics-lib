package com.minestom.mechanics.config.blocking;

import net.minestom.server.particle.Particle;
import net.minestom.server.item.Material;

import static com.minestom.mechanics.constants.CombatConstants.DEFAULT_BLOCKING_PARTICLE_COUNT;

// TODO: Move this to player data package? Make player data manager? Or is that too much for this library?
//  As of right now, this gets reset upon restarting the server, and players do NOT have persistent data.
//  WOULD LIKE to move to persistent data using mysql or something similar, but I feel like adding an entire player data
//  manage to THIS library would be too much. Probably do that in the server itself, or in a separate library, and have this
//  feature (among others) be compatible (wouldn't be difficult), would also probably allow simplification here

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

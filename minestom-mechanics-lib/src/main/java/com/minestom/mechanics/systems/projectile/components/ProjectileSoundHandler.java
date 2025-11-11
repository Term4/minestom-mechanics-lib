package com.minestom.mechanics.systems.projectile.components;

import com.minestom.mechanics.systems.sounds.SoundPlayer;
import com.minestom.mechanics.util.LogUtil;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;

import java.util.concurrent.ThreadLocalRandom;

// TODO: Just remove this and use a separate sounds package for all sounds.
//  ALSO right now players can hear OTHER players projectiles, but not their own.
//  ALSO with the new sounds package, can move the blocking features sound (successful block sound)
//  to the sounds package.

/**
 * Consolidated sound handler for all projectile types.
 * ✅ CONSOLIDATED: Merged 3 sound manager classes into a single handler.
 * ✅ INTERFACE: Implements SoundPlayer for standardized sound playing.
 */
public class ProjectileSoundHandler implements SoundPlayer {
    private final LogUtil.SystemLogger log;
    
    public ProjectileSoundHandler() {
        this.log = LogUtil.system("ProjectileSoundHandler");
    }
    
    // ===========================
    // BOW SOUNDS
    // ===========================
    
    /**
     * Play bow shooting sound
     * @param player The shooting player
     * @param power The bow power (affects pitch)
     */
    public void playBowShootSound(Player player, double power) {
        float pitch = 1.0f + (float) power * 0.5f;
        playSound(player, SoundEvent.ENTITY_ARROW_SHOOT, 
                 Sound.Source.PLAYER, 1.0f, pitch);
    }
    
    /**
     * Play bow drawing start sound (if needed)
     * @param player The drawing player
     */
    public void playBowDrawStartSound(Player player) {
        // Bow drawing is typically silent in vanilla Minecraft
        // This method is here for potential future enhancements
        log.debug("Bow drawing started for {}", player.getUsername());
    }
    
    /**
     * Play bow drawing stop sound (if needed)
     * @param player The player who stopped drawing
     */
    public void playBowDrawStopSound(Player player) {
        // Bow drawing stop is typically silent in vanilla Minecraft
        // This method is here for potential future enhancements
        log.debug("Bow drawing stopped for {}", player.getUsername());
    }
    
    // ===========================
    // FISHING ROD SOUNDS
    // ===========================
    
    /**
     * Play fishing rod cast sound
     * @param player The casting player
     */
    public void playFishingCastSound(Player player) {
        playSound(player, SoundEvent.ENTITY_FISHING_BOBBER_THROW, 
                 Sound.Source.NEUTRAL, 0.5f, 0.4f);
    }
    
    /**
     * Play fishing rod retrieve sound
     * @param player The retrieving player
     */
    public void playFishingRetrieveSound(Player player) {
        playSound(player, SoundEvent.ENTITY_FISHING_BOBBER_RETRIEVE, 
                 Sound.Source.NEUTRAL, 1.0f, 0.4f);
    }
    
    // ===========================
    // MISC PROJECTILE SOUNDS
    // ===========================
    
    /**
     * Play throw sound for a projectile
     * @param player The throwing player
     * @param material The material being thrown
     */
    public void playThrowSound(Player player, Material material) {
        SoundEvent soundEvent = getThrowSound(material);
        Sound.Source source = getSoundSource(material);
        
        playSound(player, soundEvent, source, 0.5f, 0.4f);
    }
    
    /**
     * Get the appropriate throw sound for a material
     * @param material The material being thrown
     * @return The sound event
     */
    private SoundEvent getThrowSound(Material material) {
        if (material == Material.SNOWBALL) {
            return SoundEvent.ENTITY_SNOWBALL_THROW;
        } else if (material == Material.ENDER_PEARL) {
            return SoundEvent.ENTITY_ENDER_PEARL_THROW;
        } else if (material == Material.EGG) {
            return SoundEvent.ENTITY_EGG_THROW;
        } else {
            return SoundEvent.ENTITY_SNOWBALL_THROW; // Default fallback
        }
    }
    
    /**
     * Get the appropriate sound source for a material
     * @param material The material being thrown
     * @return The sound source
     */
    private Sound.Source getSoundSource(Material material) {
        if (material == Material.SNOWBALL || material == Material.ENDER_PEARL) {
            return Sound.Source.NEUTRAL;
        } else if (material == Material.EGG) {
            return Sound.Source.PLAYER;
        } else {
            return Sound.Source.NEUTRAL;
        }
    }
    
    // ===========================
    // COMMON SOUND LOGIC
    // ===========================
    
    /**
     * Play a sound with optional pitch randomization.
     * 
     * @param player The player to play the sound for
     * @param soundEvent The sound event to play
     * @param source The sound source
     * @param volume The volume of the sound
     * @param basePitch The base pitch of the sound
     * @param randomizePitch Whether to randomize the pitch
     */
    private void playSound(Player player, SoundEvent soundEvent, Sound.Source source, 
                           float volume, float basePitch, boolean randomizePitch) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float pitch = randomizePitch ? 
            basePitch / (random.nextFloat() * 0.4f + 0.8f) : basePitch;
        
        player.getViewersAsAudience().playSound(Sound.sound(
            soundEvent, source, volume, pitch
        ), player);
        
        log.debug("Played {} sound for {} with pitch {:.2f}", 
                soundEvent.name(), player.getUsername(), pitch);
    }
    
    /**
     * Play a sound with default pitch randomization.
     * 
     * @param player The player to play the sound for
     * @param soundEvent The sound event to play
     * @param source The sound source
     * @param volume The volume of the sound
     * @param basePitch The base pitch of the sound
     */
    private void playSound(Player player, SoundEvent soundEvent, Sound.Source source, 
                           float volume, float basePitch) {
        playSound(player, soundEvent, source, volume, basePitch, true);
    }
    
    // ===========================
    // SOUNDPLAYER INTERFACE
    // ===========================
    
    @Override
    public void playShootSound(Player player) {
        // Default implementation - can be overridden by specific projectile types
        playBowShootSound(player, 1.0);
    }
    
    @Override
    public void playHitSound(Entity projectile, Entity hit) {
        // Default implementation - play generic hit sound
        if (hit instanceof Player hitPlayer) {
            playSound(hitPlayer, SoundEvent.ENTITY_ARROW_HIT_PLAYER, 
                     Sound.Source.PLAYER, 1.0f, 1.0f);
        }
    }
    
    @Override
    public void playImpactSound(Entity projectile) {
        // Default implementation - play generic impact sound
        // Note: This would need to be called from the projectile's location
        // For now, we'll just log that impact occurred
        log.debug("Projectile impact sound triggered for {}", projectile.getEntityType());
    }
}


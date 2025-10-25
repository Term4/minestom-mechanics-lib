package com.minestom.mechanics.systems.blocking;

import com.minestom.mechanics.systems.util.LogUtil;
import com.minestom.mechanics.config.blocking.BlockingPreferences;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.sound.SoundEvent;

// TODO: Integrate with proposed new abstract method in damage feature:
//  (abstract DamageType class would have modifyDamage method,
//  probably with configurability for static values, or reductions). This would
//  simplify the blocking feature package a lot, as well as provide customizability for
//  setting if blocking reduces a certain damage type or not (something I've been wanting to do for a while)

// TODO: Similar thing with knockback multipliers in the knockback modifier class.
//  should generalize that class, then the only thing blockingmodifiers would really do is to
//  provide feedback. Then just move that feedback to blockingvisuals in effects.

// TODO: Remove this??? Only have handled by tags? Is there a point in hard coding it or nah?

/**
 * Handles damage and knockback reduction for blocking players.
 * Consolidated from BlockingDamageReducer and BlockingKnockbackReducer.
 */
public class BlockingModifiers {
    private static final LogUtil.SystemLogger log = LogUtil.system("BlockingModifiers");

    private final BlockingSystem blockingSystem;
    private final BlockingState stateManager;

    public BlockingModifiers(BlockingSystem blockingSystem, BlockingState stateManager) {
        this.blockingSystem = blockingSystem;
        this.stateManager = stateManager;
    }

    /**
     * Handle damage reduction for blocking players
     */
    public void handleDamage(EntityDamageEvent event) {
        if (!blockingSystem.isEnabled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!stateManager.isBlocking(victim)) return;

        Damage damage = event.getDamage();
        float originalAmount = damage.getAmount();

        // Apply damage reduction
        double currentReduction = blockingSystem.getDamageReduction();
        float reducedAmount = originalAmount * (float)(1.0 - currentReduction);
        damage.setAmount(reducedAmount);

        log.debug("{} blocked {:.2f} damage (from {:.2f} to {:.2f})",
                victim.getUsername(), originalAmount - reducedAmount,
                originalAmount, reducedAmount);

        // Provide feedback based on preferences
        provideFeedback(victim, originalAmount, reducedAmount);
    }

    /**
     * Provide visual and audio feedback for successful blocks
     */
    private void provideFeedback(Player victim, float originalAmount, float reducedAmount) {
        BlockingPreferences prefs = victim.getTag(BlockingState.PREFERENCES);
        if (prefs == null) return;

        // Action bar message
        if (prefs.showActionBarOnBlock && blockingSystem.shouldShowDamageMessages()) {
            victim.sendActionBar(Component.text(
                    String.format("⛨ Blocked! %.1f → %.1f", originalAmount, reducedAmount),
                    NamedTextColor.GOLD));
        }

        // Sound effect
        if (prefs.playAnvilSoundOnSuccessfulBlock && originalAmount > reducedAmount) {
            victim.playSound(
                    Sound.sound(SoundEvent.BLOCK_ANVIL_LAND.key(), Sound.Source.PLAYER, 1.0f, 1f),
                    victim.getPosition()
            );
        }
    }
}


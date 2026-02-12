package com.minestom.mechanics.systems.blocking;

import com.minestom.mechanics.systems.health.HealthSystem;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.config.blocking.BlockingPreferences;
import net.minestom.server.entity.Player;
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
     * Apply blocking damage reduction. Called from the damage pipeline after damage is calculated.
     *
     * @return the reduced damage amount, or originalAmount if not blocking / not applicable
     */
    public float applyBlockingReduction(Player victim, float originalAmount,
                                        net.minestom.server.registry.RegistryKey<?> damageType) {
        if (!blockingSystem.isEnabled()) return originalAmount;
        if (!stateManager.isBlocking(victim)) return originalAmount;

        try {
            HealthSystem hs = HealthSystem.getInstance();
            if (!hs.isBlockingApplicable(damageType, victim)) return originalAmount;
        } catch (IllegalStateException ignored) {
            // HealthSystem not initialized; blocking applies to all
        }

        double currentReduction = blockingSystem.getDamageReduction(victim);
        float reducedAmount = originalAmount * (float) (1.0 - currentReduction);

        log.debug("{} blocked {:.2f} damage (from {:.2f} to {:.2f})",
                victim.getUsername(), originalAmount - reducedAmount,
                originalAmount, reducedAmount);

        provideFeedback(victim, originalAmount, reducedAmount);
        return reducedAmount;
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


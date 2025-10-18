package com.minestom.mechanics.features.blocking;

import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.config.blocking.BlockingConfig;
import com.minestom.mechanics.config.blocking.BlockingPreferences;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.entity.EntityVelocityEvent;
import net.minestom.server.coordinate.Vec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.sound.SoundEvent;

/**
 * Handles damage and knockback reduction for blocking players.
 * Consolidated from BlockingDamageReducer and BlockingKnockbackReducer.
 */
public class BlockingModifiers {
    private static final LogUtil.SystemLogger log = LogUtil.system("BlockingModifiers");

    private final BlockingConfig config;
    private final BlockingStateManager stateManager;

    public BlockingModifiers(BlockingConfig config, BlockingStateManager stateManager) {
        this.config = config;
        this.stateManager = stateManager;
    }

    /**
     * Handle damage reduction for blocking players
     */
    public void handleDamage(EntityDamageEvent event) {
        if (!config.isEnabled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!stateManager.isBlocking(victim)) return;

        Damage damage = event.getDamage();
        float originalAmount = damage.getAmount();

        // Apply damage reduction
        double currentReduction = config.getDamageReduction();
        float reducedAmount = originalAmount * (float)(1.0 - currentReduction);
        damage.setAmount(reducedAmount);

        log.debug("{} blocked {:.2f} damage (from {:.2f} to {:.2f})",
                victim.getUsername(), originalAmount - reducedAmount,
                originalAmount, reducedAmount);

        // Provide feedback based on preferences
        provideFeedback(victim, originalAmount, reducedAmount);
    }

    /**
     * Handle knockback reduction for blocking players
     */
    public void handleVelocity(EntityVelocityEvent event) {
        if (!config.isEnabled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!stateManager.isBlocking(victim)) return;

        Vec originalVelocity = event.getVelocity();

        // Apply knockback reduction
        double hMultiplier = config.getKnockbackHorizontalMultiplier();
        double vMultiplier = config.getKnockbackVerticalMultiplier();

        // Split velocity into horizontal and vertical components
        Vec reducedVelocity = new Vec(
                originalVelocity.x() * hMultiplier,
                originalVelocity.y() * vMultiplier,
                originalVelocity.z() * hMultiplier
        );

        event.setVelocity(reducedVelocity);

        double hReduction = (1.0 - hMultiplier) * 100;
        double vReduction = (1.0 - vMultiplier) * 100;

        log.debug("{} blocked knockback (h: {:.0f}%, v: {:.0f}%)",
                victim.getUsername(), hReduction, vReduction);
    }

    /**
     * Provide visual and audio feedback for successful blocks
     */
    private void provideFeedback(Player victim, float originalAmount, float reducedAmount) {
        BlockingPreferences prefs = victim.getTag(BlockingStateManager.PREFERENCES);
        if (prefs == null) return;

        // Action bar message
        if (prefs.showActionBarOnBlock && config.isShowDamageMessages()) {
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


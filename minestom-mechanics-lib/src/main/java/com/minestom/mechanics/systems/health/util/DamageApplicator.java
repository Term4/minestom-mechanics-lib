package com.minestom.mechanics.systems.health.util;

import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.manager.ArmorManager;
import com.minestom.mechanics.manager.MechanicsManager;
import com.minestom.mechanics.systems.health.damagetypes.DamageTypeRegistry;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

/**
 * Applies damage with invulnerability and replacement rules.
 * Owns the flow: bypass checks, i-frame block, replacement vs normal hit, then updates Invulnerability state.
 * Analogous to {@link com.minestom.mechanics.systems.knockback.KnockbackApplicator}.
 */
public class DamageApplicator {

    private static final LogUtil.SystemLogger log = LogUtil.system("DamageApplicator");

    private final HealthConfig config;
    private final Invulnerability invulnerability;
    private final DamageTypeRegistry damageTypeRegistry;

    public DamageApplicator(HealthConfig config, Invulnerability invulnerability, DamageTypeRegistry damageTypeRegistry) {
        this.config = config;
        this.invulnerability = invulnerability;
        this.damageTypeRegistry = damageTypeRegistry;
    }

    /**
     * Handle an entity damage event: apply bypass checks, i-frames, replacement logic, and update invulnerability state.
     * Call after duplicate-tick and creative-mode checks (done by HealthSystem).
     */
    public void handleDamage(EntityDamageEvent event) {
        LivingEntity victim = (LivingEntity) event.getEntity();
        Damage damage = event.getDamage();
        float damageAmount = damage.getAmount();
        RegistryKey<net.minestom.server.entity.damage.DamageType> damageType = damage.getType();

        Entity attacker = damage.getSource();
        net.minestom.server.item.ItemStack item = (attacker instanceof Player p) ? p.getItemInMainHand() : null;

        // 1. Damage-type bypass: if this damage type bypasses invulnerability, allow and skip i-frame block
        if (victim instanceof Player player && damageTypeRegistry.isBypassInvulnerability(damageType, player)) {
            invulnerability.setInvulnerable(victim, damageAmount);
            invulnerability.setLastDamageReplacement(victim, false);
            logDamage(victim, damage, damageAmount);
            return;
        }

        // 2. Weapon/attacker bypass: only if explicitly true (default false when unspecified)
        var invulnTag = invulnerability.resolveInvulnerabilityTag(attacker, victim, item);
        if (invulnTag != null && Boolean.TRUE.equals(invulnTag.bypassInvulnerability())) {
            invulnerability.setInvulnerable(victim, damageAmount);
            invulnerability.setLastDamageReplacement(victim, false);
            logDamage(victim, damage, damageAmount);
            return;
        }

        // 3. Not in i-frames: allow damage, set invulnerable
        long ticksSince = invulnerability.getTicksSinceLastDamage(victim);
        int invulnTicks = invulnerability.getInvulnerabilityTicks(attacker, victim, item);
        boolean isInIFrames = ticksSince >= 0 && ticksSince < invulnTicks;

        if (!isInIFrames) {
            invulnerability.setInvulnerable(victim, damageAmount);
            invulnerability.setLastDamageReplacement(victim, false);
            logDamage(victim, damage, damageAmount);
            return;
        }

        // 4. In i-frames: replacement or block
        boolean replacementEnabled = invulnerability.isDamageReplacementEnabled(attacker, victim, item);
        if (!replacementEnabled) {
            event.setCancelled(true);
            if (config.isLogReplacementDamage()) {
                log.debug("{} is invulnerable ({}/{} ticks)", getEntityName(victim), ticksSince, invulnTicks);
            }
            return;
        }

        float previousDamage = invulnerability.getLastDamageAmount(victim);
        if (damageAmount <= previousDamage) {
            event.setCancelled(true);
            if (config.isLogReplacementDamage()) {
                log.debug("{} damage rejected (weaker): {} <= {} ({}/{} ticks)",
                        getEntityName(victim), String.format("%.1f", damageAmount), String.format("%.1f", previousDamage),
                        ticksSince, invulnTicks);
            }
            return;
        }

        // 5. Replacement hit: cancel event, apply only the difference
        event.setCancelled(true);
        float damageDifference = damageAmount - previousDamage;
        float finalDifference = damageDifference;
        if (victim instanceof Player player && MechanicsManager.getInstance().getArmorManager() != null) {
            ArmorManager armorManager = MechanicsManager.getInstance().getArmorManager();
            if (armorManager.isInitialized() && armorManager.isEnabled()) {
                finalDifference = armorManager.calculateReducedDamage(player, damageDifference, damageType);
            }
        }
        float newHealth = Math.max(0, victim.getHealth() - finalDifference);
        victim.setHealth(newHealth);

        invulnerability.updateDamageAmount(victim, damageAmount);
        invulnerability.setLastDamageReplacement(victim, true);

        if (config.isLogReplacementDamage()) {
            log.debug("{} took {} additional damage (replacement: {} -> {}, armor reduced: {})",
                    getEntityName(victim), String.format("%.1f", finalDifference),
                    String.format("%.1f", previousDamage), String.format("%.1f", damageAmount),
                    String.format("%.1f", damageDifference - finalDifference));
        }
    }

    private void logDamage(LivingEntity victim, Damage damage, float amount) {
        if (config.isLogReplacementDamage()) {
            log.debug("{} took {} {} damage", getEntityName(victim), String.format("%.2f", amount), damage.getType().name());
        }
    }

    private static String getEntityName(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getUsername();
        }
        return entity.getClass().getSimpleName();
    }
}

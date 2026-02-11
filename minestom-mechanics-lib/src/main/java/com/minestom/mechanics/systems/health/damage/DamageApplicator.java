package com.minestom.mechanics.systems.health.damage;

import com.minestom.mechanics.config.health.HealthConfig;
import com.minestom.mechanics.manager.ArmorManager;
import com.minestom.mechanics.manager.MechanicsManager;
import com.minestom.mechanics.systems.health.InvulnerabilityTracker;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.registry.RegistryKey;


/**
 * Core damage pipeline. Handles:
 * <ol>
 *   <li>Creative mode bypass (from {@link DamageTypeProperties#bypassCreative()})</li>
 *   <li>Invulnerability bypass (from {@link DamageTypeProperties#bypassInvulnerability()})</li>
 *   <li>I-frame blocking</li>
 *   <li>Damage replacement (from {@link DamageTypeProperties#damageReplacement()})</li>
 *   <li>Armor reduction for replacement hits (respects {@link DamageTypeProperties#penetratesArmor()})</li>
 * </ol>
 *
 * <p>Does NOT apply multipliers -- that's handled by {@link DamageType#processDamage}
 * after this pipeline allows the event through.</p>
 */
public class DamageApplicator {
    private static final LogUtil.SystemLogger log = LogUtil.system("DamageApplicator");

    private final HealthConfig config;
    private final InvulnerabilityTracker invulnerability;

    public DamageApplicator(HealthConfig config, InvulnerabilityTracker invulnerability) {
        this.config = config;
        this.invulnerability = invulnerability;
    }

    /**
     * Run the full damage pipeline on an EntityDamageEvent.
     * Called by HealthSystem after deduplication.
     */
    public void handleDamage(EntityDamageEvent event) {
        LivingEntity victim = (LivingEntity) event.getEntity();
        Damage damage = event.getDamage();
        float damageAmount = damage.getAmount();
        RegistryKey<net.minestom.server.entity.damage.DamageType> damageType = damage.getType();

        Entity attacker = damage.getSource();
        ItemStack item = (attacker instanceof Player p) ? p.getItemInMainHand() : null;

        // Resolve properties from the matching damage type, or fall back to attack defaults
        DamageType matched = findDamageType(damageType);
        DamageTypeProperties props = matched != null
                ? matched.resolveProperties(attacker, victim, item)
                : DamageTypeProperties.ATTACK_DEFAULT;

        // 1. Creative mode check
        if (victim instanceof Player player && player.getGameMode() == GameMode.CREATIVE) {
            if (!props.bypassCreative()) {
                event.setCancelled(true);
                logDebug("Blocked damage for creative player: {}", getEntityName(victim));
                return;
            }
        }

        // 2. Bypass invulnerability: allow damage, skip i-frame check
        if (props.bypassInvulnerability()) {
            invulnerability.markDamaged(victim, damageAmount);
            invulnerability.setLastDamageReplacement(victim, false);
            logDamage(victim, damage, damageAmount);
            return;
        }

        // 3. Not in i-frames: allow damage normally
        if (!invulnerability.isInvulnerable(victim)) {
            invulnerability.markDamaged(victim, damageAmount);
            invulnerability.setLastDamageReplacement(victim, false);
            logDamage(victim, damage, damageAmount);
            return;
        }

        // 4. In i-frames: check if this damage type supports replacement
        if (!props.damageReplacement()) {
            event.setCancelled(true);
            long ticksSince = invulnerability.getTicksSinceLastDamage(victim);
            logDebug("{} is invulnerable ({}/{} ticks)",
                    getEntityName(victim), ticksSince, config.invulnerabilityTicks());
            return;
        }

        // 5. Replacement: only if incoming damage > previous damage
        float previousDamage = invulnerability.getLastDamageAmount(victim);
        if (damageAmount <= previousDamage) {
            event.setCancelled(true);
            logDebug("{} damage rejected (weaker): {:.1f} <= {:.1f}",
                    getEntityName(victim), damageAmount, previousDamage);
            return;
        }

        // 6. Apply replacement damage (difference only)
        event.setCancelled(true);
        float damageDifference = damageAmount - previousDamage;
        float finalDifference = damageDifference;

        // Apply armor reduction if available and damage doesn't penetrate
        if (!props.penetratesArmor() && victim instanceof Player player) {
            try {
                ArmorManager armorManager = MechanicsManager.getInstance().getArmorManager();
                if (armorManager != null && armorManager.isInitialized() && armorManager.isEnabled()) {
                    finalDifference = armorManager.calculateReducedDamage(player, damageDifference, damageType);
                }
            } catch (IllegalStateException ignored) {
                // MechanicsManager not initialized yet
            }
        }

        float newHealth = Math.max(0, victim.getHealth() - finalDifference);
        victim.setHealth(newHealth);

        invulnerability.updateDamageAmount(victim, damageAmount);
        invulnerability.setLastDamageReplacement(victim, true);

        logDebug("{} took {:.1f} replacement damage ({:.1f} -> {:.1f})",
                getEntityName(victim), finalDifference, previousDamage, damageAmount);
    }

    // ===========================
    // LOOKUP
    // ===========================

    private DamageType findDamageType(RegistryKey<?> type) {
        return DamageType.find(type);
    }

    // ===========================
    // LOGGING
    // ===========================

    private void logDamage(LivingEntity victim, Damage damage, float amount) {
        if (config.logDamage()) {
            log.debug("{} took {:.2f} {} damage",
                    getEntityName(victim), amount, damage.getType().name());
        }
    }

    private void logDebug(String msg, Object... args) {
        if (config.logDamage()) {
            log.debug(msg, args);
        }
    }

    private static String getEntityName(LivingEntity entity) {
        if (entity instanceof Player player) return player.getUsername();
        return entity.getClass().getSimpleName();
    }
}

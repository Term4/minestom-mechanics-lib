package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.systems.knockback.components.KnockbackStrength;
import com.minestom.mechanics.systems.knockback.components.KnockbackType;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;

/**
 * Applies knockback to entities based on resolved configuration.
 * Delegates config resolution to {@link KnockbackSystem}.
 */
public class KnockbackApplicator {

    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackApplicator");

    private final KnockbackCalculator calculator;

    public KnockbackApplicator(KnockbackConfig config) {
        this.calculator = new KnockbackCalculator(config);
    }

    // ===========================
    // PUBLIC API
    // ===========================

    /**
     * Apply melee knockback.
     */
    public void applyKnockback(LivingEntity victim, Entity attacker, KnockbackType type,
                               boolean wasSprinting, int kbEnchantLevel) {
        applyKnockbackInternal(victim, attacker, attacker.getPosition(),
                type, wasSprinting, kbEnchantLevel);
    }

    /**
     * Apply projectile knockback.
     */
    public void applyProjectileKnockback(LivingEntity victim, Entity projectile,
                                         Pos shooterOrigin, int kbEnchantLevel) {
        applyKnockbackInternal(victim, projectile, shooterOrigin,
                KnockbackType.PROJECTILE, false, kbEnchantLevel);
    }

    // ===========================
    // CORE LOGIC
    // ===========================

    private void applyKnockbackInternal(LivingEntity victim, Entity attacker, Pos sourcePos,
                                        KnockbackType type, boolean wasSprinting, int kbEnchantLevel) {
        // Resolve config from KnockbackSystem
        EquipmentSlot handUsed = (attacker instanceof Player && type != KnockbackType.PROJECTILE)
                ? EquipmentSlot.MAIN_HAND : null;
        KnockbackConfig resolved = KnockbackSystem.getInstance().resolveConfig(attacker, victim, handUsed);

        // Calculate direction
        Vec direction = type == KnockbackType.PROJECTILE
                ? calculator.calculateProjectileKnockbackDirection(victim, sourcePos)
                : calculator.calculateKnockbackDirection(victim, attacker);

        // Build strength from config
        double horizontal = resolved.horizontal();
        double vertical = resolved.vertical();

        // Apply sprint bonus (melee only)
        if (wasSprinting && type != KnockbackType.PROJECTILE) {
            horizontal += resolved.sprintBonusHorizontal();
            vertical += resolved.sprintBonusVertical();
            if (attacker instanceof Player player) {
                player.setSprinting(false);
            }
        }

        // Apply enchantment bonus (melee only)
        if (kbEnchantLevel > 0 && type != KnockbackType.PROJECTILE) {
            horizontal += kbEnchantLevel * 0.6;
            vertical += 0.1;
        }

        // Sweeping reduction
        if (type == KnockbackType.SWEEPING) {
            horizontal *= 0.5;
            vertical *= 0.5;
        }

        // Create strength record
        KnockbackStrength strength = new KnockbackStrength(horizontal, vertical);

        // Apply air multipliers
        if (!victim.isOnGround()) {
            strength = new KnockbackStrength(
                    strength.horizontal() * resolved.airMultiplierHorizontal(),
                    strength.vertical() * resolved.airMultiplierVertical()
            );
        }

        // Apply resistance attribute
        double resistance = victim.getAttributeValue(net.minestom.server.entity.attribute.Attribute.KNOCKBACK_RESISTANCE);
        strength = new KnockbackStrength(
                strength.horizontal() * (1 - resistance),
                strength.vertical() * (1 - resistance)
        );

        // Calculate final velocity
        Vec finalVelocity = calculator.calculateFinalVelocity(victim, direction, strength, type);

        // Apply velocity
        victim.setVelocity(finalVelocity);
        if (victim instanceof Player player) {
            player.sendPacket(new EntityVelocityPacket(player.getEntityId(), finalVelocity));
        }

        log.debug("Applied {} knockback: {}", type, finalVelocity);
    }
}
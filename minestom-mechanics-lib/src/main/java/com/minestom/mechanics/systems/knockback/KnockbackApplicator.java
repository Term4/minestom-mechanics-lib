package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import static com.minestom.mechanics.config.constants.CombatConstants.MIN_KNOCKBACK_DISTANCE;
import com.minestom.mechanics.systems.blocking.BlockingSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import org.jetbrains.annotations.Nullable;

/**
 * Applies knockback to entities based on resolved configuration.
 * Delegates config resolution to {@link KnockbackSystem} and direction to {@link KnockbackCalculator}.
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
     * Apply knockback with full context. Used by HealthSystem after damage pipeline.
     * Direction mode is determined from the resolved KnockbackConfig.
     *
     * @param victim          the entity being knocked back
     * @param attacker        the player who caused the damage (or null)
     * @param source          the direct damage source (player for melee, projectile for ranged)
     * @param shooterOriginPos where the shooter was at projectile launch (or null for melee)
     * @param type            knockback type (ATTACK, PROJECTILE, SWEEPING)
     * @param wasSprinting    whether the attacker was sprinting
     * @param kbEnchantLevel  knockback enchantment level
     */
    public void applyKnockback(LivingEntity victim, @Nullable Entity attacker, @Nullable Entity source,
                                @Nullable Pos shooterOriginPos, KnockbackSystem.KnockbackType type,
                                boolean wasSprinting, int kbEnchantLevel) {
        // Resolve config
        EquipmentSlot handUsed = (attacker instanceof Player && type != KnockbackSystem.KnockbackType.PROJECTILE)
                ? EquipmentSlot.MAIN_HAND : null;
        Entity configEntity = (type == KnockbackSystem.KnockbackType.PROJECTILE && source != null)
                ? source : (attacker != null ? attacker : source);
        KnockbackConfig resolved = KnockbackSystem.getInstance().resolveConfig(configEntity, victim, handUsed);

        // Calculate direction based on configured mode
        KnockbackSystem.KnockbackDirectionMode dirMode = (type == KnockbackSystem.KnockbackType.PROJECTILE)
                ? resolved.projectileDirection()
                : resolved.meleeDirection();
        KnockbackSystem.DegenerateFallback degenerateFallback = resolved.degenerateFallback();
        double proximityScaleDistance = resolved.proximityScaleDistance();
        double lookWeight = resolved.lookWeight();
        double sprintLookWeight = resolved.sprintLookWeight() != null ? resolved.sprintLookWeight() : lookWeight;

        KnockbackCalculator.KnockbackDirectionResult baseResult = calculator.calculateDirection(
                dirMode, victim, attacker, source, shooterOriginPos, lookWeight,
                degenerateFallback, proximityScaleDistance);
        double proximityMult = baseResult.proximityMultiplier();

        Vec direction;
        double horizontal;
        double vertical;

        if (wasSprinting && type != KnockbackSystem.KnockbackType.PROJECTILE) {
            if (attacker instanceof Player player) player.setSprinting(false);
            KnockbackCalculator.KnockbackDirectionResult sprintResult = calculator.calculateDirection(
                    dirMode, victim, attacker, source, shooterOriginPos, sprintLookWeight,
                    degenerateFallback, proximityScaleDistance);
            double baseH = resolved.horizontal();
            double sprintH = resolved.sprintBonusHorizontal();
            double hVecX = baseH * baseResult.direction().x() + sprintH * sprintResult.direction().x();
            double hVecZ = baseH * baseResult.direction().z() + sprintH * sprintResult.direction().z();
            double len = Math.sqrt(hVecX * hVecX + hVecZ * hVecZ);
            if (len < MIN_KNOCKBACK_DISTANCE) {
                direction = baseResult.direction();
                horizontal = baseH + sprintH;
            } else {
                direction = new Vec(hVecX / len, 0, hVecZ / len);
                horizontal = len;
            }
            vertical = resolved.vertical() + resolved.sprintBonusVertical();
        } else {
            direction = baseResult.direction();
            horizontal = resolved.horizontal();
            vertical = resolved.vertical();
        }

        horizontal *= proximityMult;
        vertical *= proximityMult;

        // Enchantment bonus (melee only)
        if (kbEnchantLevel > 0 && type != KnockbackSystem.KnockbackType.PROJECTILE) {
            horizontal += kbEnchantLevel * 0.6;
            vertical += 0.1;
        }

        // Sweeping reduction
        if (type == KnockbackSystem.KnockbackType.SWEEPING) {
            horizontal *= 0.5;
            vertical *= 0.5;
        }

        KnockbackSystem.KnockbackStrength strength = new KnockbackSystem.KnockbackStrength(horizontal, vertical);

        // Blocking reduction
        if (victim instanceof Player player) {
            try {
                BlockingSystem blocking = BlockingSystem.getInstance();
                if (blocking.isBlocking(player)) {
                    horizontal *= (1.0 - blocking.getKnockbackHorizontalReduction(player));
                    vertical *= (1.0 - blocking.getKnockbackVerticalReduction(player));
                    strength = new KnockbackSystem.KnockbackStrength(horizontal, vertical);
                }
            } catch (IllegalStateException ignored) {}
        }

        // Air multipliers
        if (!victim.isOnGround()) {
            strength = new KnockbackSystem.KnockbackStrength(
                    strength.horizontal() * resolved.airMultiplierHorizontal(),
                    strength.vertical() * resolved.airMultiplierVertical()
            );
        }

        // Resistance attribute
        double resistance = victim.getAttributeValue(net.minestom.server.entity.attribute.Attribute.KNOCKBACK_RESISTANCE);
        strength = new KnockbackSystem.KnockbackStrength(
                strength.horizontal() * (1 - resistance),
                strength.vertical() * (1 - resistance)
        );

        // Calculate and apply final velocity
        Vec finalVelocity = calculator.calculateFinalVelocity(victim, direction, strength, type);
        victim.setVelocity(finalVelocity);
        if (victim instanceof Player player) {
            player.sendPacket(new EntityVelocityPacket(player.getEntityId(), finalVelocity));
        }

        log.debug("Applied {} knockback ({}): {}", type, dirMode, finalVelocity);
    }

    // ===========================
    // LEGACY API (kept for backward compatibility)
    // ===========================

    /** @deprecated Use {@link #applyKnockback(LivingEntity, Entity, Entity, Pos, KnockbackSystem.KnockbackType, boolean, int)} */
    @Deprecated
    public void applyKnockback(LivingEntity victim, Entity attacker, KnockbackSystem.KnockbackType type,
                                boolean wasSprinting, int kbEnchantLevel) {
        applyKnockback(victim, attacker, attacker, null, type, wasSprinting, kbEnchantLevel);
    }

    /** @deprecated Use the full context version. */
    @Deprecated
    public void applyProjectileKnockback(LivingEntity victim, Entity projectile,
                                          Pos shooterOrigin, int kbEnchantLevel) {
        applyKnockback(victim, null, projectile, shooterOrigin,
                KnockbackSystem.KnockbackType.PROJECTILE, false, kbEnchantLevel);
    }
}

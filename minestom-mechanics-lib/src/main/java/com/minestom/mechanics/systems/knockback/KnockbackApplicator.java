package com.minestom.mechanics.systems.knockback;

import com.minestom.mechanics.config.knockback.KnockbackConfig;
import com.minestom.mechanics.util.LogUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import org.jetbrains.annotations.Nullable;

/**
 * Event-like knockback applicator. Resolves config and context, requests velocity from
 * {@link KnockbackCalculator}, then applies it. All computation lives in the calculator.
 */
public class KnockbackApplicator {

    private static final LogUtil.SystemLogger log = LogUtil.system("KnockbackApplicator");
    private final KnockbackCalculator calculator = new KnockbackCalculator();

    public KnockbackApplicator(KnockbackConfig config) {
        // Config used only for backward compat / future use; resolution is per-call
    }

    /**
     * Apply knockback with full context. Resolves config, builds context, computes via calculator, applies result.
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
        resolved = KnockbackSystem.resolveConfigForVictim(resolved, victim);

        // Determine wasSprinting: use buffer (ticks after stopping we still count as sprint hit) when configured
        boolean effectiveSprint = wasSprinting;
        if (attacker instanceof Player p && type != KnockbackSystem.KnockbackType.PROJECTILE) {
            effectiveSprint = resolved.sprintBufferTicks() > 0
                    ? KnockbackSystem.isSprintHit(p, resolved.sprintBufferTicks(), KnockbackSystem.getInstance().getCurrentTick())
                    : p.isSprinting();
        }

        KnockbackSystem.KnockbackContext ctx = new KnockbackSystem.KnockbackContext(
                victim, attacker, source, shooterOriginPos, type, effectiveSprint, kbEnchantLevel, resolved);

        // Side effect: stop sprint for tick (before applying knockback)
        if (effectiveSprint && type != KnockbackSystem.KnockbackType.PROJECTILE && attacker instanceof Player p) {
            p.setSprinting(false);
        }

        var debugSink = KnockbackSystem.isDebugToChat() ? new KnockbackCalculator.DebugSink() : null;
        Vec finalVelocity = calculator.computeKnockbackVelocity(ctx, debugSink);

        log.debug("Final Velocity: {}", finalVelocity);
        victim.setVelocity(finalVelocity);
        if (victim instanceof Player player) {
            player.sendPacket(new EntityVelocityPacket(player.getEntityId(), finalVelocity));
        }

        if (debugSink != null && debugSink.info != null) {
            sendDebugToChat(debugSink.info, attacker, victim);
        }
        log.debug("Applied {} knockback: {}", type, finalVelocity);
    }

    @Deprecated
    public void applyKnockback(LivingEntity victim, Entity attacker, KnockbackSystem.KnockbackType type,
                                boolean wasSprinting, int kbEnchantLevel) {
        applyKnockback(victim, attacker, attacker, null, type, wasSprinting, kbEnchantLevel);
    }

    @Deprecated
    public void applyProjectileKnockback(LivingEntity victim, Entity projectile,
                                          Pos shooterOrigin, int kbEnchantLevel) {
        applyKnockback(victim, null, projectile, shooterOrigin,
                KnockbackSystem.KnockbackType.PROJECTILE, false, kbEnchantLevel);
    }

    private static void sendDebugToChat(KnockbackSystem.KnockbackDebugInfo d,
                                        Entity attacker, LivingEntity victim) {
        var msg = Component.text()
                .append(Component.text("[KB] ", NamedTextColor.GRAY))
                .append(Component.text("old: ", NamedTextColor.DARK_GRAY))
                .append(vecStr(d.oldVelocity()))
                .append(Component.text(" → post: ", NamedTextColor.DARK_GRAY))
                .append(vecStr(d.postKnockbackVelocity()))
                .append(Component.text(" | preSprint: ", NamedTextColor.DARK_GRAY))
                .append(vecStr(d.preSprintBonusVector()))
                .build();
        var vert = Component.empty();
        if (d.verticalPreLimit() != null && d.verticalLimitApplied() != null) {
            vert = Component.text(" | vert preLimit: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(String.format("%.3f", d.verticalPreLimit()), NamedTextColor.YELLOW))
                    .append(Component.text(" limit: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.format("%.3f", d.verticalLimitApplied()), NamedTextColor.YELLOW));
        }
        var range = Component.empty();
        if (d.rangeDistance() != null && d.rangeReductionH() != null && d.rangeReductionV() != null) {
            range = Component.text(" | range: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(String.format("d=%.2f", d.rangeDistance()), NamedTextColor.GOLD))
                    .append(Component.text(" redH=", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.format("%.3f", d.rangeReductionH()), NamedTextColor.GOLD))
                    .append(Component.text(" redV=", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.format("%.3f", d.rangeReductionV()), NamedTextColor.GOLD));
        } else {
            range = Component.text(" | range: N/A (factor=0 or no origin)", NamedTextColor.DARK_GRAY);
        }
        var full = msg.append(vert).append(range);

        if (attacker instanceof Player p) p.sendMessage(full);
        if (victim instanceof Player p && p != attacker) p.sendMessage(full);
    }

    private static Component vecStr(Vec v) {
        return Component.text(String.format("(%.3f, %.3f, %.3f)", v.x(), v.y(), v.z()), NamedTextColor.AQUA);
    }
}

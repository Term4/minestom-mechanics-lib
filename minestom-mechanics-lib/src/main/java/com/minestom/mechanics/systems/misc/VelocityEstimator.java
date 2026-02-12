package com.minestom.mechanics.systems.misc;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estimates entity velocity from position deltas (PlayerTickEvent). More accurate than
 * Minestom's built-in velocity, especially for Y. Improves with higher TPS.
 * <p>
 * Must call {@link #initialize()} before use (e.g. when knockback is initialized).
 */
public final class VelocityEstimator {

    private static final Tag<Vec> ESTIMATED_VELOCITY = Tag.Transient("estimated_velocity");
    private static final Tag<Pos> LAST_POSITION = Tag.Transient("estimated_velocity_last_pos");
    private static final Logger log = LoggerFactory.getLogger(VelocityEstimator.class);

    private VelocityEstimator() {}

    public static void initialize() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerTickEvent.class, event -> {
            Player player = event.getPlayer();
            Pos currentPos = player.getPosition();
            Pos lastPos = player.getTag(LAST_POSITION);

            if (lastPos != null) {
                // Position delta per tick = velocity in blocks/tick (same units as Minestom)
                Vec estimated = new Vec(
                        currentPos.x() - lastPos.x(),
                        currentPos.y() - lastPos.y(),
                        currentPos.z() - lastPos.z()
                );
                player.setTag(ESTIMATED_VELOCITY, estimated);
            }

            player.setTag(LAST_POSITION, currentPos);
        });
    }

    /**
     * Get estimated velocity for an entity. For players, uses position-based estimation.
     * For others, falls back to entity.getVelocity() with a ground fix for phantom Y.
     */
    public static Vec getVelocity(Entity entity) {
        if (entity instanceof Player player) {
            Vec est = player.getTag(ESTIMATED_VELOCITY);
            if (est != null) return est;
        }

        Vec vel = entity.getVelocity();
        if (entity.isOnGround() && vel.y() < -0.2) {
            return new Vec(vel.x(), 0, vel.z());
        }
        return vel;
    }
}

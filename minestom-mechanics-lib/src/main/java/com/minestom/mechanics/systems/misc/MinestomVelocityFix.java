package com.minestom.mechanics.systems.misc;

import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.tag.Tag;

// TODO: Replace this with a player velocity estimation using last tick position + this tick position

public class MinestomVelocityFix {
    // Track actual velocity ourselves
    private static final Tag<Vec> REAL_VELOCITY = Tag.Transient("real_velocity");
    private static final Tag<Pos> LAST_POSITION = Tag.Transient("last_position");

    public static void initialize() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerTickEvent.class, event -> {
            Player player = event.getPlayer();

            // Calculate ACTUAL velocity from position changes
            Pos currentPos = player.getPosition();
            Pos lastPos = player.getTag(LAST_POSITION);

            if (lastPos != null) {
                // Calculate real velocity from actual movement
                double ticksPerSecond = ServerFlag.SERVER_TICKS_PER_SECOND;
                Vec actualVelocity = new Vec(
                        (currentPos.x() - lastPos.x()) * ticksPerSecond,
                        (currentPos.y() - lastPos.y()) * ticksPerSecond,
                        (currentPos.z() - lastPos.z()) * ticksPerSecond
                );

                // Store the REAL velocity
                player.setTag(REAL_VELOCITY, actualVelocity);
            }

            player.setTag(LAST_POSITION, currentPos);
        });
    }

    public static Vec getRealVelocity(Entity entity) {
        if (entity instanceof Player player) {
            Vec realVel = player.getTag(REAL_VELOCITY);
            if (realVel != null) {
                return realVel;
            }
        }

        // Fallback to Minestom's velocity (might be broken)
        Vec vel = entity.getVelocity();

        // Apply phantom velocity fix for entities on ground
        if (entity.isOnGround() && vel.y() < -0.2) {
            return new Vec(vel.x(), 0, vel.z());
        }

        return vel;
    }
}

package com.minestom.mechanics.systems.util;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CustomPhysicsPlayer extends Player {
    private double customGravityVy = 0.0;

    public CustomPhysicsPlayer(@NotNull UUID uuid, @NotNull String username,
                               @NotNull PlayerConnection playerConnection) {
        super(playerConnection, new GameProfile(uuid, username));
    }

    @Override
    public void update(long time) {
        // FIRST: Let super.update() process player input (WASD, jump, etc.)
        super.update(time);

        // THEN: Apply our custom gravity
        Double customGravity = getTag(GravitySystem.GRAVITY);
        if (customGravity != null && !isOnGround() && !isFlying()) {
            // Neutralize vanilla gravity
            Vec velocity = getVelocity();
            velocity = velocity.add(0, 0.08, 0); // Anti-gravity

            // Apply custom gravity only when falling
            if (velocity.y() <= 0) {
                velocity = velocity.sub(0, customGravity, 0);
            }

            setVelocity(velocity);
        }
    }
}
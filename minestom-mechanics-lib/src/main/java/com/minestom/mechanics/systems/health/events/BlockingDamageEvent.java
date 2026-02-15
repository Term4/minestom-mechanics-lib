package com.minestom.mechanics.systems.health.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.registry.RegistryKey;

/**
 * Fired when a player blocks damage. Listen for this to show custom feedback
 * (e.g. action bar "Blocked! X → Y" above the hotbar).
 */
public record BlockingDamageEvent(
        Player victim,
        float originalAmount,
        float reducedAmount,
        RegistryKey<?> damageType
) implements Event {}

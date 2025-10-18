package com.minestom.mechanics.features.blocking;

import com.minestom.mechanics.config.blocking.BlockingConfig;
import com.minestom.mechanics.config.blocking.BlockingPreferences;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.EntityEquipmentPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.timer.Task;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.minestom.mechanics.config.combat.CombatConstants.*;

// TODO: Would be nice if we could play the shield blocking by sending a use item packet
//  on a per viewer basis. Something to look into, but nothing to worry too much about
//  also considering adding additional visual effects for blocking. Unsure as to what
//  as of now, but it could be a cool cosmetics system maybe

/**
 * Handles visual effects for blocking - shield display, particles, animations.
 * Focused responsibility: Visual effects only.
 */
public class BlockingVisualEffects {

    private final BlockingConfig config;
    private final BlockingStateManager stateManager;
    private final Map<UUID, Task> particleTasks = new ConcurrentHashMap<>();

    public BlockingVisualEffects(BlockingConfig config, BlockingStateManager stateManager) {
        this.config = config;
        this.stateManager = stateManager;
    }

    /**
     * Update blocking visuals for a player
     */
    public void updateBlockingVisuals(Player blockingPlayer, boolean blocking) {
        BlockingPreferences selfPrefs = blockingPlayer.getTag(BlockingStateManager.PREFERENCES);
        ItemStack original = blockingPlayer.getTag(BlockingStateManager.ORIGINAL_OFFHAND);
        if (original == null) original = ItemStack.AIR;

        // Update for other viewers
        for (Player viewer : blockingPlayer.getViewers()) {
            BlockingPreferences viewerPrefs = viewer.getTag(BlockingStateManager.PREFERENCES);

            ItemStack toShow = blocking && viewerPrefs != null && viewerPrefs.showShieldOnOthers
                    ? ItemStack.of(Material.SHIELD)
                    : original;

            viewer.sendPacket(new EntityEquipmentPacket(
                    blockingPlayer.getEntityId(),
                    Map.of(EquipmentSlot.OFF_HAND, toShow)
            ));
        }

        // Update for self
        ItemStack selfOffhand = blocking && selfPrefs != null && selfPrefs.showShieldOnSelf
                ? ItemStack.of(Material.SHIELD)
                : original;

        blockingPlayer.sendPacket(new EntityEquipmentPacket(
                blockingPlayer.getEntityId(),
                Map.of(EquipmentSlot.OFF_HAND, selfOffhand)
        ));
    }

    /**
     * Start particle effects for a blocking player
     */
    public void startParticleTask(Player player) {
        UUID uuid = player.getUuid();

        // Cancel existing task if any
        stopParticleTask(player);

        BlockingPreferences prefs = player.getTag(BlockingStateManager.PREFERENCES);
        if (prefs == null) return;

        // Check if particles should be shown
        boolean showParticles = prefs.showParticlesOnSelf || prefs.showParticlesOnOthers;
        if (!showParticles || !config.isShowBlockEffects()) return;

        Task task = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    if (!stateManager.isBlocking(player)) {
                        stopParticleTask(player);
                        return;
                    }
                    spawnBlockingParticles(player, prefs);
                })
                .repeat(Duration.ofMillis(PARTICLE_UPDATE_INTERVAL_MS))
                .schedule();

        particleTasks.put(uuid, task);
    }

    /**
     * Stop particle effects for a player
     */
    public void stopParticleTask(Player player) {
        UUID uuid = player.getUuid();
        Task task = particleTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Spawn blocking particles around a player
     */
    private void spawnBlockingParticles(Player player, BlockingPreferences prefs) {
        Pos pos = player.getPosition();
        double x = pos.x();
        double y = pos.y() + PARTICLE_Y_OFFSET;
        double z = pos.z();

        int count = prefs.particleCount;
        double radius = BLOCKING_PARTICLE_RADIUS;

        // Pre-calculate all particle positions
        List<ParticlePacket> packets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double px = x + radius * Math.cos(angle);
            double pz = z + radius * Math.sin(angle);
            packets.add(createParticlePacket(prefs.particleType, px, y, pz));
        }

        // Batch send to viewers
        for (Player viewer : player.getViewers()) {
            BlockingPreferences viewerPrefs = viewer.getTag(BlockingStateManager.PREFERENCES);
            if (viewerPrefs != null && viewerPrefs.showParticlesOnOthers) {
                packets.forEach(viewer::sendPacket);
            }
        }

        // Batch send to self
        if (prefs.showParticlesOnSelf) {
            packets.forEach(player::sendPacket);
        }
    }

    /**
     * Create a particle packet based on the particle type
     */
    private ParticlePacket createParticlePacket(BlockingPreferences.ParticleType type,
                                                double x, double y, double z) {
        return switch (type) {
            case DUST -> new ParticlePacket(
                    Particle.DUST.withColor(new net.minestom.server.color.Color(255, 0, 0)),
                    x, y, z, 0, 0, 0, 0, 1
            );
            case BLOCK -> new ParticlePacket(
                    Particle.BLOCK.withBlock(net.minestom.server.instance.block.Block.STONE),
                    x, y, z, 0, 0, 0, 0, 1
            );
            case ITEM -> new ParticlePacket(
                    Particle.ITEM.withItem(ItemStack.of(Material.SLIME_BALL)),
                    x, y, z, 0, 0, 0, 0, 1
            );
            default -> new ParticlePacket(
                    type.particle,
                    x, y, z, 0, 0, 0, 0, 1
            );
        };
    }

    /**
     * Show action bar message for blocking
     */
    public void showBlockingMessage(Player player, boolean blocking) {
        BlockingPreferences prefs = player.getTag(BlockingStateManager.PREFERENCES);
        if (prefs != null && prefs.showActionBarOnBlock) {
            if (blocking) {
                player.sendActionBar(Component.text("⚔ Blocking ⚔", NamedTextColor.YELLOW));
            } else {
                player.sendActionBar(Component.empty());
            }
        }
    }

    /**
     * Clean up visual effects for a player
     */
    public void cleanup(Player player) {
        stopParticleTask(player);
    }

    /**
     * Clean up all visual effects
     */
    public void shutdown() {
        for (Task task : particleTasks.values()) {
            if (task != null) task.cancel();
        }
        particleTasks.clear();
    }
}

package com.minestom.mechanics.systems.compatibility.movement;

import com.minestom.mechanics.config.gameplay.MovementConfig;
import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.block.Block;

// TODO: Move to (and create) player package, and make this configurable
//  ALSO look for a better way to prevent poses in the minestom javadocs, cancelling them
//  right when they enter causes stuttering and is hacky at best
//  If there's a native way to prevent poses, would also shorten this by allowing
//  the removal of corrective velocity measures

/**
 * MovementRestrictionSystem - Controls which movement mechanics are allowed.
 * 
 * Can disable modern movement features like swimming, crawling, elytra flying
 * for servers that want to maintain older gameplay mechanics.
 */
public class MovementRestrictionSystem extends InitializableSystem {
    private static MovementRestrictionSystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("MovementRestrictionSystem");

    // ===========================
    // CONSTANTS
    // ===========================

    /** 1.8 water movement speed multiplier */

    private final MovementConfig config;
    private int tickCounter = 0;

    private MovementRestrictionSystem(MovementConfig config) {
        this.config = config;
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static MovementRestrictionSystem initialize(MovementConfig config) {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("MovementRestrictionSystem");
            return instance;
        }
        instance = new MovementRestrictionSystem(config);
        instance.registerListeners();
        instance.markInitialized();
        LogUtil.system("MovementRestrictionSystem").debug("Movement restrictions active (swimming: {}, crawling: {}, elytra: {})", 
                config.allowSwimming(), config.allowCrawling(), config.allowElytraFlying());
        return instance;
    }

    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Initialize on spawn
        handler.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            correctPlayerPose(player);
        });

        // Enforce every configured interval
        handler.addListener(PlayerTickEvent.class, event -> {
            tickCounter++;
            if (tickCounter >= config.checkIntervalTicks()) {
                tickCounter = 0;
                correctPlayerPose(event.getPlayer());
            }
        });

        // Prevent restricted movement
        handler.addListener(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();

            // If player tries to enter restricted state, block it
            if (isInWater(player) && !config.allowSwimming()) {
                preventSwimming(player);
            }
        });

        log.debug("Movement restrictions active");
    }

    // ===========================
    // CORE LOGIC
    // ===========================

    /**
     * Correct player pose to match allowed poses.
     */
    public void correctPlayerPose(Player player) {
        // Check if player is in custom spectator mode (via tag)
        boolean isCustomSpectator = Boolean.TRUE.equals(player.getTag(
                net.minestom.server.tag.Tag.Boolean("spectator_mode")));
        
        // Force disable flying (unless creative, spectator mode, or custom spectator tag)
        if (player.isFlying() && player.getGameMode() != net.minestom.server.entity.GameMode.CREATIVE 
                && player.getGameMode() != net.minestom.server.entity.GameMode.SPECTATOR
                && !isCustomSpectator) {
            player.setFlying(false);
        }
        
        // Force enable flying for custom spectators
        if (isCustomSpectator && !player.isFlying()) {
            player.setAllowFlying(true);
            player.setFlying(true);
        }

        // Ensure pose is allowed
        EntityPose pose = player.getPose();
        /*if (!isPoseAllowed(pose)) {
            player.setPose(EntityPose.STANDING);

            // Force sync to viewers
            player.sendPacketToViewers(player.getMetadataPacket());

            log.debug("{} corrected from pose {} to STANDING",
                    player.getUsername(), pose);
        }*/

        // If in water, apply water physics if swimming not allowed
        if (isInWater(player) && !config.allowSwimming()) {
            applyWaterPhysics(player);
        }
    }

    /**
     * Check if a pose is allowed by the configuration.
     * 
     * @param pose The pose to check
     * @return true if the pose is allowed, false otherwise
     */
    public boolean isPoseAllowed(EntityPose pose) {
        return config.allowedPoses().contains(pose);
    }

    /**
     * Prevent player from entering swimming state.
     */
    private void preventSwimming(Player player) {
        EntityPose pose = player.getPose();

        // If trying to swim, force standing
        if (pose == EntityPose.SWIMMING) {
            player.setPose(EntityPose.STANDING);
            player.sendPacketToViewers(player.getMetadataPacket());
            log.debug("{} tried to swim, blocked", player.getUsername());
        }
    }

    /**
     * Apply water physics (slow movement, no swimming).
     */
    private void applyWaterPhysics(Player player) {
        EntityPose pose = player.getPose();

        // Only allow STANDING or SNEAKING in water
        if (pose != EntityPose.STANDING && pose != EntityPose.SNEAKING) {
            player.setPose(EntityPose.STANDING);
            player.sendPacketToViewers(player.getMetadataPacket());
        }
    }

    /**
     * Check if player is in water.
     */
    private boolean isInWater(Player player) {
        try {
            // Check if player's position has water
            Block block = player.getInstance().getBlock(player.getPosition());
            String blockName = block.name();

            return blockName.equals("minecraft:water") ||
                    blockName.equals("minecraft:flowing_water") ||
                    blockName.contains("WATER");
        } catch (Exception e) {
            return false;
        }
    }

    // ===========================
    // PUBLIC API
    // ===========================

    /**
     * Check if a player is allowed to use modern movement.
     * 
     * @return true if modern movement is allowed, false otherwise
     */
    public boolean isModernMovementAllowed() {
        return config.allowSwimming() && config.allowCrawling() && 
               config.allowElytraFlying() && config.allowSpinAttack();
    }

    /**
     * Force disable all restricted movement for a player.
     * (Useful for testing or manual correction)
     * 
     * @param player The player to correct
     */
    public void forceCorrectPlayer(Player player) {
        correctPlayerPose(player);
        log.info("Manually corrected movement for: {}", player.getUsername());
    }

    public static MovementRestrictionSystem getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MovementRestrictionSystem not initialized!");
        }
        return instance;
    }
}

package com.minestom.mechanics.systems.player;

import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

/**
 * Handles player death and respawn logic, including visibility and hitbox management.
 * Manages the death animation delay and ensures proper cleanup on respawn.
 */
public class PlayerDeathHandler extends InitializableSystem {
    private static PlayerDeathHandler instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("PlayerDeathHandler");
    
    /** Tag to track if a player is dead (for visibility/hitbox removal) */
    public static final Tag<Boolean> IS_DEAD = Tag.Boolean("health_is_dead");
    
    /** Default player bounding box (0.6 width, 1.8 height, 0.6 depth) */
    private static final BoundingBox DEFAULT_PLAYER_BOX = new BoundingBox(0.6, 1.8, 0.6);
    
    /** Zero bounding box for dead players (no collision) */
    private static final BoundingBox ZERO_BOX = new BoundingBox(0, 0, 0);
    
    private PlayerDeathHandler() {
        // Private constructor for singleton
    }
    
    // ===========================
    // INITIALIZATION
    // ===========================
    
    public static PlayerDeathHandler initialize() {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("PlayerDeathHandler");
            return instance;
        }
        
        instance = new PlayerDeathHandler();
        instance.registerListeners();
        instance.markInitialized();
        
        LogUtil.logInit("PlayerDeathHandler");
        return instance;
    }
    
    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();
        
        // When a new player spawns, hide all dead players from them
        handler.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                Player newPlayer = event.getPlayer();
                var instance = newPlayer.getInstance();
                if (instance != null) {
                    // Send destroy packets for all dead players in the instance
                    for (Player otherPlayer : instance.getPlayers()) {
                        if (otherPlayer != newPlayer && Boolean.TRUE.equals(otherPlayer.getTag(IS_DEAD))) {
                            DestroyEntitiesPacket destroyPacket = new DestroyEntitiesPacket(otherPlayer.getEntityId());
                            newPlayer.sendPacket(destroyPacket);
                            log.debug("Sent destroy packet for dead player {} to new player {}", 
                                    otherPlayer.getUsername(), newPlayer.getUsername());
                        }
                    }
                }
            }
        });
        
        // Player spawn events (for respawn handling)
        handler.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            
            // Restore visibility and hitbox if player was dead
            // The canHit method will also check health > 0 as a backup
            if (Boolean.TRUE.equals(player.getTag(IS_DEAD))) {
                player.removeTag(IS_DEAD);
                
                // Restore bounding box so projectiles can hit them again
                player.setBoundingBox(DEFAULT_PLAYER_BOX);
                
                // Re-spawn player entity for all viewers to make them visible again
                // Send spawn packet to all current viewers
                Pos pos = player.getPosition();
                Vec velocity = player.getVelocity();
                Vec velocityPerTick = velocity.div(net.minestom.server.ServerFlag.SERVER_TICKS_PER_SECOND);
                
                SpawnEntityPacket spawnPacket = new SpawnEntityPacket(
                        player.getEntityId(),
                        player.getUuid(),
                        player.getEntityType(),
                        pos,
                        pos.yaw(),
                        0, // data (not used for players)
                        velocityPerTick
                );
                
                player.sendPacketToViewers(spawnPacket);
                player.sendPacketToViewers(player.getMetadataPacket());
                
                log.debug("Restored visibility and hitbox for {} on respawn", player.getUsername());
            }
        });
        
        // Player death events
        handler.addListener(PlayerDeathEvent.class, event -> {
            Player player = event.getPlayer();
            
            // Mark player as dead and remove hitbox immediately
            // This prevents dead players from blocking arrows, picking up items, etc.
            player.setTag(IS_DEAD, true);
            player.setBoundingBox(ZERO_BOX);
            
            // Delay hiding the player by ~1 second (20 ticks) so players can see the death animation
            MinecraftServer.getSchedulerManager().buildTask(() -> {
                // Double-check player is still dead and hasn't respawned
                if (Boolean.TRUE.equals(player.getTag(IS_DEAD)) && !player.isRemoved()) {
                    // Hide player from all viewers by sending destroy entity packet
                    // This makes them invisible without disconnecting them
                    DestroyEntitiesPacket destroyPacket = new DestroyEntitiesPacket(player.getEntityId());
                    player.sendPacketToViewers(destroyPacket);
                    
                    log.debug("Sent destroy packet for {} after death animation delay", player.getUsername());
                }
            }).delay(TaskSchedule.tick(20)).schedule();
            
            log.debug("Removed hitbox for {} on death (visibility will be hidden after 1 second)", player.getUsername());
            
            // Cancel death messages to prevent 1.7 client crashes with incomplete TranslatableComponent
            event.setDeathText(null);
            event.setChatMessage(null);
        });
    }
    
    // ===========================
    // STATIC ACCESS
    // ===========================
    
    public static PlayerDeathHandler getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlayerDeathHandler not initialized!");
        }
        return instance;
    }
}


package com.minestom.mechanics.features.gameplay;

import com.minestom.mechanics.config.gameplay.PlayerCollisionConfig;
import com.minestom.mechanics.util.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.server.network.packet.server.play.TeamsPacket;

// TODO: Make sure is thread / memory safe (should at least be thread safe)
//  and move to player package when it is made

/**
 * System to control player collisions.
 * When disabled, players can pass through each other.
 */
public class PlayerCollisionSystem extends InitializableSystem {
    private static PlayerCollisionSystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("PlayerCollisionSystem");
    
    private PlayerCollisionConfig config;
    private boolean enabled = true;
    
    // Team for collision control
    private Team collisionTeam;
    
    private PlayerCollisionSystem(PlayerCollisionConfig config) {
        this.config = config;
        this.enabled = config.enabled();
    }
    
    public static PlayerCollisionSystem initialize(PlayerCollisionConfig config) {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("PlayerCollisionSystem");
            return instance;
        }
        
        instance = new PlayerCollisionSystem(config);
        instance.initializeTeam();
        instance.registerListeners();
        instance.markInitialized();
        
        LogUtil.logInit("PlayerCollisionSystem");
        return instance;
    }
    
    public static PlayerCollisionSystem getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlayerCollisionSystem not initialized!");
        }
        return instance;
    }
    
    
    private void initializeTeam() {
        // Create a team for collision control
        TeamManager teamManager = MinecraftServer.getTeamManager();
        collisionTeam = teamManager.createTeam("collision_control");
        collisionTeam.setCollisionRule(TeamsPacket.CollisionRule.ALWAYS); // Default to collisions enabled
    }

    // TODO: Could be good to have a single initialization / register listeners method for all
    //  systems, features, etc. To avoid listener spam and potential memory leaks. THIS IS A VERY IMPORTANT NOTE!!!

    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();
        
        // Listen for player spawn events to add them to the collision team
        handler.addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn);
    }
    
    private void handlePlayerSpawn(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        
        // Add player to collision team
        collisionTeam.addMember(player.getUsername());
        
        // Update collision rule based on current setting
        updatePlayerCollision(player);
        
        log.debug("Added player {} to collision team", player.getUsername());
    }


    // TODO: I like this, it falls in line with what I mentioned earlier about more future-proofing this library
    /**
     * Update collision rule for a specific player
     */
    private void updatePlayerCollision(Player player) {
        if (!enabled) {
            // Disable collisions by setting team collision rule to NEVER
            collisionTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);
            log.debug("Disabled collisions for player: {}", player.getUsername());
        } else {
            // Enable collisions by setting team collision rule to ALWAYS
            collisionTeam.setCollisionRule(TeamsPacket.CollisionRule.ALWAYS);
            log.debug("Enabled collisions for player: {}", player.getUsername());
        }
    }
    
    /**
     * Enable or disable player collisions at runtime
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        
        // Update collision rule for all players in the team
        if (collisionTeam != null) {
            if (enabled) {
                collisionTeam.setCollisionRule(TeamsPacket.CollisionRule.ALWAYS);
            } else {
                collisionTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);
            }
        }
        
        log.info("Player collisions {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Check if player collisions are enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Update configuration
     */
    public void updateConfig(PlayerCollisionConfig newConfig) {
        this.config = newConfig;
        this.enabled = newConfig.enabled();
        log.info("PlayerCollisionSystem config updated (collisions: {})", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Get current configuration
     */
    public PlayerCollisionConfig getConfig() {
        return config;
    }
}

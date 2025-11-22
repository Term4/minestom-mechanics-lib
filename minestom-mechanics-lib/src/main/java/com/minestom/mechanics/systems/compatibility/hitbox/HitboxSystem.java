package com.minestom.mechanics.systems.compatibility.hitbox;

import com.minestom.mechanics.config.gameplay.HitboxConfig;
import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.Shape;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.block.Block;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Pay very close attention to this class when refactoring, and it WILL require a major refactor.
//  It is VERY monolithic, and it has many duplicate methods seen in other classes earlier (starting from
//  the attack package and working your way down)

/**
 * HitboxSystem - Enforces specific hitbox dimensions and collision.
 * 
 * Can enforce fixed hitbox dimensions (like 1.8's 0.6x1.8x0.6) with precise
 * collision detection using actual block shapes.
 */
public class HitboxSystem extends InitializableSystem {

    private static HitboxSystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("HitboxSystem");

    // ===========================
    // CONSTANTS
    // ===========================

    // ZERO tolerance for clipping
    private static final double COLLISION_EPSILON = 0.0;

    // State tracking
    private final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();
    private final HitboxConfig config;
    private int tickCounter = 0;

    private HitboxSystem(HitboxConfig config) {
        this.config = config;
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static HitboxSystem initialize(HitboxConfig config) {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("HitboxSystem");
            return instance;
        }

        instance = new HitboxSystem(config);
        instance.registerListeners();
        instance.markInitialized();

        LogUtil.system("HitboxSystem").debug("Hitbox enforcement active ({}x{}x{})", 
                config.width(), config.height(), config.width());
        return instance;
    }

    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();

        handler.addListener(PlayerSpawnEvent.class, this::onPlayerSpawn);
        handler.addListener(PlayerMoveEvent.class, this::onPlayerMove);
        handler.addListener(PlayerTickEvent.class, this::onPlayerTick);
        handler.addListener(PlayerStartSneakingEvent.class, e -> enforceHitbox(e.getPlayer()));
        handler.addListener(PlayerStopSneakingEvent.class, e -> enforceHitbox(e.getPlayer()));
        handler.addListener(PlayerDisconnectEvent.class, e -> playerStates.remove(e.getPlayer().getUuid()));

        log.debug("Hitbox enforcement active - precise shape checking (slabs, stairs, etc.)");
    }

    // ===========================
    // EVENT HANDLERS
    // ===========================

    private void onPlayerSpawn(PlayerSpawnEvent event) {
        if (event.isFirstSpawn()) {
            Player player = event.getPlayer();
            playerStates.put(player.getUuid(), new PlayerState());
            enforceHitbox(player);
        }
    }

    // TODO: What does this do?

    /**
     * Validates movement with PRECISE collision shape checking
     */
    private void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        enforceHitbox(player);

        // Skip collision checks for spectators (they can noclip)
        if (isSpectator(player)) {
            return; // Spectators can move freely
        }

        Pos oldPos = player.getPosition();
        Pos newPos = event.getNewPosition();

        // CRITICAL: Check DESTINATION position with full shape collision
        if (!isPositionValid(player, newPos)) {
            // Try sliding
            Pos slidingPos = calculateSlidingPosition(player, oldPos, newPos);

            if (slidingPos != null) {
                event.setNewPosition(slidingPos);
                PlayerState state = playerStates.computeIfAbsent(player.getUuid(), _ -> new PlayerState());
                state.slidingCount++;
            } else {
                // Block movement completely
                event.setCancelled(true);
                PlayerState state = playerStates.computeIfAbsent(player.getUuid(), _ -> new PlayerState());
                state.blockedCount++;
            }
        }
    }

    private void onPlayerTick(PlayerTickEvent event) {
        tickCounter++;
        if (tickCounter % config.validationIntervalTicks() == 0) {
            Player player = event.getPlayer();
            enforceHitbox(player);

            // Force correction if invalid
            Pos currentPos = player.getPosition();
            if (!isPositionValid(player, currentPos)) {
                Pos correctedPos = findNearestValidPosition(player, currentPos);
                if (correctedPos != null && !correctedPos.samePoint(currentPos)) {
                    player.teleport(correctedPos);
                    log.warn("Corrected clipping for {} (Y: {:.3f} -> {:.3f})",
                            player.getUsername(), currentPos.y(), correctedPos.y());
                }
            }
        }
    }

    // ===========================
    // SPECTATOR MODE CHECK
    // ===========================
    
    /**
     * Check if a player is in spectator mode (for 1.7 clients emulating spectator mode).
     * Uses reflection to avoid dependency on test server code.
     */
    @SuppressWarnings("unchecked")
    private boolean isSpectator(Player player) {
        try {
            var spectatorTag = Class.forName("com.test.minestom.commands.SpectatorCommand")
                    .getField("SPECTATOR_MODE").get(null);
            return Boolean.TRUE.equals(player.getTag((net.minestom.server.tag.Tag<Boolean>) spectatorTag));
        } catch (Exception e) {
            // SpectatorCommand not available, player is not a spectator
            return false;
        }
    }
    
    // ===========================
    // PRECISE COLLISION CHECKING
    // ===========================

    /**
     * PRECISE collision check using actual block shapes
     * 
     * Handles slabs, stairs, and all partial blocks correctly.
     * NO tolerance - if player bounding box intersects block shape, it's invalid.
     */
    public boolean isPositionValid(Player player, Pos pos) {
        var instance = player.getInstance();
        if (instance == null) return true;

        // Skip collision checks for spectators (they can noclip)
        if (isSpectator(player)) {
            return true; // Spectators can pass through blocks
        }

        // Calculate block range to check (player hitbox extends 0.3 blocks horizontally, 1.8 up)
        double halfWidth = config.width() / 2;
        int minBlockX = (int) Math.floor(pos.x() - halfWidth);
        int minBlockY = (int) Math.floor(pos.y());
        int minBlockZ = (int) Math.floor(pos.z() - halfWidth);
        int maxBlockX = (int) Math.floor(pos.x() + halfWidth);
        int maxBlockY = (int) Math.floor(pos.y() + config.height());
        int maxBlockZ = (int) Math.floor(pos.z() + halfWidth);

        // Check each block in range
        for (int bx = minBlockX; bx <= maxBlockX; bx++) {
            for (int by = minBlockY; by <= maxBlockY; by++) {
                for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                    Block block = instance.getBlock(bx, by, bz);

                    if (!block.isSolid()) continue;

                    // Get the actual collision shape of this block
                    Shape shape = block.registry().collisionShape();

                    // CRITICAL: Position the BoundingBox at CENTER-BOTTOM (like entity positions)
                    // NOT at the min corner!
                    Point relativePos = new Pos(
                            pos.x() - bx,  // center X relative to block
                            pos.y() - by,  // bottom Y relative to block
                            pos.z() - bz   // center Z relative to block
                    );

                    // Check intersection with player's hitbox
                    BoundingBox playerHitbox = getPlayerHitbox(player);
                    if (shape.intersectBox(relativePos, playerHitbox)) {
                        return false;  // Collision detected!
                    }
                }
            }
        }

        return true;  // No collisions
    }

    /**
     * Find nearest valid position by pushing player down
     */
    private Pos findNearestValidPosition(Player player, Pos invalidPos) {
        // Try moving down in small increments
        for (double yOffset = 0; yOffset >= -1.0; yOffset -= 0.05) {
            Pos testPos = invalidPos.add(0, yOffset, 0);
            if (isPositionValid(player, testPos)) {
                return testPos;
            }
        }
        return null;
    }

    // TODO: This is pretty good, could be better, but might as well try and improve it anyways.
    //  See if there's a way to maybe use ghost blocks or something to prevent ping from playing a role in
    //  sliding-smoothness.
    //  ALSO this is very mathy and long. Could be moved to aformentioned math / physics / utils class.

    // ===========================
    // COLLISION SLIDING
    // ===========================

    /**
     * Calculate valid sliding position
     * Every slide is validated with PRECISE collision checking
     */
    private Pos calculateSlidingPosition(Player player, Pos oldPos, Pos newPos) {
        double dx = newPos.x() - oldPos.x();
        double dy = newPos.y() - oldPos.y();
        double dz = newPos.z() - oldPos.z();

        float yaw = newPos.yaw();
        float pitch = newPos.pitch();

        // Try horizontal only
        if (Math.abs(dx) > COLLISION_EPSILON || Math.abs(dz) > COLLISION_EPSILON) {
            Pos horizontal = oldPos.withX(newPos.x()).withZ(newPos.z()).withView(yaw, pitch);
            if (isPositionValid(player, horizontal)) {
                return horizontal;
            }
        }

        // Try X only
        if (Math.abs(dx) > COLLISION_EPSILON) {
            Pos xOnly = oldPos.withX(newPos.x()).withView(yaw, pitch);
            if (isPositionValid(player, xOnly)) {
                return xOnly;
            }
        }

        // Try Z only
        if (Math.abs(dz) > COLLISION_EPSILON) {
            Pos zOnly = oldPos.withZ(newPos.z()).withView(yaw, pitch);
            if (isPositionValid(player, zOnly)) {
                return zOnly;
            }
        }

        // Try vertical only
        if (Math.abs(dy) > COLLISION_EPSILON) {
            Pos vertical = oldPos.withY(newPos.y()).withView(yaw, pitch);
            if (isPositionValid(player, vertical)) {
                return vertical;
            }
        }

        return null;
    }

    // ===========================
    // HITBOX ENFORCEMENT
    // ===========================

    /**
     * Enforce configured hitbox dimensions
     */
    public void enforceHitbox(Player player) {
        BoundingBox current = player.getBoundingBox();
        BoundingBox target = getPlayerHitbox(player);
        
        if (!current.equals(target)) {
            player.setBoundingBox(target);
        }
    }

    /**
     * Get the player's hitbox based on configuration and sneaking state.
     */
    private BoundingBox getPlayerHitbox(Player player) {
        double height = config.height();
        
        // Check if height should change when sneaking
        if (config.heightChangesOnSneak() && isSneaking(player)) {
            height = config.sneakingHeight();
        }
        
        return new BoundingBox(config.width(), height, config.width());
    }

    /**
     * Check if player is sneaking (simplified check).
     */
    private boolean isSneaking(Player player) {
        return player.getPose() == net.minestom.server.entity.EntityPose.SNEAKING;
    }

    // ===========================
    // DEBUGGING
    // ===========================

    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("╔═══════════════════════════════════════╗\n");
        stats.append("║      HitboxSystem Statistics          ║\n");
        stats.append("╚═══════════════════════════════════════╝\n");
        stats.append(String.format("  Tracked Players: %d\n", playerStates.size()));

        int totalSliding = playerStates.values().stream().mapToInt(s -> s.slidingCount).sum();
        int totalBlocked = playerStates.values().stream().mapToInt(s -> s.blockedCount).sum();
        int total = totalSliding + totalBlocked;

        stats.append(String.format("  Sliding: %d (%.1f%%)\n",
                totalSliding, total > 0 ? 100.0 * totalSliding / total : 0));
        stats.append(String.format("  Blocked: %d (%.1f%%)\n",
                totalBlocked, total > 0 ? 100.0 * totalBlocked / total : 0));

        return stats.toString();
    }

    public void debugPlayer(Player player) {
        Pos pos = player.getPosition();
        double headY = pos.y() + config.height();
        boolean isValid = isPositionValid(player, pos);

        log.info("=== Debug: {} ===", player.getUsername());
        log.info("  Position: {:.3f}, {:.3f}, {:.3f}", pos.x(), pos.y(), pos.z());
        log.info("  Height: {:.2f}", config.height());
        log.info("  Head at: {:.3f}", headY);
        log.info("  Valid: {}", isValid);
        log.info("  Hitbox: {}", player.getBoundingBox());

        // Check blocks above with shapes
        int baseY = pos.blockY();
        int baseX = pos.blockX();
        int baseZ = pos.blockZ();

        for (int dy = 0; dy <= 2; dy++) {
            Block block = player.getInstance().getBlock(baseX, baseY + dy, baseZ);
            if (block.isSolid()) {
                Shape shape = block.registry().collisionShape();
                log.info("  Y+{}: {} (shape bounds: {})", dy, block.name(),
                        shape.relativeEnd());
            }
        }
    }

    // ===========================
    // STATE TRACKING
    // ===========================

    public static class PlayerState {
        public int slidingCount = 0;
        public int blockedCount = 0;
    }

    public static HitboxSystem getInstance() {
        if (instance == null) {
            throw new IllegalStateException("HitboxSystem not initialized!");
        }
        return instance;
    }
}

package com.minestom.mechanics.systems.compatibility.hitbox;

import com.minestom.mechanics.config.gameplay.EyeHeightConfig;
import com.minestom.mechanics.InitializableSystem;
import com.minestom.mechanics.util.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.event.player.PlayerStopSneakingEvent;
import net.minestom.server.instance.block.BlockFace;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EyeHeightSystem - Enforces specific eye heights for cross-version fairness.
 * 
 * Useful for normalizing reach mechanics across different Minecraft versions.
 * Can enforce 1.8 eye heights (1.62 standing, 1.54 sneaking) for competitive balance.
 */
public class EyeHeightSystem extends InitializableSystem {
    private static EyeHeightSystem instance;
    private static final LogUtil.SystemLogger log = LogUtil.system("EyeHeightSystem");

    // ===========================
    // CONSTANTS
    // ===========================

    /** Standard block size in Minecraft */
    private static final double BLOCK_SIZE = 1.0;

    /** Y offset for adding eye height to position */
    private static final double EYE_HEIGHT_Y_OFFSET = 0.0;

    // ===========================
    // STATE TRACKING
    // ===========================

    private final Map<UUID, Boolean> sneakingPlayers = new ConcurrentHashMap<>();
    private final EyeHeightConfig config;

    private EyeHeightSystem(EyeHeightConfig config) {
        this.config = config;
    }

    // ===========================
    // INITIALIZATION
    // ===========================

    public static EyeHeightSystem initialize(EyeHeightConfig config) {
        if (instance != null && instance.isInitialized()) {
            LogUtil.logAlreadyInitialized("EyeHeightSystem");
            return instance;
        }
        instance = new EyeHeightSystem(config);
        instance.registerListeners();
        instance.markInitialized();
        LogUtil.system("EyeHeightSystem").debug("Eye height enforcement enabled (standing: {:.2f}, sneaking: {:.2f})", 
                config.standingEyeHeight(), config.sneakingEyeHeight());
        return instance;
    }

    private void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Track sneaking state
        handler.addListener(PlayerStartSneakingEvent.class, e -> {
            sneakingPlayers.put(e.getPlayer().getUuid(), true);
            log.debug("{} started sneaking (eye: {:.2f})",
                    e.getPlayer().getUsername(), config.sneakingEyeHeight());
        });

        handler.addListener(PlayerStopSneakingEvent.class, e -> {
            sneakingPlayers.remove(e.getPlayer().getUuid());
            log.debug("{} stopped sneaking (eye: {:.2f})",
                    e.getPlayer().getUsername(), config.standingEyeHeight());
        });

        // Validate block placement reach if enabled
        if (config.enforceBlockPlaceReach()) {
            handler.addListener(PlayerBlockPlaceEvent.class, this::validateBlockPlacement);
        }

        // TODO: There's that cleanup on disconnect again. Really should create some cleanup class(es)
        //  in the future player package to prevent all this duplication.

        // Cleanup on disconnect
        handler.addListener(PlayerDisconnectEvent.class, e ->
                sneakingPlayers.remove(e.getPlayer().getUuid()));

        log.debug("Eye height enforcement active");
    }

    // TODO: This part feels like more of a subsystem, and could probably be moved
    //  out to a separate class for organization.

    // ===========================
    // BLOCK PLACEMENT VALIDATION
    // ===========================

    private void validateBlockPlacement(PlayerBlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Get corrected eye position
        Pos eye = getEyePosition(player);

        // Get appropriate reach distance for player's game mode
        double maxReach = getReach(player);

        // Calculate distance to clicked face of neighbor block
        BlockFace clickedFace = event.getBlockFace();
        Point placedPos = event.getBlockPosition();
        Point neighbor = getNeighborBlock(placedPos, clickedFace);

        double distance = distanceToFace(eye, neighbor, clickedFace);

        // Enforce reach limit
        if (distance > maxReach) {
            event.setCancelled(true);

            log.debug("[DENY] {} placing at {} | face={} neighbor={} | " +
                            "eye={} dist={:.3f} > {:.1f} (mode={} sneaking={})",
                    player.getUsername(),
                    formatPoint(placedPos),
                    clickedFace.name(),
                    formatPoint(neighbor),
                    formatPos(eye),
                    distance,
                    maxReach,
                    player.getGameMode().name(),
                    isSneaking(player));
        }
    }

    // ===========================
    // PUBLIC API
    // ===========================

    /**
     * Get player's eye position using configured eye heights.
     * 
     * @param player The player
     * @return Eye position with configured height
     */
    public Pos getEyePosition(Player player) {
        double eyeHeight = isSneaking(player) ? config.sneakingEyeHeight() : config.standingEyeHeight();
        return player.getPosition().add(EYE_HEIGHT_Y_OFFSET, eyeHeight, EYE_HEIGHT_Y_OFFSET);
    }

    // TODO: NOTE a similar method exists in the server config / world interaction config (can't remember rn)
    //  But we should avoid duplicate methods like this. Consolidate.

    /**
     * Get the appropriate reach distance for a player based on game mode.
     * 
     * @param player The player
     * @return Reach distance based on game mode
     */
    public double getReach(Player player) {
        return player.getGameMode() == GameMode.CREATIVE
                ? config.creativeReach()
                : config.survivalReach();
    }

    /**
     * Check if player is currently sneaking.
     * 
     * @param player The player
     * @return true if sneaking, false otherwise
     */
    public boolean isSneaking(Player player) {
        return sneakingPlayers.getOrDefault(player.getUuid(), false);
    }

    // ===========================
    // HELPER METHODS
    // ===========================

    // TODO: This distanceToFace helper method could be useful later, and is very mathy.
    //  Consider moving it to the aformentioned math / physics / utils class.

    /**
     * Calculate distance from point to the nearest point on a block face.
     */
    private double distanceToFace(Pos point, Point block, BlockFace face) {
        // Calculate block bounds
        double minX = block.blockX();
        double minY = block.blockY();
        double minZ = block.blockZ();
        double maxX = minX + BLOCK_SIZE;
        double maxY = minY + BLOCK_SIZE;
        double maxZ = minZ + BLOCK_SIZE;

        // Find closest point on the face rectangle
        double closestX, closestY, closestZ;

        switch (face) {
            case TOP:    // y = maxY plane
                closestY = maxY;
                closestX = clamp(point.x(), minX, maxX);
                closestZ = clamp(point.z(), minZ, maxZ);
                break;
            case BOTTOM: // y = minY plane
                closestY = minY;
                closestX = clamp(point.x(), minX, maxX);
                closestZ = clamp(point.z(), minZ, maxZ);
                break;
            case NORTH:  // z = minZ plane
                closestZ = minZ;
                closestX = clamp(point.x(), minX, maxX);
                closestY = clamp(point.y(), minY, maxY);
                break;
            case SOUTH:  // z = maxZ plane
                closestZ = maxZ;
                closestX = clamp(point.x(), minX, maxX);
                closestY = clamp(point.y(), minY, maxY);
                break;
            case WEST:   // x = minX plane
                closestX = minX;
                closestY = clamp(point.y(), minY, maxY);
                closestZ = clamp(point.z(), minZ, maxZ);
                break;
            case EAST:   // x = maxX plane
                closestX = maxX;
                closestY = clamp(point.y(), minY, maxY);
                closestZ = clamp(point.z(), minZ, maxZ);
                break;
            default:
                // Fallback to closest point on entire AABB
                closestX = clamp(point.x(), minX, maxX);
                closestY = clamp(point.y(), minY, maxY);
                closestZ = clamp(point.z(), minZ, maxZ);
        }

        // Calculate Euclidean distance
        double dx = point.x() - closestX;
        double dy = point.y() - closestY;
        double dz = point.z() - closestZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // TODO: Same for this one
    /**
     * Get the neighbor block that was clicked to place a block.
     */
    private Point getNeighborBlock(Point placedPos, BlockFace clickedFace) {
        int x = placedPos.blockX();
        int y = placedPos.blockY();
        int z = placedPos.blockZ();

        // Calculate neighbor position based on face
        return switch (clickedFace) {
            case TOP    -> new Pos(x, y - 1, z); // Clicked top of block below
            case BOTTOM -> new Pos(x, y + 1, z); // Clicked bottom of block above
            case NORTH  -> new Pos(x, y, z + 1); // Clicked north face of block south
            case SOUTH  -> new Pos(x, y, z - 1); // Clicked south face of block north
            case WEST   -> new Pos(x + 1, y, z); // Clicked west face of block east
            case EAST   -> new Pos(x - 1, y, z); // Clicked east face of block west
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatPoint(Point p) {
        return String.format("(%d,%d,%d)", p.blockX(), p.blockY(), p.blockZ());
    }

    private String formatPos(Pos p) {
        return String.format("(%.3f,%.3f,%.3f)", p.x(), p.y(), p.z());
    }

    public static EyeHeightSystem getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EyeHeightSystem not initialized!");
        }
        return instance;
    }
}

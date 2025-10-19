package com.minestom.mechanics.features.blocking;

import com.minestom.mechanics.config.combat.CombatConfig;
import com.minestom.mechanics.util.LogUtil;
import com.minestom.mechanics.config.blocking.BlockingPreferences;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.*;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.client.play.ClientPlayerDiggingPacket;
import net.minestom.server.network.packet.client.play.ClientUseItemPacket;

/**
 * Handles input detection for blocking - processes packets and player events.
 * Focused responsibility: Input detection and blocking trigger logic only.
 */
public class BlockingInputHandler {
    private static final LogUtil.SystemLogger log = LogUtil.system("BlockingInputHandler");

    private final CombatConfig config;
    private final BlockingSystem blockingSystem;

    public BlockingInputHandler(CombatConfig config, BlockingSystem blockingSystem) {
        this.config = config;
        this.blockingSystem = blockingSystem;
    }

    /**
     * Register all input event listeners
     */
    public void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();

        // Initialize preferences on spawn
        handler.addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn);

        // Handle blocking start/stop via packets
        handler.addListener(PlayerPacketEvent.class, this::handlePlayerPacket);

        // TODO: This seems to be a common listener
        // Stop blocking on slot change
        handler.addListener(PlayerChangeHeldSlotEvent.class, this::handleSlotChange);

        // Stop blocking on death
        handler.addListener(PlayerDeathEvent.class, this::handlePlayerDeath);

        // Cleanup on disconnect
        handler.addListener(PlayerDisconnectEvent.class, this::handlePlayerDisconnect);
    }

    private void handlePlayerSpawn(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        if (player.getTag(BlockingStateManager.PREFERENCES) == null) {
            BlockingPreferences prefs = new BlockingPreferences();
            prefs.showShieldOnSelf = true;
            prefs.showShieldOnOthers = true;
            prefs.showParticlesOnSelf = true;
            prefs.showParticlesOnOthers = false;
            prefs.showActionBarOnBlock = true;
            prefs.particleType = BlockingPreferences.ParticleType.CRIT;
            prefs.particleCount = 8; // TODO: remove hard coded value here!!!
            player.setTag(BlockingStateManager.PREFERENCES, prefs);
        }
    }

    private void handlePlayerPacket(PlayerPacketEvent event) {
        if (!config.blockingEnabled()) return;

        Player player = event.getPlayer();

        if (event.getPacket() instanceof ClientUseItemPacket) {
            ItemStack mainHand = player.getItemInMainHand();
            if (isSword(mainHand) && !blockingSystem.isBlocking(player)) {
                blockingSystem.startBlocking(player);
            }
        } else if (event.getPacket() instanceof ClientPlayerDiggingPacket digging) {
            if (digging.status() == ClientPlayerDiggingPacket.Status.UPDATE_ITEM_STATE && 
                blockingSystem.isBlocking(player)) {
                blockingSystem.stopBlocking(player);
            }
        }
    }

    // TODO: This is repeated logic for other things, consider refactoring to avoid duplicating code
    //  (I know it was used for rods at one point, tbh this is small and probably fine, but idk)
    private void handleSlotChange(PlayerChangeHeldSlotEvent event) {
        if (blockingSystem.isBlocking(event.getPlayer())) {
            blockingSystem.stopBlocking(event.getPlayer());
        }
    }

    // TODO: Probably a good idea to add a couple cleanup things to the aformentioned player package
    //  to avoid duplicating handleplayerdeath, handledisconnect, etc. in multiple features / systems

    private void handlePlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (blockingSystem.isBlocking(player)) {
            blockingSystem.stopBlocking(player);
            log.debug("Stopped blocking for {} on death", player.getUsername());
        }
    }

    private void handlePlayerDisconnect(PlayerDisconnectEvent event) {
        blockingSystem.cleanup(event.getPlayer());
    }

    // TODO: Find a better way to tell if an item is a sword. This is pathetic lmao
    /**
     * Check if an item is a sword that can be used for blocking
     */
    private boolean isSword(ItemStack stack) {
        if (stack == null || stack.isAir()) return false;
        Material mat = stack.material();
        return mat == Material.WOODEN_SWORD ||
                mat == Material.STONE_SWORD ||
                mat == Material.IRON_SWORD ||
                mat == Material.GOLDEN_SWORD ||
                mat == Material.DIAMOND_SWORD ||
                mat == Material.NETHERITE_SWORD;
    }
}

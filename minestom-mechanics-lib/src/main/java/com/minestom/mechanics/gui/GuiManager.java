package com.minestom.mechanics.gui;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized manager for all GUIs, similar to InvUI's WindowManager.
 * Registers event listeners once and routes events to the appropriate GUI.
 */
public class GuiManager {

    private static GuiManager instance;

    private final Map<Inventory, GuiBuilder> guisByInventory = new HashMap<>();
    private final Map<Player, GuiBuilder> guisByPlayer = new HashMap<>();

    private GuiManager() {
        // Register global event listeners ONCE
        MinecraftServer.getGlobalEventHandler().addListener(InventoryPreClickEvent.class, event -> {
            GuiBuilder gui = guisByPlayer.get(event.getPlayer());

            if (gui != null && event.getInventory() == gui.getInventory()) {
                // Cancel the click by default to prevent item manipulation
                event.setCancelled(true);

                // Handle the click
                gui.handleClick(event);
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(InventoryCloseEvent.class, event -> {
            GuiBuilder gui = guisByPlayer.get(event.getPlayer());

            if (gui != null && event.getInventory() == gui.getInventory()) {
                // Remove from tracking
                removeGui(gui);

                // Call close handler
                gui.handleClose(event.getPlayer());
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> {
            Player player = event.getPlayer();
            GuiBuilder gui = guisByPlayer.remove(player);

            if (gui != null) {
                guisByInventory.remove(gui.getInventory());
            }
        });
    }

    /**
     * Get the singleton instance
     */
    public static GuiManager getInstance() {
        if (instance == null) {
            instance = new GuiManager();
        }
        return instance;
    }

    /**
     * Register a GUI for tracking
     */
    public void addGui(GuiBuilder gui) {
        guisByInventory.put(gui.getInventory(), gui);
        guisByPlayer.put(gui.getPlayer(), gui);
    }

    /**
     * Remove a GUI from tracking
     */
    public void removeGui(GuiBuilder gui) {
        guisByInventory.remove(gui.getInventory());
        guisByPlayer.remove(gui.getPlayer());
    }

    /**
     * Get the GUI a player has open
     */
    public GuiBuilder getOpenGui(Player player) {
        return guisByPlayer.get(player);
    }

    public int getOpenGuisCount() {
        return guisByPlayer.size();
    }

    /**
     * Cleanup player's GUI data
     */
    public void cleanup(Player player) {
        GuiBuilder gui = guisByPlayer.remove(player);
        if (gui != null) {
            guisByInventory.remove(gui.getInventory());
        }
    }

    /**
     * Shutdown the GUI manager and close all open GUIs.
     * Should be called when the server is shutting down.
     */
    public void shutdown() {
        // Close all open GUIs by removing them from tracking
        // The inventory close events will be handled by the event listeners
        for (GuiBuilder gui : guisByPlayer.values()) {
            try {
                // Remove from tracking - this will trigger cleanup
                removeGui(gui);
            } catch (Exception e) {
                // Ignore errors during shutdown
            }
        }
        
        // Clear all tracking maps (should already be empty from removeGui calls)
        guisByPlayer.clear();
        guisByInventory.clear();
    }
}

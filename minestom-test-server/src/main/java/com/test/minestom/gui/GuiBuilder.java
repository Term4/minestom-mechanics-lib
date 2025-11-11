package com.test.minestom.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.component.DataComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.test.minestom.misc.MessageBuilder.*;

/**
 * Custom GUI system built on Minestom's native inventory system.
 * Uses a centralized GuiManager (similar to InvUI's WindowManager) for event handling.
 *
 * Usage:
 * <pre>
 * GuiBuilder.create(player, "My GUI", 3) // 3 rows
 *     .setBorder(Material.GRAY_STAINED_GLASS_PANE)
 *     .setItem(13, toggleItem)
 *     .onClose(p -> p.sendMessage("Closed!"))
 *     .open();
 * </pre>
 */
public class GuiBuilder {

    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, Consumer<GuiClickContext>> clickHandlers = new HashMap<>();
    private Consumer<Player> closeHandler = null;

    private GuiBuilder(Player player, Component title, int rows) {
        this.player = player;

        // Determine inventory type based on rows
        InventoryType type = switch (rows) {
            case 1 -> InventoryType.CHEST_1_ROW;
            case 2 -> InventoryType.CHEST_2_ROW;
            case 3 -> InventoryType.CHEST_3_ROW;
            case 4 -> InventoryType.CHEST_4_ROW;
            case 5 -> InventoryType.CHEST_5_ROW;
            case 6 -> InventoryType.CHEST_6_ROW;
            default -> throw new IllegalArgumentException("Rows must be between 1 and 6");
        };

        this.inventory = new Inventory(type, title);
    }

    /**
     * Create a new GUI builder
     *
     * @param player Player to show GUI to
     * @param title GUI title
     * @param rows Number of rows (1-6), each row is 9 slots
     */
    public static GuiBuilder create(Player player, String title, int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Rows must be between 1 and 6");
        }
        return new GuiBuilder(player, Component.text(title, PRIMARY, TextDecoration.BOLD), rows);
    }

    /**
     * Create a new GUI builder with Component title
     */
    public static GuiBuilder create(Player player, Component title, int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Rows must be between 1 and 6");
        }
        return new GuiBuilder(player, title, rows);
    }

    /**
     * Set an item at a specific slot
     */
    public GuiBuilder setItem(int slot, ItemStack item) {
        inventory.setItemStack(slot, item);
        return this;
    }

    /**
     * Set an item with a click handler
     */
    public GuiBuilder setItem(int slot, ItemStack item, Consumer<GuiClickContext> onClick) {
        inventory.setItemStack(slot, item);
        clickHandlers.put(slot, onClick);
        return this;
    }

    /**
     * Fill all empty slots with a border item
     */
    public GuiBuilder setBorder(Material borderMaterial) {
        ItemStack border = ItemStack.builder(borderMaterial)
                .set(DataComponents.CUSTOM_NAME, Component.empty())
                .build();

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItemStack(i).isAir()) {
                inventory.setItemStack(i, border);
            }
        }
        return this;
    }

    /**
     * Fill specific slots with border
     */
    public GuiBuilder fillBorder(Material borderMaterial, int... slots) {
        ItemStack border = ItemStack.builder(borderMaterial)
                .set(DataComponents.CUSTOM_NAME, Component.empty())
                .build();

        for (int slot : slots) {
            inventory.setItemStack(slot, border);
        }
        return this;
    }

    /**
     * Set close handler
     */
    public GuiBuilder onClose(Consumer<Player> handler) {
        this.closeHandler = handler;
        return this;
    }

    /**
     * Open the GUI for the player
     */
    public void open() {
        // Register with GuiManager
        GuiManager.getInstance().addGui(this);

        // Open the inventory
        player.openInventory(inventory);
    }

    /**
     * Refresh the GUI (useful after changes)
     */
    public void refresh() {
        if (player.getOpenInventory() == inventory) {
            player.closeInventory();
            open();
        }
    }

    /**
     * Close the GUI
     */
    public void close() {
        player.closeInventory();
        // GuiManager will handle cleanup via InventoryCloseEvent
    }

    /**
     * Internal: Handle click event (called by GuiManager)
     */
    void handleClick(InventoryPreClickEvent event) {
        int slot = event.getSlot();

        Consumer<GuiClickContext> handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.accept(new GuiClickContext(
                    event.getPlayer(),
                    slot,
                    event.getClick(),
                    this
            ));
        }
    }

    /**
     * Internal: Handle close (called by GuiManager)
     */
    void handleClose(Player player) {
        if (closeHandler != null) {
            closeHandler.accept(player);
        }
    }

    /**
     * Get the underlying inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Get the player viewing this GUI
     */
    public Player getPlayer() {
        return player;
    }

    // ===========================
    // ITEM BUILDERS
    // ===========================

    /**
     * Create a toggle item
     */
    public static ItemStack toggleItem(
            Material enabledMaterial,
            String name,
            String description,
            boolean enabled) {

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(description, LABEL));
        lore.add(Component.empty());
        lore.add(Component.text(enabled ? "✓ Enabled" : "✗ Disabled",
                enabled ? SUCCESS : ERROR));
        lore.add(Component.text("Click to toggle", VALUE));

        return ItemStack.builder(enabled ? enabledMaterial : Material.BARRIER)
                .set(DataComponents.CUSTOM_NAME,
                        Component.text(name, enabled ? SUCCESS : ERROR))
                .set(DataComponents.LORE, lore)
                .build();
    }

    /**
     * Create an info item (non-clickable display)
     */
    public static ItemStack infoItem(Material material, String name, String... loreLines) {
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            if (line.isEmpty()) {
                lore.add(Component.empty());
            } else {
                lore.add(Component.text(line, LABEL));
            }
        }

        return ItemStack.builder(material)
                .set(DataComponents.CUSTOM_NAME, Component.text(name, VALUE))
                .set(DataComponents.LORE, lore)
                .build();
    }

    /**
     * Create an adjustable number item
     */
    public static ItemStack adjustableItem(
            Material material,
            String name,
            int currentValue,
            String description) {

        List<Component> lore = new ArrayList<>();
        if (!description.isEmpty()) {
            lore.add(Component.text(description, LABEL));
            lore.add(Component.empty());
        }
        lore.add(Component.text("Current: ", LABEL)
                .append(Component.text(String.valueOf(currentValue), VALUE)));
        lore.add(Component.empty());
        lore.add(Component.text("Right-click: +1", SUCCESS));
        lore.add(Component.text("Left-click: -1", ERROR));
        lore.add(Component.text("Shift + Right: +10", SUCCESS));
        lore.add(Component.text("Shift + Left: -10", ERROR));

        return ItemStack.builder(material)
                .set(DataComponents.CUSTOM_NAME, Component.text(name, ACCENT))
                .set(DataComponents.LORE, lore)
                .amount(Math.max(1, Math.min(64, currentValue)))
                .build();
    }

    /**
     * Create a cycle item (for enum selections)
     */
    public static ItemStack cycleItem(
            Material material,
            String name,
            String currentOption,
            String description) {

        List<Component> lore = new ArrayList<>();
        if (!description.isEmpty()) {
            lore.add(Component.text(description, LABEL));
            lore.add(Component.empty());
        }
        lore.add(Component.text("Current: ", LABEL)
                .append(Component.text(currentOption, VALUE)));
        lore.add(Component.empty());
        lore.add(Component.text("Right-click: Next", SUCCESS));
        lore.add(Component.text("Left-click: Previous", ERROR));

        return ItemStack.builder(material)
                .set(DataComponents.CUSTOM_NAME, Component.text(name, ACCENT))
                .set(DataComponents.LORE, lore)
                .build();
    }

    /**
     * Create an action button
     */
    public static ItemStack actionItem(Material material, String name, String description) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(description, LABEL));
        lore.add(Component.empty());
        lore.add(Component.text("Click to activate", VALUE));

        return ItemStack.builder(material)
                .set(DataComponents.CUSTOM_NAME, Component.text(name, PRIMARY, TextDecoration.BOLD))
                .set(DataComponents.LORE, lore)
                .build();
    }

    /**
     * Create a close/back button
     */
    public static ItemStack closeButton() {
        return actionItem(Material.BARRIER, "Close", "Close this menu");
    }

    /**
     * Create a border pane (decoration)
     */
    public static ItemStack borderPane() {
        return ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE)
                .set(DataComponents.CUSTOM_NAME, Component.empty())
                .build();
    }

    /**
     * Context provided to click handlers.
     */
    public static class GuiClickContext {
        private final Player player;
        private final int slot;
        private final Click click;
        private final GuiBuilder gui;

        GuiClickContext(Player player, int slot, Click click, GuiBuilder gui) {
            this.player = player;
            this.slot = slot;
            this.click = click;
            this.gui = gui;
        }

        public Player getPlayer() { return player; }
        public int getSlot() { return slot; }
        public Click getClick() { return click; }
        public GuiBuilder getGui() { return gui; }

        public boolean isLeftClick() {
            return click instanceof Click.Left;
        }

        public boolean isRightClick() {
            return click instanceof Click.Right;
        }

        public boolean isShiftLeftClick() {
            return click instanceof Click.LeftShift;
        }

        public boolean isShiftRightClick() {
            return click instanceof Click.RightShift;
        }

        public boolean isMiddleClick() {
            return click instanceof Click.Middle;
        }

        /**
         * Check if any shift click (left or right)
         */
        public boolean isShiftClick() {
            return isShiftLeftClick() || isShiftRightClick();
        }
    }
}

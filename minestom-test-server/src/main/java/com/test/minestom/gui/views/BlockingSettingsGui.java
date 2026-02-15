package com.test.minestom.gui.views;

import com.minestom.mechanics.systems.blocking.BlockingSystem;
import com.test.minestom.gui.GuiBuilder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;

import static com.test.minestom.misc.MessageBuilder.error;

/**
 * Minimal blocking info GUI. Visual effects/preferences removed; use BlockingDamageHook for custom displays.
 */
public class BlockingSettingsGui {

    private static BlockingSystem blockingSystem;

    public static void setBlockingSystem(BlockingSystem system) {
        blockingSystem = system;
    }

    public static void open(Player player) {
        try {
            double damageReduction = blockingSystem != null ? blockingSystem.getDamageReduction(null) : 0.5;
            double hKnockbackReduction = blockingSystem != null ? blockingSystem.getKnockbackHorizontalReduction(null) : 0;
            double vKnockbackReduction = blockingSystem != null ? blockingSystem.getKnockbackVerticalReduction(null) : 0;

            GuiBuilder.create(player, "Blocking Info", 3)
                .setBorder(Material.GRAY_STAINED_GLASS_PANE)
                .setItem(4, GuiBuilder.infoItem(
                        Material.BOOK,
                        "Blocking",
                        "Right-click with blockable item to block",
                        "",
                        String.format("• %.0f%% damage reduction (default)", damageReduction * 100),
                        String.format("• %.0f%% horizontal KB reduction (default)", hKnockbackReduction * 100),
                        String.format("• %.0f%% vertical KB reduction (default)", vKnockbackReduction * 100)
                ))
                .setItem(22, GuiBuilder.closeButton(), ctx -> ctx.getGui().close())
                .open();
        } catch (Exception e) {
            player.sendMessage(error("Failed to open blocking info: " + e.getMessage()));
            MinecraftServer.LOGGER.error("[BlockingSettingsGui] Error opening GUI for " + player.getUsername(), e);
        }
    }
}

package com.test.minestom.gui.views;

import com.minestom.mechanics.systems.blocking.BlockingSystem;
import com.test.minestom.gui.GuiBuilder;
import com.minestom.mechanics.systems.blocking.BlockingState;
import com.minestom.mechanics.config.blocking.BlockingPreferences;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;

import static com.minestom.mechanics.systems.util.MessageBuilder.error;

// TODO: To be honest, we should just move the gui and guibuilder to the server directly.
//  Having a GUI builder and set guis in a mechanics lib doesn't make sense.

/**
 * Blocking settings GUI using custom GuiBuilder (no external dependencies).
 * Migrated from InvUI to native Minestom implementation.
 */
public class BlockingSettingsGui {

    private static BlockingSystem blockingSystem;

    public static void setBlockingSystem(BlockingSystem system) {
        blockingSystem = system;
    }

    public static void open(Player player) {
        try {
            BlockingPreferences prefs = player.getTag(BlockingState.PREFERENCES);
            if (prefs == null) {
                prefs = new BlockingPreferences();
                player.setTag(BlockingState.PREFERENCES, prefs);
            }

            // Store prefs reference for click handlers
            final BlockingPreferences finalPrefs = prefs;

            GuiBuilder.create(player, "Blocking Settings", 5)
                .setBorder(Material.GRAY_STAINED_GLASS_PANE)

                // Row 2: Visual settings for others
                .setItem(10, createToggleItem(
                        Material.SHIELD,
                        "Show Shield on Others",
                        "See shields when other players block",
                        finalPrefs.showShieldOnOthers
                ), ctx -> {
                    finalPrefs.showShieldOnOthers = !finalPrefs.showShieldOnOthers;
                    playClickSound(ctx.getPlayer());
                    open(ctx.getPlayer());
                })

                .setItem(12, createToggleItem(
                        Material.BLAZE_POWDER,
                        "Show Particles on Others",
                        "See particles when other players block",
                        finalPrefs.showParticlesOnOthers
                ), ctx -> {
                    finalPrefs.showParticlesOnOthers = !finalPrefs.showParticlesOnOthers;
                    playClickSound(ctx.getPlayer());
                    open(ctx.getPlayer());
                })

                .setItem(14, createParticleTypeItem(finalPrefs), ctx -> {
                    BlockingPreferences.ParticleType[] types = BlockingPreferences.ParticleType.values();
                    int current = finalPrefs.particleType.ordinal();

                    if (ctx.isRightClick() || ctx.isShiftRightClick()) {
                        finalPrefs.particleType = types[(current + 1) % types.length];
                        playClickSound(ctx.getPlayer(), 1.2f);
                    } else if (ctx.isLeftClick() || ctx.isShiftLeftClick()) {
                        finalPrefs.particleType = types[(current - 1 + types.length) % types.length];
                        playClickSound(ctx.getPlayer(), 0.8f);
                    }

                    open(ctx.getPlayer());
                })

                .setItem(16, createParticleCountItem(finalPrefs), ctx -> {
                    if (ctx.isShiftRightClick() || ctx.isShiftLeftClick()) {
                        // Reset to default
                        finalPrefs.particleCount = 8;
                        playClickSound(ctx.getPlayer(), 1.0f);
                    } else if (ctx.isRightClick()) {
                        finalPrefs.particleCount = Math.min(20, finalPrefs.particleCount + 1);
                        playClickSound(ctx.getPlayer(), 1.2f);
                    } else if (ctx.isLeftClick()) {
                        finalPrefs.particleCount = Math.max(1, finalPrefs.particleCount - 1);
                        playClickSound(ctx.getPlayer(), 0.8f);
                    }

                    open(ctx.getPlayer());
                })

                // Row 3: Visual settings for self
                .setItem(19, createToggleItem(
                        Material.GOLDEN_CHESTPLATE,
                        "Show Shield on Self",
                        "See shield when you block",
                        finalPrefs.showShieldOnSelf
                ), ctx -> {
                    finalPrefs.showShieldOnSelf = !finalPrefs.showShieldOnSelf;
                    playClickSound(ctx.getPlayer());
                    open(ctx.getPlayer());
                })

                .setItem(21, createToggleItem(
                        Material.GLOWSTONE_DUST,
                        "Show Particles on Self",
                        "See particles when you block",
                        finalPrefs.showParticlesOnSelf
                ), ctx -> {
                    finalPrefs.showParticlesOnSelf = !finalPrefs.showParticlesOnSelf;
                    playClickSound(ctx.getPlayer());
                    open(ctx.getPlayer());
                })

                // Row 4: Notifications
                .setItem(28, createToggleItem(
                        Material.PAPER,
                        "Action Bar Notification",
                        "Show 'Blocking' in action bar",
                        finalPrefs.showActionBarOnBlock
                ), ctx -> {
                    finalPrefs.showActionBarOnBlock = !finalPrefs.showActionBarOnBlock;
                    playClickSound(ctx.getPlayer());
                    open(ctx.getPlayer());
                })

                .setItem(30, createToggleItem(
                        Material.ANVIL,
                        "Anvil Sound on Successful Block",
                        "Play anvil sound when you block damage",
                        finalPrefs.playAnvilSoundOnSuccessfulBlock
                ), ctx -> {
                    finalPrefs.playAnvilSoundOnSuccessfulBlock = !finalPrefs.playAnvilSoundOnSuccessfulBlock;
                    playClickSound(ctx.getPlayer());
                    open(ctx.getPlayer());
                })

                // Info item
                .setItem(34, createInfoItem())

                // Close button
                .setItem(40, GuiBuilder.closeButton(), ctx -> {
                    ctx.getGui().close();
                })

                .open();

        } catch (Exception e) {
            player.sendMessage(error("Failed to open blocking settings: " + e.getMessage()));
            MinecraftServer.LOGGER.error("[BlockingSettingsGui] Error opening GUI for " +
                    player.getUsername(), e);
        }
    }

    private static net.minestom.server.item.ItemStack createToggleItem(
            Material material, String name, String description, boolean enabled) {
        return GuiBuilder.toggleItem(material, name, description, enabled);
    }

    private static net.minestom.server.item.ItemStack createParticleTypeItem(BlockingPreferences prefs) {
        return GuiBuilder.cycleItem(
                prefs.particleType.icon,
                "Particle Type",
                prefs.particleType.displayName,
                "Cycle through particle types"
        );
    }

    private static net.minestom.server.item.ItemStack createParticleCountItem(BlockingPreferences prefs) {
        return GuiBuilder.adjustableItem(
                Material.REPEATER,
                "Particle Count",
                prefs.particleCount,
                "Adjust number of particles shown"
        );
    }

    private static net.minestom.server.item.ItemStack createInfoItem() {
        double damageReduction = blockingSystem != null ? blockingSystem.getDamageReduction() : 0.5;
        double hKnockbackReduction = blockingSystem != null ? blockingSystem.getKnockbackHorizontalReduction() : 0.95;
        double vKnockbackReduction = blockingSystem != null ? blockingSystem.getKnockbackVerticalReduction() : 0.95;

        return GuiBuilder.infoItem(
                Material.BOOK,
                "Info",
                "Blocking System Settings",
                "",
                "• Right-click with sword to block",
                String.format("• %.0f%% damage reduction", damageReduction * 100),
                String.format("• %.0f%% horizontal KB reduction", hKnockbackReduction * 100),
                String.format("• %.0f%% vertical KB reduction", vKnockbackReduction * 100),
                "",
                "Your preferences are saved automatically"
        );
    }

    private static void playClickSound(Player player) {
        playClickSound(player, 1.0f);
    }

    private static void playClickSound(Player player, float pitch) {
        player.playSound(Sound.sound(
                Key.key("minecraft:block.note_block.pling"),
                Sound.Source.MASTER,
                0.3f, pitch
        ));
    }
}

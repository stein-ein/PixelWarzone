package com.github.steinein.pixelwarzone.ui;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.utils.Utils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class Leaderboard {

    public static void openMenu(ServerPlayer player, PixelWarzone plugin) {
        Thread.ofVirtual().start(() -> {
            List<WarzonePlayer> topPlayers = plugin.getDatabase().getTopPlayers();
            // Must open the menu on the server thread
            player.getServer().execute(() -> {
                SimpleContainer container = new SimpleContainer(54);
                fillBorders(container);
                placePlayerHeads(container, topPlayers, plugin);

                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x6, id, inv, container, 6),
                        Message.parseColors("    &8&lWarzone Leaderboard")
                ));
            });
        });
    }

    private static void fillBorders(SimpleContainer container) {
        ItemStack border = new ItemStack(Items.RED_STAINED_GLASS_PANE);
        border.set(DataComponents.CUSTOM_NAME, Component.empty());

        // Top and bottom rows
        for (int i = 0; i < 9; i++) container.setItem(i, border.copy());
        for (int i = 45; i < 54; i++) container.setItem(i, border.copy());
        // Side borders
        container.setItem(9, border.copy());
        container.setItem(17, border.copy());
        container.setItem(18, border.copy());
        container.setItem(26, border.copy());
        container.setItem(27, border.copy());
        container.setItem(35, border.copy());
        container.setItem(36, border.copy());
        container.setItem(44, border.copy());
    }

    private static final int[] SLOTS = {13, 20, 24, 37, 38, 39, 40, 41, 42, 43};

    private static void placePlayerHeads(SimpleContainer container, List<WarzonePlayer> players, PixelWarzone plugin) {
        for (int i = 0; i < Math.min(players.size(), SLOTS.length); i++) {
            WarzonePlayer wp = players.get(i);
            int wins = wp.getWins();
            int losses = wp.getLosses();

            String name = "Unknown";
            WarzonePlayer online = null;
            if (wp.getPlayer() != null) {
                name = wp.getPlayer().getName().getString();
                online = plugin.playerDataMap.get(wp.getPlayer().getUUID());
                if (online != null) {
                    wins = online.getWins();
                    losses = online.getLosses();
                }
            }

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.CUSTOM_NAME, Message.parseColors("&f#" + (i + 1) + ": &e" + name));

            List<Component> lore = List.of(
                    Message.parseColors("&aWins: &b" + wins),
                    Message.parseColors("&cLosses: &b" + losses),
                    Message.parseColors("&6Ratio: &b" + Utils.formatDouble(Utils.ratio(wins, losses)))
            );
            head.set(DataComponents.LORE, new ItemLore(lore));

            container.setItem(SLOTS[i], head);
        }
    }
}

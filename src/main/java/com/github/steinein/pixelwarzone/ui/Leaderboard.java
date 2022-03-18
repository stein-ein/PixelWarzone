package com.github.steinein.pixelwarzone.ui;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.mcsimonflash.sponge.teslalibs.inventory.Element;
import com.mcsimonflash.sponge.teslalibs.inventory.Layout;
import com.mcsimonflash.sponge.teslalibs.inventory.View;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Leaderboard {

    public static void openMenu(Player player) {
        Task.builder().execute(() -> {
            List<WarzonePlayer> playerList = PixelWarzone.getInstance().getDatabase().getTopPlayers();
            Optional<UserStorageService> userStorage = Sponge.getServiceManager().provide(UserStorageService.class);
            if (playerList != null && userStorage.isPresent()) {
                PluginContainer container = PixelWarzone.getInstance().getContainer();

                View view = View.builder()
                        .archetype(InventoryArchetypes.DOUBLE_CHEST)
                        .property(InventoryTitle.of(Utils.toText("    &8&lWarzone Leaderboard")))
                        .build(container)
                        .define(Layout.builder().build());

                view.open(player);

                for (int x = 0; x < 9; x++) {
                    view.setElement(x, Element.of(getPlaceholder()));
                }

                for (int x = 45; x < 54; x++) {
                    view.setElement(x, Element.of(getPlaceholder()));
                }

                view.setElement(9, Element.of(getPlaceholder()));
                view.setElement(17, Element.of(getPlaceholder()));
                view.setElement(18, Element.of(getPlaceholder()));
                view.setElement(26, Element.of(getPlaceholder()));
                view.setElement(27, Element.of(getPlaceholder()));
                view.setElement(35, Element.of(getPlaceholder()));
                view.setElement(36, Element.of(getPlaceholder()));
                view.setElement(44, Element.of(getPlaceholder()));

                showPlayers(playerList, userStorage.get(), view);
            }
        }).async().submit(PixelWarzone.getInstance());
    }

    private static void showPlayers(
            List<WarzonePlayer> playerList,
            UserStorageService userStorage,
            View view
    ) {
        List<WarzonePlayer> sortedPlayers = new ArrayList<>();
        playerList.forEach(wp -> {
            WarzonePlayer onlinePlayer = PixelWarzone.getInstance().playerDataMap.get(wp.getUser().getUniqueId());
            if (onlinePlayer != null) {
                sortedPlayers.add(onlinePlayer);
            } else {
                sortedPlayers.add(wp);
            }
        });
        sortedPlayers.sort(Comparator.comparingDouble(WarzonePlayer::getWins).reversed());
        for (int x = 0; x < sortedPlayers.size(); x++) {
            WarzonePlayer warzonePlayer = sortedPlayers.get(x);
            UUID uuid = warzonePlayer.getUser() != null ? warzonePlayer.getUser().getUniqueId() : warzonePlayer.getSpongePlayer().getUniqueId();
            User user = userStorage.get(uuid).orElse(null);
            if (user != null) {
                int pos = x + 1;
                switch (pos) {
                    case 1:
                        view.setElement(13, Element.of(getWarzonePlayer(user, warzonePlayer, pos)));
                        break;
                    case 2:
                        view.setElement(20, Element.of(getWarzonePlayer(user, warzonePlayer, pos)));
                        break;
                    case 3:
                        view.setElement(24, Element.of(getWarzonePlayer(user, warzonePlayer, pos)));
                        break;
                    default:
                        view.setElement(33 + pos, Element.of(getWarzonePlayer(user, warzonePlayer, pos)));
                        break;
                }
            }
        }
    }

    private static ItemStack getWarzonePlayer(User user, WarzonePlayer warzonePlayer, int pos) {
        ArrayList<Text> lore = new ArrayList<>();
        int wins = warzonePlayer.getWins();
        int losses = warzonePlayer.getLosses();
        GameProfile gameProfile = GameProfile.of(user.getUniqueId(), user.getName());
        String lastPlayed = "N/A";

        if (user.get(Keys.LAST_DATE_PLAYED).isPresent()) {
            Date date = Date.from(user.get(Keys.LAST_DATE_PLAYED).get());
            long duration = new Date().getTime() - date.getTime();
            lastPlayed = Utils.formatDuration(Duration.of(duration, ChronoUnit.MILLIS));
        }

        lore.add(Utils.toText("&aWins: &b" + wins));
        lore.add(Utils.toText("&cLosses: &b" + losses));
        lore.add(Utils.toText("&6Ratio: &b" + Utils.formatDouble(Utils.ratio(wins, losses))));
        if (user.isOnline()) {
            lore.add(Utils.toText("&7Currently &aOnline"));
        } else {
            lore.add(Utils.toText("&7Last online &a" + lastPlayed + " &7ago"));
        }
        ItemStack itemStack = ItemStack.builder()
                .itemType(ItemTypes.SKULL)
                .add(Keys.SKULL_TYPE, SkullTypes.PLAYER)
                .add(Keys.REPRESENTED_PLAYER, gameProfile)
                .add(Keys.DISPLAY_NAME, Utils.toText("&f#" + pos + ":" + " &e" + user.getName()))
                .add(Keys.ITEM_LORE, lore)
                .add(Keys.SKIN_UNIQUE_ID, user.getUniqueId())
                .build();
        return itemStack;
    }

    private static ItemStack getPlaceholder() {
        ItemStack itemStack = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        itemStack.offer(Keys.DISPLAY_NAME, Text.of(""));
        itemStack.offer(Keys.DYE_COLOR, DyeColors.RED);
        return itemStack;
    }

}

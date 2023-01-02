package com.github.steinein.pixelwarzone.utils;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.config.PixelmonItemsTools;
import com.pixelmonmod.pixelmon.config.PixelmonItemsPokeballs;
import net.minecraft.item.Item;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class Utils {
    private static final List<Item> duskItems = Arrays.asList(
            PixelmonItemsTools.duskstoneHelm,
            PixelmonItemsTools.duskstoneAxeItem,
            PixelmonItemsTools.duskstoneBoots,
            PixelmonItemsTools.duskstoneHammerItem,
            PixelmonItemsTools.duskstoneHoeItem,
            PixelmonItemsTools.duskstoneLegs,
            PixelmonItemsTools.duskstonePickaxeItem,
            PixelmonItemsTools.duskstonePlate,
            PixelmonItemsTools.duskstoneShovelItem,
            PixelmonItemsTools.duskstoneSwordItem
    );

    private static final List<Item> ballItems = Arrays.asList(
            PixelmonItemsPokeballs.masterBall,
            PixelmonItemsPokeballs.parkBall
    );

    public static Text toText(final String str) {
        if (str == null) {
            return (Text) Text.of("");
        }
        return TextSerializers.FORMATTING_CODE.deserialize(str);
    }

    public static double ratio(final double wins, final double losses) {
        if ((wins + losses) == 0) {
            return 0.0;
        }
        return wins / (wins + losses) * 100;
    }

    public static String formatDouble(final double ratio) {
        return String.format("%.2f", ratio) + "%";
    }

    public static boolean isHiddenAbility(Pokemon pokemon) {
        return pokemon.getBaseStats().getHiddenAbility()
                .map(ha -> ha.equals(pokemon.getAbility()))
                .orElse(false);
    }

    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds > 86400) {
            return (int) Math.floor(seconds / 86400) + " day(s)";
        } else if (seconds > 3600) {
            return (int) Math.floor(seconds / 3600) + " hour(s)";
        } else if (seconds > 60) {
            return (int) Math.floor(seconds / 60) + " minute(s)";
        } else {
            return (int) seconds + " seconds";
        }
    }

    public static boolean checkWarzonePlayer(
            final PixelWarzone plugin,
            final Player player,
            final WarzonePlayer warzonePlayer
    ) {
        if (warzonePlayer.getParty().getTeam().size() < 6) {
            player.sendMessage(
                    Utils.toText(plugin.getPluginConfig().getPrefix() + "&cYou must have 6 Pokemon to warp to warzone.")
            );
            return false;
        }

        if (warzonePlayer.getParty().countAblePokemon() < warzonePlayer.getParty().getTeam().size()) {
            player.sendMessage(
                    Utils.toText(plugin.getPluginConfig().getPrefix() + "&cYou can't enter warzone with fainted Pokemon!")
            );
            return false;
        }

        if (!player.hasPermission(WarzonePermission.WARZONE_FLIGHT_EXEMPT) && player.get(Keys.IS_FLYING).get()) {
            player.sendMessage(Utils.toText(plugin.getPluginConfig().getPrefix() + " &cFlying is not allowed."));
            return false;
        }

        int minLvl = plugin.getPluginConfig().getMinWarpLevel();
        int maxLvl = plugin.getPluginConfig().getMaxWarpLevel();
        boolean rangeSatisfied =
                warzonePlayer.getParty().getTeam().stream().allMatch(
                        p -> (p.getLevel() >= minLvl) && (p.getLevel() <= maxLvl)
                );

        if (!rangeSatisfied) {
            player.sendMessage(
                    Utils.toText(
                            plugin.getPluginConfig().getPrefix() +
                                    "&cAll of your pokemon must be in level range of " + minLvl + " - " + maxLvl + " to warp to warzone."
                    )

            );
            return false;
        }

        boolean allNotHA =
                warzonePlayer.getParty().getTeam().stream().allMatch(
                        p -> (!isHiddenAbility(p))
                );

        if (plugin.getPluginConfig().getDisableHA() && !allNotHA) {
            player.sendMessage(
                    Utils.toText(
                            plugin.getPluginConfig().getPrefix() + "&cYou cannot have any HA pokemon in your party."
                    )
            );
            return false;
        }

        boolean hasUntradeable = warzonePlayer.getParty().getTeam().stream().anyMatch(
                p -> (p.hasSpecFlag("untradeable"))
        );

        if (hasUntradeable) {
            player.sendMessage(
                    Utils.toText(
                            plugin.getPluginConfig().getPrefix() + "&cYou cannot enter the warzone with untradeable Pokemon."
                    )

            );
            return false;
        }

        AtomicBoolean hasDuskItem = new AtomicBoolean(false);

        duskItems.forEach(item -> {
            player.getInventory().contains((ItemType) item);
            if (player.getInventory().contains((ItemType) item)) {
                hasDuskItem.set(true);
            }
        });

        if (hasDuskItem.get()) {
            player.sendMessage(
                    Utils.toText(plugin.getPluginConfig().getPrefix() + "&cYou cannot have any dusk items in your inventory.")
            );
            return false;
        }

        AtomicBoolean hasBallItem = new AtomicBoolean(false);

        ballItems.forEach(item -> {
            player.getInventory().contains((ItemType) item);
            if (player.getInventory().contains((ItemType) item)) {
                hasBallItem.set(true);
            }
        });

        if (hasBallItem.get()) {
            player.sendMessage(
                    Utils.toText(plugin.getPluginConfig().getPrefix() + "&cYou cannot have master ball or park ball in your inventory.")
            );
            return false;
        }

        Optional<List<PotionEffect>> potionData = player.get(Keys.POTION_EFFECTS);
        if (potionData.isPresent()) {

            AtomicBoolean hasInvisEffect = new AtomicBoolean(false);

            potionData.get().forEach(effect -> {
                if (effect.getType() == PotionEffectTypes.INVISIBILITY) {
                    hasInvisEffect.set(true);
                }
            });

            if (hasInvisEffect.get()) {
                player.sendMessage(
                        Text.of(TextSerializers.FORMATTING_CODE.deserialize(
                                "&cYou cannot enter Warzone while invisible!")
                        )
                );
                return false;
            }
        }

        return true;
    }
}
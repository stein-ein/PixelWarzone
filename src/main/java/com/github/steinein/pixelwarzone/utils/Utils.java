package com.github.steinein.pixelwarzone.utils;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

import java.time.Duration;

public class Utils {

    public static double ratio(final double wins, final double losses) {
        if ((wins + losses) == 0) return 0.0;
        return wins / (wins + losses) * 100;
    }

    public static String formatDouble(final double ratio) {
        return String.format("%.2f", ratio) + "%";
    }

    public static boolean isHiddenAbility(Pokemon pokemon) {
        return pokemon.hasHiddenAbility();
    }

    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds > 86400) return (int) Math.floor(seconds / 86400.0) + " day(s)";
        else if (seconds > 3600) return (int) Math.floor(seconds / 3600.0) + " hour(s)";
        else if (seconds > 60) return (int) Math.floor(seconds / 60.0) + " minute(s)";
        else return seconds + " seconds";
    }

    public static boolean checkWarzonePlayer(
            final PixelWarzone plugin,
            final ServerPlayer player,
            final WarzonePlayer warzonePlayer
    ) {
        var party = warzonePlayer.getParty();
        if (party == null) return false;

        if (party.getTeam().size() < 6) {
            player.sendSystemMessage(Message.parseColors(
                    plugin.getPluginConfig().getPrefix() + "&cYou must have 6 Pokemon to warp to warzone."));
            return false;
        }

        if (party.countAblePokemon() < party.getTeam().size()) {
            player.sendSystemMessage(Message.parseColors(
                    plugin.getPluginConfig().getPrefix() + "&cYou can't enter warzone with fainted Pokemon!"));
            return false;
        }

        // Flying check (exempt if op level 3+)
        if (!player.hasPermissions(WarzonePermission.EXEMPT_LEVEL) && player.getAbilities().flying) {
            player.sendSystemMessage(Message.parseColors(
                    plugin.getPluginConfig().getPrefix() + "&cFlying is not allowed."));
            return false;
        }

        int minLvl = plugin.getPluginConfig().getMinWarpLevel();
        int maxLvl = plugin.getPluginConfig().getMaxWarpLevel();
        boolean rangeSatisfied = party.getTeam().stream()
                .allMatch(p -> p.getPokemonLevel() >= minLvl && p.getPokemonLevel() <= maxLvl);

        if (!rangeSatisfied) {
            player.sendSystemMessage(Message.parseColors(
                    plugin.getPluginConfig().getPrefix() +
                            "&cAll of your Pokemon must be in level range " + minLvl + "-" + maxLvl + "."));
            return false;
        }

        if (plugin.getPluginConfig().getDisableHA()) {
            boolean hasHA = party.getTeam().stream().anyMatch(Utils::isHiddenAbility);
            if (hasHA) {
                player.sendSystemMessage(Message.parseColors(
                        plugin.getPluginConfig().getPrefix() + "&cYou cannot have HA Pokemon in your party."));
                return false;
            }
        }

        boolean hasUntradeable = party.getTeam().stream().anyMatch(p -> p.hasFlag("untradeable"));
        if (hasUntradeable) {
            player.sendSystemMessage(Message.parseColors(
                    plugin.getPluginConfig().getPrefix() + "&cYou cannot enter warzone with untradeable Pokemon."));
            return false;
        }

        // Invisibility check
        if (player.hasEffect(MobEffects.INVISIBILITY)) {
            player.sendSystemMessage(Message.parseColors(
                    "&cYou cannot enter Warzone while invisible!"));
            return false;
        }

        return true;
    }
}

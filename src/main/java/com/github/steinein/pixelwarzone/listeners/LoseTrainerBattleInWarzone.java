package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.api.battles.BattleResults;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Map;

public class LoseTrainerBattleInWarzone {

    private final PixelWarzone plugin;

    public LoseTrainerBattleInWarzone(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void loseBattle(BattleEndEvent event) {
        plugin.log("BattleEndEvent: players=%d abnormal=%s cause=%s", event.getPlayers().size(), event.isAbnormal(), event.getCause());

        // COMMAND: handled by EndBattle.forfeit() (loser = who ran the command)
        // PLAYER_QUIT: handled by PlayerQuit listener (loser = who disconnected)
        // Skip both here to avoid double-applying consequences.
        if (event.getCause() == com.pixelmonmod.pixelmon.api.battles.BattleEndCause.COMMAND
                || event.getCause() == com.pixelmonmod.pixelmon.api.battles.BattleEndCause.PLAYER_QUIT) {
            plugin.debug("BattleEndEvent: cause={}, handled elsewhere — skipping.", event.getCause());
            return;
        }

        if (event.getPlayers().size() != 2) return;

        WarzonePlayer winner;
        WarzonePlayer loser;

        if (event.isAbnormal()) {
            // /endbattle or other forced end — determine winner/loser by remaining Pokemon count then HP%
            var bc = event.getBattleController();
            if (bc.participants.size() != 2) return;
            if (!(bc.participants.get(0) instanceof PlayerParticipant pp1)) return;
            if (!(bc.participants.get(1) instanceof PlayerParticipant pp2)) return;

            WarzonePlayer wp1 = new WarzonePlayer(plugin, pp1.player);
            WarzonePlayer wp2 = new WarzonePlayer(plugin, pp2.player);
            if (!plugin.getWarzoneList().stream().anyMatch(w -> w.hasPlayer(pp1.player))
                    || !plugin.getWarzoneList().stream().anyMatch(w -> w.hasPlayer(pp2.player))) {
                plugin.debug("BattleEndEvent: abnormal but players not both in warzone. Ignoring.");
                return;
            }

            int count1 = pp1.countAblePokemon();
            int count2 = pp2.countAblePokemon();
            plugin.log("BattleEndEvent: %s ablePokemon=%d, %s ablePokemon=%d",
                    pp1.player.getName().getString(), count1, pp2.player.getName().getString(), count2);

            if (count1 > count2) {
                winner = wp1; loser = wp2;
            } else if (count2 > count1) {
                winner = wp2; loser = wp1;
            } else {
                float hp1 = pp1.countHealthPercent();
                float hp2 = pp2.countHealthPercent();
                plugin.log("BattleEndEvent: tied count — hp1=%.2f hp2=%.2f", hp1, hp2);
                if (hp1 > hp2) {
                    winner = wp1; loser = wp2;
                } else if (hp2 > hp1) {
                    winner = wp2; loser = wp1;
                } else {
                    plugin.log("BattleEndEvent: true draw, no consequences.");
                    return;
                }
            }
        } else {
            // Normal battle end — use Pixelmon's own VICTORY/DEFEAT results
            winner = getPlayer(event.getResults(), BattleResults.VICTORY);
            loser = getPlayer(event.getResults(), BattleResults.DEFEAT);
            if (winner == null || loser == null) return;
            if (!plugin.getWarzoneList().stream().anyMatch(w -> w.hasPlayer(winner.getPlayer()))
                    || !plugin.getWarzoneList().stream().anyMatch(w -> w.hasPlayer(loser.getPlayer()))) {
                plugin.debug("Winner or loser not in warzone. Ignoring.");
                return;
            }
        }

        plugin.log("BattleEndEvent: winner=%s loser=%s",
                winner.getPlayer().getName().getString(), loser.getPlayer().getName().getString());

        Pokemon lostPoke = loser.removePokemon(1);
        if (lostPoke == null) {
            plugin.getLogger().error("Something went wrong during Pokemon removal.");
            return;
        }

        loser.sendMessage(Message.LOST_POKEMON, lostPoke.getDisplayName().getString());

        // Clear guiOpened on both players so they can battle again immediately
        var wp = com.pixelmonmod.pixelmon.api.storage.StorageProxy.getPartyNow(winner.getPlayer());
        if (wp != null) wp.guiOpened = false;
        var lp = com.pixelmonmod.pixelmon.api.storage.StorageProxy.getPartyNow(loser.getPlayer());
        if (lp != null) lp.guiOpened = false;

        boolean success = winner.givePokemon(lostPoke);
        if (success) {
            winner.sendMessage(Message.GAINED_POKEMON, lostPoke.getDisplayName().getString());

            WarzonePlayer dbWinner = plugin.playerDataMap.get(winner.getPlayer().getUUID());
            WarzonePlayer dbLoser = plugin.playerDataMap.get(loser.getPlayer().getUUID());
            if (dbWinner != null) { dbWinner.setWins(dbWinner.getWins() + 1); plugin.getDatabase().savePlayer(dbWinner); }
            if (dbLoser != null)  { dbLoser.setLosses(dbLoser.getLosses() + 1); plugin.getDatabase().savePlayer(dbLoser); }

            teleportToSpawn(loser.getPlayer());

            if (winner.getParty() != null) winner.getParty().heal();

            plugin.log("Gave %s the Pokemon %s that %s lost.",
                    winner.getPlayer().getName().getString(),
                    lostPoke.getDisplayName().getString(),
                    loser.getPlayer().getName().getString());
        }
    }

    private void teleportToSpawn(ServerPlayer player) {
        var config = plugin.getPluginConfig();
        if (config == null) return;
        var warzone = plugin.getWarzoneList().stream()
                .filter(w -> w.hasPlayer(player)).findFirst().orElse(null);
        String warzoneName = warzone != null ? warzone.getName() : null;
        if (warzoneName == null || !config.hasSpawn(warzoneName)) {
            plugin.getLogger().warn("No spawn set for warzone '{}' — cannot teleport loser. Use /warzone setspawn <name>.",
                    warzoneName != null ? warzoneName : "unknown");
            return;
        }
        double[] pos = config.getSpawn(warzoneName);
        ResourceLocation dim = ResourceLocation.parse(config.getSpawnWorld(warzoneName));
        ServerLevel level = player.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim));
        if (level == null) level = player.getServer().overworld();
        player.teleportTo(level, pos[0], pos[1], pos[2], player.getYRot(), player.getXRot());
    }

    private WarzonePlayer getPlayer(Map<BattleParticipant, BattleResults> results, BattleResults wanted) {
        if (!results.containsValue(BattleResults.VICTORY)) return null;
        for (Map.Entry<BattleParticipant, BattleResults> entry : results.entrySet()) {
            if (!(entry.getKey() instanceof PlayerParticipant pp)) continue;
            if (entry.getValue() == wanted) {
                return new WarzonePlayer(plugin, pp.player);
            }
        }
        return null;
    }
}

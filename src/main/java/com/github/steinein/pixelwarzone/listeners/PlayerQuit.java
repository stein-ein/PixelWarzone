package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerQuit {

    private final PixelWarzone plugin;

    public PlayerQuit(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        // Use direct bounds check — inWarzone() exempts ops but they should still forfeit
        boolean inWarzone = plugin.getWarzoneList().stream().anyMatch(w -> w.hasPlayer(sp));
        if (!inWarzone || !BattleRegistry.inBattle(sp)) return;

        WarzonePlayer warzonePlayer = new WarzonePlayer(plugin, sp);
        BattleController bc = BattleRegistry.getBattle(sp);
        WarzonePlayer opponent = warzonePlayer.getBattleOpponent();
        if (bc == null || opponent == null) return;

        // Only apply forfeit for PvP battles
        long pvpCount = bc.participants.stream()
                .filter(p -> p instanceof com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant)
                .count();
        if (pvpCount != 2 || pvpCount != bc.participants.size()) return;

        plugin.log("%s disconnected during a warzone PvP battle — applying forfeit.", sp.getName().getString());

        Pokemon pokemon = warzonePlayer.removePokemon(1);
        if (pokemon == null) return;

        warzonePlayer.sendMessage(Message.LOST_POKEMON, pokemon.getDisplayName().getString());
        boolean gave = opponent.givePokemon(pokemon);
        if (gave) {
            opponent.sendMessage(Message.GAINED_POKEMON, pokemon.getDisplayName().getString());
            plugin.log("Gave %s %s that %s lost on disconnect.",
                    opponent.getPlayer().getName().getString(),
                    pokemon.getDisplayName().getString(),
                    sp.getName().getString());
        }

        WarzonePlayer dbWinner = plugin.playerDataMap.get(opponent.getPlayer().getUUID());
        WarzonePlayer dbLoser  = plugin.playerDataMap.get(sp.getUUID());
        if (dbWinner != null) { dbWinner.setWins(dbWinner.getWins() + 1); plugin.getDatabase().savePlayer(dbWinner); }
        if (dbLoser  != null) { dbLoser.setLosses(dbLoser.getLosses() + 1); plugin.getDatabase().savePlayer(dbLoser); }

        if (opponent.getParty() != null) opponent.getParty().heal();
    }
}

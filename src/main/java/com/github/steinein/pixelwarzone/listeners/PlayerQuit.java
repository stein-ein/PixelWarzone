package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class PlayerQuit {

    private final PixelWarzone plugin;

    public PlayerQuit(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void PlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent e) {
        WarzonePlayer warzonePlayer = WarzonePlayer.fromForge(plugin, (EntityPlayerMP) e.player);
        if (warzonePlayer != null && warzonePlayer.inWarzone()) {
            if (BattleRegistry.getBattle(e.player) != null) {
                BattleControllerBase bcb = BattleRegistry.getBattle(e.player);
                PlayerParticipant winner = null;
                if (bcb.getPlayers() == null) {
                    return;
                }
                for (PlayerParticipant playerParticipant : bcb.getPlayers()) {
                    if (!playerParticipant.player.getUniqueID().equals(e.player.getUniqueID())) {
                        winner = playerParticipant;
                    }
                }
                Pokemon pokemon = warzonePlayer.removePokemon(1);
                if (winner != null) {
                    WarzonePlayer winnerPlayer = WarzonePlayer.fromForge(plugin, winner.player);
                    boolean success = winnerPlayer.givePokemon(pokemon);
                    if (success) {
                        winnerPlayer.sendMessage(Message.GAINED_POKEMON, pokemon.getDisplayName());
                    } else {
                        this.plugin.debug("Could not give pokemon to the winner. Check console output.");
                    }
                }
            }

        }
    }
}

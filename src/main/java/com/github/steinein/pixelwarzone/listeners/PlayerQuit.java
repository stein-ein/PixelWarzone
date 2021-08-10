package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
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
                WarzonePlayer opponent = warzonePlayer.getBattleOpponent();
                if (bcb.getPlayers() == null || opponent == null) {
                    return;
                }
                Pokemon pokemon = warzonePlayer.removePokemon(1);
                boolean success = opponent.givePokemon(pokemon);
                if (success) {
                    opponent.sendMessage(Message.GAINED_POKEMON, pokemon.getDisplayName());
                    this.plugin.getLogger().info(
                            "Gave " + opponent.getForgePlayer().getName() + " " + pokemon.getDisplayName() + " that "
                                    + warzonePlayer.getForgePlayer().getName() + " lost."
                    );
                } else {
                    this.plugin.getLogger().info("Could not give " + pokemon.getDisplayName() + " to the winner");
                }
            }

        }
    }
}

package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.api.events.LostToWildPixelmonEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.neoforged.bus.api.SubscribeEvent;

public class LoseNPCBattle {

    private final PixelWarzone plugin;

    public LoseNPCBattle(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void loseNPCBattle(LostToWildPixelmonEvent event) {
        WarzonePlayer player = new WarzonePlayer(plugin, event.player);
        plugin.debug("LostToWildPixelmonEvent for %s", player);

        if (!player.inWarzone()) {
            plugin.debug("%s not in warzone. Ignoring.", player);
            return;
        }

        Pokemon removed = player.removePokemon(1);
        if (removed == null) {
            plugin.getLogger().error("Could not remove Pokemon during NPC battle loss.");
            return;
        }

        player.sendMessage(Message.LOST_POKEMON, removed.getDisplayName().getString());
    }
}

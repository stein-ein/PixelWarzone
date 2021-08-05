package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.api.events.LostToTrainerEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LoseNPCBattle {

    private final PixelWarzone plugin;

    public LoseNPCBattle(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void loseTrainerBattle(LostToTrainerEvent event) {
        WarzonePlayer player = WarzonePlayer.fromForge(plugin, event.player);

        this.plugin.debug("Starting LostToTrainerEvent for player %s!", player.toString());

        // Ignore players that are not in a Warzone
        if (!player.inWarzone()) {
            this.plugin.debug(player + " was not found in any warzone. Aborting event.");
            return;
        }

        Pokemon removed = player.removePokemon(1);
        if (removed == null) {
            this.plugin.getLogger().error("Something went wrong during Pokemon removal. Check plugin debug.");
            return;
        }

        player.sendMessage(Message.LOST_POKEMON, removed.getDisplayName());

    }

}

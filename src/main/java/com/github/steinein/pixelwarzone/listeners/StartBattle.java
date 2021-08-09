package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.pixelmonmod.pixelmon.api.events.BattleStartedEvent;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class StartBattle {

    private final PixelWarzone plugin;

    public StartBattle(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void startBattle(BattleStartedEvent event) {
        BattleControllerBase bcb = event.bc;

        if (bcb == null || bcb.getPlayers() == null || bcb.getPlayers().size() != 2) {
            return;
        }

        boolean inWarzone = bcb.getPlayers().stream().filter(playerParticipant ->
                WarzonePlayer.fromForge(plugin, playerParticipant.player).inWarzone()
        ).count() > 0;

        if (!inWarzone) {
            return;
        }

        for (PlayerParticipant playerParticipant : bcb.getPlayers()) {
            if (playerParticipant.party.getTeam().size() <= 1) {
                bcb.getPlayers().forEach(playerParticipant1 -> playerParticipant1.player.sendMessage(
                        new TextComponentString(playerParticipant.player.getName() + " only has 1 Pokemon! Cannot start a battle.")
                ));
                event.setCanceled(true);
                return;
            }
        }
    }
}

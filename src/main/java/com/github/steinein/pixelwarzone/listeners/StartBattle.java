package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.pixelmonmod.pixelmon.api.events.battles.BattleStartedEvent;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;

public class StartBattle {

    private final PixelWarzone plugin;

    public StartBattle(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void startBattle(BattleStartedEvent.Pre event) {
        plugin.log("BattleStartedEvent.Pre fired: %d vs %d participants", event.getTeamOne().length, event.getTeamTwo().length);
        BattleParticipant[] team1 = event.getTeamOne();
        BattleParticipant[] team2 = event.getTeamTwo();

        // Only care about 1v1 player battles
        if (team1.length != 1 || team2.length != 1) return;
        if (!(team1[0] instanceof PlayerParticipant pp1)) return;
        if (!(team2[0] instanceof PlayerParticipant pp2)) return;

        // Direct bounds check — inWarzone() exempts ops, but battle rules apply regardless of op status
        boolean p1InWarzone = plugin.getWarzoneList().stream().anyMatch(w -> w.hasPlayer(pp1.player));
        boolean p2InWarzone = plugin.getWarzoneList().stream().anyMatch(w -> w.hasPlayer(pp2.player));

        // If neither is in a warzone, this isn't a warzone battle — ignore
        if (!p1InWarzone && !p2InWarzone) return;

        // If only one player is in the warzone, block the battle
        if (p1InWarzone != p2InWarzone) {
            String outsider = (!p2InWarzone ? pp2.player : pp1.player).getName().getString();
            pp1.player.sendSystemMessage(Component.literal(
                    outsider + " must be inside the warzone to battle here."));
            pp2.player.sendSystemMessage(Component.literal(
                    outsider + " must be inside the warzone to battle here."));
            event.setCanceled(true);
            return;
        }

        // Check both have more than 1 Pokemon
        for (PlayerParticipant pp : new PlayerParticipant[]{pp1, pp2}) {
            if (pp.party == null || pp.party.getTeam().size() <= 1) {
                pp1.player.sendSystemMessage(Component.literal(
                        pp.player.getName().getString() + " only has 1 Pokemon! Cannot start a battle."));
                pp2.player.sendSystemMessage(Component.literal(
                        pp.player.getName().getString() + " only has 1 Pokemon! Cannot start a battle."));
                event.setCanceled(true);
                return;
            }
        }
    }
}

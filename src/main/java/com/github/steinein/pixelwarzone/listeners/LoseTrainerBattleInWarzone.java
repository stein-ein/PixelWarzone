package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.google.common.collect.ImmutableMap;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.enums.battle.BattleResults;
import com.pixelmonmod.pixelmon.enums.battle.EnumBattleEndCause;
import com.pixelmonmod.pixelmon.storage.PlayerPartyStorage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.Map;

public class LoseTrainerBattleInWarzone {

    private final PixelWarzone plugin;

    public LoseTrainerBattleInWarzone(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void loseBattle(BattleEndEvent event) {

        final List<EntityPlayerMP> players = event.getPlayers();
        if (players == null || players.size() != 2) {
            return;
        }

        if (event.abnormal) { // We ignore crashes/forced/error
            this.plugin.debug("The battle had ended abnormally. Ignoring the rest.");
            return;
        }

        final EnumBattleEndCause cause = event.cause;
        if (cause == EnumBattleEndCause.FORCE) { // Ignore forced endings
            this.plugin.debug("Ending was forced. Ignoring the rest.");
        }

        final WarzonePlayer winner = this.get(event.results, Result.WINNER);
        final WarzonePlayer loser = this.get(event.results, Result.LOSER);

        if (winner == null) {
            this.plugin.getLogger().error("Battle winner could not be found. Check debug output for more info.");
            return;
        }

        if (loser == null) {
            this.plugin.getLogger().error("Battle loser could not be found. Check debug output for more info.");
            return;
        }

        if (!winner.inWarzone() || !loser.inWarzone()) {
            this.plugin.debug("Winner or loser not in warzone. Ignoring the rest.");
            return;
        }

        final Pokemon lostPoke = loser.removePokemon(1);
        if (lostPoke == null) {
            this.plugin.getLogger().error("Something went wrong during Pokemon removal. Check plugin debug.");
            return;
        }

        loser.sendMessage(Message.LOST_POKEMON, lostPoke.getDisplayName());

        boolean success = winner.givePokemon(lostPoke);

        if (success) {
            winner.sendMessage(Message.GAINED_POKEMON, lostPoke.getDisplayName());
            PlayerPartyStorage party = winner.getParty();
            if (party != null) {
                party.heal();
            }
            this.plugin.getLogger().info(
                    "Gave " + winner.getForgePlayer().getName() + " " + lostPoke.getDisplayName() + " that "
                            + loser.getForgePlayer().getName() + " lost."
            );
        } else {
            this.plugin.debug("Could not give pokemon to the winner. Check console output.");
        }


    }

    private WarzonePlayer get(final ImmutableMap<BattleParticipant, BattleResults> results, final Result wantedResult) {

        if (!results.containsValue(BattleResults.VICTORY)) {
            this.plugin.debug("This battle had no winners. Returning null!");
            return null;
        } // There is no winner, so we just return null

        for (Map.Entry<BattleParticipant, BattleResults> result : results.entrySet()) {

            this.plugin.debug("Checking result entry [Participant: %s, Result: %s]",
                    result.getKey().toString(),
                    result.getValue().toString());

            if (!(result.getKey() instanceof PlayerParticipant)) continue; // Ignore non-players

            PlayerParticipant playerParticipant = (PlayerParticipant) result.getKey();
            WarzonePlayer potentialResult = WarzonePlayer.fromForge(plugin, playerParticipant.player);

            if (result.getValue() == BattleResults.VICTORY && wantedResult == Result.WINNER) {
                this.plugin.debug("Found winner player %s!", potentialResult.toString());
                return potentialResult; // We were looking for a winner and found them
            }

            if (result.getValue() == BattleResults.DEFEAT && wantedResult == Result.LOSER) {
                this.plugin.debug("Found loser player %s!", potentialResult.toString());
                return potentialResult; // We were looking for a loser and found them
            }

        }

        this.plugin.debug("Could not find any results. Returning null.");
        return null;

    }

    private enum Result {
        WINNER,
        LOSER
    }

}

package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.pixelmonmod.pixelmon.api.events.PokeBallImpactEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.api.BattleBuilder;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Optional;

public class PokeballImpact {

    private final PixelWarzone plugin;

    public PokeballImpact(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onPokeballImpact(PokeBallImpactEvent event) {
        Optional<Entity> hitOpt = event.getEntityHit();
        if (hitOpt.isEmpty()) return;
        Entity hit = hitOpt.get();

        Entity thrower = event.getPokeBall().getOwner();
        if (!(thrower instanceof ServerPlayer challenger)) return;

        if (hit instanceof PixelmonEntity pixelmon) {
            // Pokeball hit a wild/owned Pixelmon — start PvP if it's owned by another player
            if (!(pixelmon.getOwner() instanceof ServerPlayer owner)) return;
            startBattle(challenger, owner);
        } else if (hit instanceof ServerPlayer challenged) {
            startBattle(challenger, challenged);
        }
    }

    private void startBattle(ServerPlayer challenger, ServerPlayer challenged) {
        if (challenger.getUUID().equals(challenged.getUUID())) return;
        if (BattleRegistry.inBattle(challenged) || BattleRegistry.inBattle(challenger)) return;

        WarzonePlayer wp1 = new WarzonePlayer(plugin, challenger);
        WarzonePlayer wp2 = new WarzonePlayer(plugin, challenged);

        if (!wp1.inWarzone() || !wp2.inWarzone()) return;
        if (!canBattle(challenger) || !canBattle(challenged)) return;

        if (!Utils.checkWarzonePlayer(plugin, challenger, wp1)) {
            challenger.getServer().getCommands().performPrefixedCommand(
                    challenger.createCommandSourceStack(), "warp warzone");
            return;
        }

        if (!Utils.checkWarzonePlayer(plugin, challenged, wp2)) {
            challenged.getServer().getCommands().performPrefixedCommand(
                    challenged.createCommandSourceStack(), "warp warzone");
            challenger.sendSystemMessage(Message.parseColors(
                    plugin.getPluginConfig().getPrefix() + "&cYour opponent does not meet the requirements!"));
            return;
        }

        PlayerParticipant p1 = new PlayerParticipant(challenger);
        PlayerParticipant p2 = new PlayerParticipant(challenged);

        BattleBuilder.builder()
                .teamOne(p1)
                .teamTwo(p2)
                .start(challenger.level().registryAccess());
    }

    private boolean canBattle(ServerPlayer player) {
        PlayerPartyStorage party = StorageProxy.getPartyNow(player);
        if (party == null || party.getTeam().isEmpty()) return false;
        return party.getTeam().stream().filter(Pokemon::canBattle).count() > 0;
    }
}

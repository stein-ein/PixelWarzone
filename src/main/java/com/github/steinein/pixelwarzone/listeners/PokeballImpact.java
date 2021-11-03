package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.PokeballImpactEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.battles.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.rules.clauses.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.enums.battle.EnumBattleType;
import com.pixelmonmod.pixelmon.storage.PlayerPartyStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.server.FMLServerHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.Optional;

public class PokeballImpact {

    private final PixelWarzone plugin;

    public PokeballImpact(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void pokeballImpactEvent(PokeballImpactEvent event) {
        if (event.getEntityHit() instanceof EntityPixelmon && event.pokeball.getThrower() instanceof EntityPlayerMP) {
            EntityPixelmon pixelmon = (EntityPixelmon) event.getEntityHit();
            if (pixelmon.getOwnerId() != null) {
                EntityPlayerMP entityPlayerMP = (EntityPlayerMP) event.pokeball.getThrower();
                Optional<Player> optionalPlayer = Sponge.getServer().getPlayer(pixelmon.getOwnerId());
                if (!optionalPlayer.isPresent()) {
                    return;
                }
                EntityPlayerMP challengedPlayer = FMLServerHandler.instance().getServer().getPlayerList().getPlayerByUUID(pixelmon.getOwnerId());
                startBattle(challengedPlayer, entityPlayerMP);
            }
        } else if (event.getEntityHit() instanceof EntityPlayerMP && event.pokeball.getThrower() instanceof EntityPlayerMP) {
            EntityPlayerMP challenger = (EntityPlayerMP) event.pokeball.getThrower();
            EntityPlayerMP challenged = (EntityPlayerMP) event.getEntityHit();
            if (challenger != null && challenged != null) {
                startBattle(challenged, challenger);
            }
        }
    }

    private void startBattle(EntityPlayerMP challengedPlayer, EntityPlayerMP entityPlayerMP) {
        if (BattleRegistry.getBattle(challengedPlayer) != null || challengedPlayer.getUniqueID().equals(entityPlayerMP.getUniqueID())) {
            return;
        }
        WarzonePlayer player1 = WarzonePlayer.fromForge(plugin, entityPlayerMP);
        WarzonePlayer player2 = WarzonePlayer.fromForge(plugin, challengedPlayer);
        if (!player1.inWarzone() || !player2.inWarzone()) {
            return;
        }
        if (!canBattle(challengedPlayer) || !canBattle(entityPlayerMP)) {
            return;
        }
        EntityPixelmon poke1 = Pixelmon.storageManager.getParty(entityPlayerMP.getUniqueID()).getTeam().get(0).getOrSpawnPixelmon((Entity) entityPlayerMP);
        PlayerParticipant playerParticipant1 = new PlayerParticipant(entityPlayerMP, poke1);
        EntityPixelmon poke2 = Pixelmon.storageManager.getParty(challengedPlayer.getUniqueID()).getTeam().get(0).getOrSpawnPixelmon((Entity) challengedPlayer);
        PlayerParticipant playerParticipant2 = new PlayerParticipant(challengedPlayer, poke2);

        BattleRules rules = new BattleRules(EnumBattleType.Single);
        rules.turnTime = 60;

        ArrayList<BattleClause> battleClauses = new ArrayList<>();
        battleClauses.add(new BattleClause("bag"));
        battleClauses.add(new BattleClause("ohko"));
        rules.setNewClauses(battleClauses);

        BattleRegistry.startBattle(new BattleParticipant[]{playerParticipant1}, new BattleParticipant[]{playerParticipant2}, rules);
    }

    private boolean canBattle(EntityPlayerMP player) {
        PlayerPartyStorage party = Pixelmon.storageManager.getParty(player.getUniqueID());
        if (party != null && party.getTeam() != null && party.getTeam().size() > 1) {
            return party.getTeam().stream().filter(Pokemon::canBattle).count() > 0;
        }
        return false;
    }
}

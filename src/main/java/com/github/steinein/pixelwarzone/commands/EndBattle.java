package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
import com.pixelmonmod.pixelmon.enums.battle.EnumBattleEndCause;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.*;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

public class EndBattle {

    private final CommandSpec spec;

    public EndBattle(final PixelWarzone plugin) {

        this.spec = CommandSpec.builder()
                .description(Text.of("Ends a pokemon battle in the warzone."))
                .arguments(
                        GenericArguments.optional(GenericArguments.string(Text.of("accept")))
                )
                .executor((src, args) -> {

                    if (src instanceof Player) {
                        Player player = (Player) src;
                        WarzonePlayer warzonePlayer = WarzonePlayer.fromSponge(plugin, (Player) src);
                        Optional<String> acceptOpt = args.getOne("accept");
                        if (warzonePlayer.inWarzone()) {

                            if (!warzonePlayer.inBattle()) {
                                warzonePlayer.sendMessage(Message.NOT_IN_BATTLE);
                                return CommandResult.success();
                            }

                            BattleControllerBase bcb = warzonePlayer.getBattle();

                            if (acceptOpt.isPresent() && acceptOpt.get().equalsIgnoreCase("accept")) {
                                if (plugin.requestMap.get(player.getUniqueId()) != null) {
                                    bcb.getPlayers().forEach(playerParticipant -> {
                                        if (playerParticipant.party != null) {
                                            playerParticipant.party.heal();
                                        }
                                        ((Player) playerParticipant.player).sendMessage(
                                                Utils.toText(plugin.getPluginConfig().getPrefix() + "Ending the battle!")
                                        );
                                    });
                                    bcb.endBattle(EnumBattleEndCause.FLEE);
                                    plugin.requestMap.remove(player.getUniqueId());
                                } else {
                                    warzonePlayer.sendMessage(Text.of("You have not received a request to end the battle!"));
                                }
                                return CommandResult.success();
                            }

                            if (bcb.getPlayers().size() > 1) {

                                WarzonePlayer opponent = warzonePlayer.getBattleOpponent();
                                src.sendMessage(Text.of("Sending " + opponent.getForgePlayer().getName() + " a request to end the battle."));
                                return this.handlePlayerBattle(warzonePlayer, opponent);

                            } else { // NPC or Pokemon battle

                                // This will still cause pokemon loss, will figure out that later
                                bcb.endBattle(EnumBattleEndCause.FORCE);
                            }


                        } else {
                            src.sendMessage(Text.of("You are not in the warzone!"));
                        }
                    }

                    return CommandResult.success();
                })
                .build();

    }

    private CommandResult handlePlayerBattle(final WarzonePlayer requester, final WarzonePlayer opponent) {

        Text endBattleRequest = TextSerializers.FORMATTING_CODE.deserialize(Message.END_BATTLE_PROPOSE.getRaw())
                .toBuilder()
                .build();

        opponent.sendMessage(endBattleRequest);

        PixelWarzone.getInstance().requestMap.put(opponent.getForgePlayer().getUniqueID(), requester.getForgePlayer().getUniqueID());

        return CommandResult.success();

    }

    public CommandSpec getSpec() {
        return this.spec;
    }

}

package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
import com.pixelmonmod.pixelmon.enums.battle.EnumBattleEndCause;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.*;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.List;
import java.util.Set;

public class EndBattle {

    private final CommandSpec spec;

    public EndBattle(final PixelWarzone plugin) {

        this.spec = CommandSpec.builder()
                .description(Text.of("Ends a pokemon battle in the warzone."))
                .executor((src, args) -> {

                    if (src instanceof Player) {
                        WarzonePlayer warzonePlayer = WarzonePlayer.fromSponge(plugin, (Player) src);
                        if (warzonePlayer.inWarzone()) {

                            if (!warzonePlayer.inBattle()) {
                                warzonePlayer.sendMessage(Message.NOT_IN_BATTLE);
                                return CommandResult.success();
                            }

                            BattleControllerBase bcb = warzonePlayer.getBattle();

                            if (bcb.getPlayers().size() > 1) {

                                WarzonePlayer opponent = warzonePlayer.getBattleOpponent();
                                return this.handlePlayerBattle(opponent, bcb);

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

    private CommandResult handlePlayerBattle(final WarzonePlayer opponent, final BattleControllerBase bcb) {

        Text endBattleRequest = Text.builder(Message.END_BATTLE_PROPOSE.getRaw())
                .color(TextColors.GOLD)
                .style(TextStyles.BOLD, TextStyles.UNDERLINE)
                .onClick(TextActions.executeCallback(clicker -> bcb.endBattle(EnumBattleEndCause.FORCE)))
                .build();

        opponent.sendMessage(endBattleRequest);

        return CommandResult.success();

    }

    public CommandSpec getSpec() {
        return this.spec;
    }

}

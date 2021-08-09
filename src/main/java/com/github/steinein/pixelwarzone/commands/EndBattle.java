package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.Set;

public class EndBattle {

    private final CommandSpec spec;

    public EndBattle(final PixelWarzone plugin) {

        this.spec = CommandSpec.builder()
                .description(Text.of("Ends a pokemon battle in the warzone."))
                .executor((src, args) -> {

                    if (src instanceof Player) {
                        WarzonePlayer warzonePlayer = WarzonePlayer.fromSponge(plugin, (Player) src);
                        if (warzonePlayer != null && warzonePlayer.inWarzone()) {
                            BattleControllerBase bcb = BattleRegistry.getBattle((EntityPlayer) src);
                            if (bcb == null) {
                                src.sendMessage(Text.of("You are not in a battle!"));
                                return CommandResult.success();
                            }
                            if (bcb.getPlayers() != null && bcb.getPlayers().size() > 1) {
                                src.sendMessage(Text.of("You cannot use this command in a player battle!"));
                            } else {
                                bcb.endBattle();
                            }
                        } else {
                            src.sendMessage(Text.of("You are not in the warzone!"));
                        }
                    }

                    return CommandResult.success();

                })
                .build();

    }

    public CommandSpec getSpec() {
        return this.spec;
    }

}

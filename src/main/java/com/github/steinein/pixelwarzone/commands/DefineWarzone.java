package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.selection.WarzoneSelection;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class DefineWarzone {

    private static final Text NAME_ARGUMENT = Text.of("name");

    private final CommandSpec spec;

    public DefineWarzone(final PixelWarzone plugin) {

        spec = CommandSpec.builder()
                .description(Text.of("Defines a new warzone"))
                .permission(WarzonePermission.DEFINE_WARZONE)
                .arguments(
                        GenericArguments.string(NAME_ARGUMENT)
                )
                .executor((src, args) -> {

                    if (src instanceof Player) {


                        Optional<String> name = args.getOne(NAME_ARGUMENT);
                        if (name.isPresent()) {

                            Optional<WarzoneSelection> optSel = plugin.getSelectionsManager().getSelection((Player) src);

                            if (optSel.isPresent()) {

                                WarzoneSelection selection = optSel.get();

                                if (selection.isCompleteSelection()) {

                                    plugin.getPluginConfig().createWarzone(name.get(), selection);

                                    src.sendMessage(Message.ZONE_DEFINE_SUCCESS.getMessage(name.get()));

                                    plugin.getSelectionsManager().removeSelection((Player) src);

                                    plugin.updateWarzoneList();

                                    return CommandResult.success();

                                } else { // Selection not complete

                                    src.sendMessage(Message.INVALID_SELECTION.getMessage());

                                    return CommandResult.empty();

                                }

                            } else { // Selection does not exist

                                src.sendMessage(Message.INVALID_SELECTION.getMessage());

                                return CommandResult.empty();

                            }

                        } else { // No defined name

                            src.sendMessage(Message.MISSING_ARGUMENT.getMessage(NAME_ARGUMENT.toPlain()));

                            return CommandResult.empty();

                        }


                    } else { // Sender not player

                        src.sendMessage(Message.NOT_A_PLAYER_ERROR.getMessage());

                        return CommandResult.empty();

                    }

                })
                .build();

    }

    public CommandSpec getSpec() {
        return this.spec;
    }

}

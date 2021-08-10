package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.messages.Message;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class DeleteWarzone {

    private final CommandSpec spec;

    private static final Text NAME_ARG = Text.of("name");

    public DeleteWarzone(final PixelWarzone plugin) {

        spec = CommandSpec.builder()
                .description(Text.of("Deletes the specified warzone"))
                .permission(WarzonePermission.DELETE_WARZONE)
                .arguments(
                        GenericArguments.string(NAME_ARG)
                )
                .executor((src, args) -> {

                    Optional<String> name = args.getOne(NAME_ARG);
                    if (name.isPresent()) {

                        if (!plugin.getPluginConfig().getWarzoneNames().contains(name.get())) {
                            src.sendMessage(Message.WARZONE_NOT_EXIST.getMessage(name.get()));
                            return CommandResult.empty();
                        }

                        plugin.getPluginConfig().deleteWarzone(name.get());
                        plugin.updateWarzoneList();

                        src.sendMessage(Message.DELET_WARZONE.getMessage(name.get()));

                        return CommandResult.success();

                    } else {

                        src.sendMessage(Message.MISSING_ARGUMENT.getMessage(NAME_ARG.toPlain()));
                        return CommandResult.empty();

                    }

                })
                .build();

    }

    public CommandSpec getSpec() {
        return this.spec;
    }
}

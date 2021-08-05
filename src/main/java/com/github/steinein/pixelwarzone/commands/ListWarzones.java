package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.messages.Message;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import java.util.Set;

public class ListWarzones {

    private final CommandSpec spec;

    public ListWarzones(final PixelWarzone plugin) {

        this.spec = CommandSpec.builder()
                .description(Text.of("Lists all currently defined warzones."))
                .permission(WarzonePermission.LIST_WARZONES)
                .executor((src, args) -> {

                    Set<String> warzoneNames = plugin.getPluginConfig().getWarzoneNames();

                    if (warzoneNames.isEmpty()) {

                        src.sendMessage(Message.NO_DEFINED_WARZONES.getMessage());

                    } else {

                        src.sendMessage(Message.LIST_WARZONES.getMessage(String.join(", ", warzoneNames)));

                    }

                    return CommandResult.success();

                })
                .build();

    }

    public CommandSpec getSpec() {
        return this.spec;
    }

}

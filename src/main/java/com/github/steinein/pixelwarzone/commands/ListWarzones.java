package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.messages.Message;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

import java.util.Set;

public class ListWarzones {

    private final PixelWarzone plugin;

    public ListWarzones(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        Set<String> names = plugin.getPluginConfig().getWarzoneNames();
        if (names.isEmpty()) {
            ctx.getSource().sendSystemMessage(Message.NO_DEFINED_WARZONES.getMessage());
        } else {
            ctx.getSource().sendSystemMessage(Message.LIST_WARZONES.getMessage(String.join(", ", names)));
        }
        return 1;
    }
}

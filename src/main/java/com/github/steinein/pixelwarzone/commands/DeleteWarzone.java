package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.messages.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public class DeleteWarzone {

    private final PixelWarzone plugin;

    public DeleteWarzone(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");

        if (!plugin.getPluginConfig().getWarzoneNames().contains(name)) {
            ctx.getSource().sendSystemMessage(Message.WARZONE_NOT_EXIST.getMessage(name));
            return 0;
        }

        plugin.getPluginConfig().deleteWarzone(name);
        plugin.updateWarzoneList();
        ctx.getSource().sendSystemMessage(Message.DELET_WARZONE.getMessage(name));
        return 1;
    }
}

package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.selection.WarzoneSelection;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class DefineWarzone {

    private final PixelWarzone plugin;

    public DefineWarzone(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Message.NOT_A_PLAYER_ERROR.getMessage());
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "name");
        Optional<WarzoneSelection> optSel = plugin.getSelectionsManager().getSelection(player);

        if (optSel.isEmpty() || !optSel.get().isCompleteSelection()) {
            player.sendSystemMessage(Message.INVALID_SELECTION.getMessage());
            return 0;
        }

        plugin.getPluginConfig().createWarzone(name, optSel.get());
        plugin.getSelectionsManager().removeSelection(player);
        plugin.updateWarzoneList();

        player.sendSystemMessage(Message.ZONE_DEFINE_SUCCESS.getMessage(name));
        return 1;
    }
}

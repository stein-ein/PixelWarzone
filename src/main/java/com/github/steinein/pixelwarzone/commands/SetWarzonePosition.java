package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.selection.Point;
import com.github.steinein.pixelwarzone.selection.WarzoneSelection;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class SetWarzonePosition {

    private final PixelWarzone plugin;

    public SetWarzonePosition(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    public int executePos1(CommandContext<CommandSourceStack> ctx) {
        return setPosition(ctx, true);
    }

    public int executePos2(CommandContext<CommandSourceStack> ctx) {
        return setPosition(ctx, false);
    }

    private int setPosition(CommandContext<CommandSourceStack> ctx, boolean first) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Message.NOT_A_PLAYER_ERROR.getMessage());
            return 0;
        }

        int x = player.blockPosition().getX();
        int z = player.blockPosition().getZ();
        String world = player.level().dimension().location().toString();
        Point pt = new Point(x, z);

        Optional<WarzoneSelection> selOpt = plugin.getSelectionsManager().getSelection(player);
        WarzoneSelection sel = selOpt.orElse(new WarzoneSelection());

        sel.setWorld(world);
        if (first) sel.setFirstPos(pt);
        else sel.setSecondPos(pt);

        if (selOpt.isEmpty()) {
            plugin.getSelectionsManager().addSelection(player, sel);
        }

        int num = first ? 1 : 2;
        player.sendSystemMessage(Message.SELECTION_DEFINE.getMessage(num, pt.toString()));
        return 1;
    }
}

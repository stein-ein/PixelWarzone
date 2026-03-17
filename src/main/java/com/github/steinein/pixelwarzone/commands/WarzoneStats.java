package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class WarzoneStats {

    private final PixelWarzone plugin;

    public WarzoneStats(final PixelWarzone plugin) {
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

        WarzonePlayer wp = plugin.playerDataMap.get(player.getUUID());
        if (wp == null) {
            player.sendSystemMessage(Message.parseColors("&cNo stats found for you yet."));
            return 0;
        }

        int wins = wp.getWins();
        int losses = wp.getLosses();

        player.sendSystemMessage(Message.parseColors("&4&lWarzone Stats:"));
        player.sendSystemMessage(Message.parseColors("&f - &aWins: " + wins));
        player.sendSystemMessage(Message.parseColors("&f - &cLosses: " + losses));
        player.sendSystemMessage(Message.parseColors("&f - &bRatio: (" + Utils.formatDouble(Utils.ratio(wins, losses)) + ")"));
        return 1;
    }
}

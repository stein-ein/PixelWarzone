package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.ui.Leaderboard;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class WarzoneTopCommand {

    private final PixelWarzone plugin;

    public WarzoneTopCommand(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();

        ServerPlayer player = src.getPlayer(); // null if console

        if (player != null && plugin.getPluginConfig().showGUILeaderboard()) {
            Leaderboard.openMenu(player, plugin);
        } else {
            List<WarzonePlayer> topPlayers = plugin.getDatabase().getTopPlayers();
            src.sendSystemMessage(Message.parseColors("&7&l=== &4&lWarzone Leaderboard &7&l==="));
            for (int i = 0; i < topPlayers.size(); i++) {
                WarzonePlayer wp = topPlayers.get(i);
                src.sendSystemMessage(Message.parseColors(
                        "&7[&f" + (i + 1) + "&7]&e " + wp.getDisplayName() +
                                ": &aWins " + wp.getWins() +
                                " &f-&c Losses " + wp.getLosses()));
            }
        }
        return 1;
    }

}

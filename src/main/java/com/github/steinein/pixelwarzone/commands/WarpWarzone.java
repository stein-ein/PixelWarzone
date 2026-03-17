package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class WarpWarzone {

    private final PixelWarzone plugin;

    public WarpWarzone(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            return 0;
        }

        WarzonePlayer warzonePlayer = new WarzonePlayer(plugin, player);
        if (Utils.checkWarzonePlayer(plugin, player, warzonePlayer)) {
            // Clear potion effects and teleport
            player.removeAllEffects();
            player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(), "warzonerandomrtp");
        }
        return 1;
    }
}

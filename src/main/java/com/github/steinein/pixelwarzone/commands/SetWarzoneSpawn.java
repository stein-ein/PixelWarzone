package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.messages.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class SetWarzoneSpawn {

    private final PixelWarzone plugin;

    public SetWarzoneSpawn(final PixelWarzone plugin) {
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

        if (!plugin.getPluginConfig().getWarzoneNames().contains(name)) {
            player.sendSystemMessage(Message.parseColors(
                    plugin.getPluginConfig().getPrefix() + "&cNo warzone named &e" + name + "&c exists."));
            return 0;
        }

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        String world = player.level().dimension().location().toString();

        plugin.getPluginConfig().setSpawn(name, x, y, z, world);

        player.sendSystemMessage(Message.parseColors(
                plugin.getPluginConfig().getPrefix() +
                "&aSpawn for warzone &e" + name + "&a set to &e" + (int)x + ", " + (int)y + ", " + (int)z));
        return 1;
    }
}

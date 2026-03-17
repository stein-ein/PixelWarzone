package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.selection.DefinedWarzone;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Random;

public class WarzoneRTP {

    private final PixelWarzone plugin;
    private static final Random RANDOM = new Random();

    public WarzoneRTP(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    // Called by /warzonerandomrtp (internal, no name) — picks random
    public int execute(CommandContext<CommandSourceStack> ctx) {
        return teleport(ctx, null);
    }

    // Called by /warzone warp <name>
    public int executeNamed(CommandContext<CommandSourceStack> ctx) {
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name");
        return teleport(ctx, name);
    }

    private int teleport(CommandContext<CommandSourceStack> ctx, String warzoneName) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            return 0;
        }

        WarzonePlayer wp = new WarzonePlayer(plugin, player);
        if (!Utils.checkWarzonePlayer(plugin, player, wp)) return 0;

        List<DefinedWarzone> warzones = plugin.getWarzoneList();
        if (warzones == null || warzones.isEmpty()) {
            player.sendSystemMessage(Message.parseColors("&cNo warzones are defined!"));
            return 0;
        }

        DefinedWarzone warzone;
        if (warzoneName != null) {
            warzone = warzones.stream()
                    .filter(w -> w.getName().equalsIgnoreCase(warzoneName))
                    .findFirst()
                    .orElse(null);
            if (warzone == null) {
                player.sendSystemMessage(Message.parseColors(
                        plugin.getPluginConfig().getPrefix() + "&cNo warzone named &e" + warzoneName + "&c exists."));
                return 0;
            }
        } else {
            warzone = warzones.get(RANDOM.nextInt(warzones.size()));
        }

        var sel = warzone.getSelection();
        int minX = sel.lesserX(), maxX = sel.greaterX();
        int minZ = sel.lesserZ(), maxZ = sel.greaterZ();

        int x = minX + RANDOM.nextInt(Math.max(1, maxX - minX));
        int z = minZ + RANDOM.nextInt(Math.max(1, maxZ - minZ));

        String worldId = warzone.getSelection().getWorld();
        ServerLevel level = null;
        if (worldId != null) {
            net.minecraft.resources.ResourceLocation dim = net.minecraft.resources.ResourceLocation.parse(worldId);
            level = player.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, dim));
        }
        if (level == null) level = player.getServer().overworld();

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        level.getChunk(new BlockPos(x, y, z));

        player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
        player.sendSystemMessage(Message.parseColors(
                plugin.getPluginConfig().getPrefix() + "&aWarping you to &e" + warzone.getName() + "&a!"));
        return 1;
    }
}

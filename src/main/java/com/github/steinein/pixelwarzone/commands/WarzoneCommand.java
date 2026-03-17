package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class WarzoneCommand {

    private final PixelWarzone plugin;

    public WarzoneCommand(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        SetWarzonePosition setPos = new SetWarzonePosition(plugin);

        var cmd = Commands.literal("warzone")
                .then(Commands.literal("warp")
                        .executes(new WarpWarzone(plugin)::execute)
                        .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    plugin.getWarzoneList().forEach(w -> builder.suggest(w.getName()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> new WarzoneRTP(plugin).executeNamed(ctx))))
                .then(Commands.literal("list").executes(new ListWarzones(plugin)::execute))
                .then(Commands.literal("l").executes(new ListWarzones(plugin)::execute))
                .then(Commands.literal("define").then(
                        Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .requires(src -> src.hasPermission(WarzonePermission.ADMIN_LEVEL))
                                .executes(ctx -> new DefineWarzone(plugin).execute(ctx))))
                .then(Commands.literal("def").then(
                        Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .requires(src -> src.hasPermission(WarzonePermission.ADMIN_LEVEL))
                                .executes(ctx -> new DefineWarzone(plugin).execute(ctx))))
                .then(Commands.literal("delete").then(
                        Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .requires(src -> src.hasPermission(WarzonePermission.ADMIN_LEVEL))
                                .executes(ctx -> new DeleteWarzone(plugin).execute(ctx))))
                .then(Commands.literal("del").then(
                        Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .requires(src -> src.hasPermission(WarzonePermission.ADMIN_LEVEL))
                                .executes(ctx -> new DeleteWarzone(plugin).execute(ctx))))
                .then(Commands.literal("endbattle")
                        .executes(ctx -> {
                            plugin.log("endbattle: lambda entered");
                            try {
                                return new EndBattle(plugin).execute(ctx);
                            } catch (Throwable t) {
                                plugin.log("endbattle: EXCEPTION %s: %s", t.getClass().getName(), t.getMessage());
                                return 0;
                            }
                        }))
                .then(Commands.literal("top").executes(new WarzoneTopCommand(plugin)::execute))
                .then(Commands.literal("stats").executes(new WarzoneStats(plugin)::execute))
                .then(Commands.literal("pos1")
                        .requires(src -> src.hasPermission(WarzonePermission.ADMIN_LEVEL))
                        .executes(ctx -> setPos.executePos1(ctx)))
                .then(Commands.literal("first")
                        .requires(src -> src.hasPermission(WarzonePermission.ADMIN_LEVEL))
                        .executes(ctx -> setPos.executePos1(ctx)))
                .then(Commands.literal("pos2")
                        .requires(src -> src.hasPermission(WarzonePermission.ADMIN_LEVEL))
                        .executes(ctx -> setPos.executePos2(ctx)))
                .then(Commands.literal("second")
                        .requires(src -> src.hasPermission(WarzonePermission.ADMIN_LEVEL))
                        .executes(ctx -> setPos.executePos2(ctx)))
                .then(Commands.literal("setspawn")
                        .requires(src -> src.hasPermission(WarzonePermission.ADMIN_LEVEL))
                        .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    plugin.getWarzoneList().forEach(w -> builder.suggest(w.getName()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> new SetWarzoneSpawn(plugin).execute(ctx))))
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(WarzonePermission.ADMIN_LEVEL))
                        .executes(ctx -> {
                            plugin.reload();
                            ctx.getSource().sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal("PixelWarzone config reloaded."));
                            return 1;
                        }));

        dispatcher.register(cmd);
        dispatcher.register(Commands.literal("wzone").redirect(dispatcher.getRoot().getChild("warzone")));

        // Internal RTP command used by /warzone warp to teleport into the warzone
        dispatcher.register(Commands.literal("warzonerandomrtp")
                .executes(ctx -> new WarzoneRTP(plugin).execute(ctx)));
    }
}

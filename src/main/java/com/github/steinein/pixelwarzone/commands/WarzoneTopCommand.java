package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.utils.Utils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import java.util.*;

public class WarzoneTopCommand {

    private final CommandSpec spec;

    public WarzoneTopCommand(final PixelWarzone plugin) {
        this.spec = CommandSpec.builder()
                .description(Text.of("Lists the top warzone players."))
                .permission(WarzonePermission.WARZONE_TOP)
                .executor((src, args) -> execute(src, args, plugin))
                .build();
    }

    public CommandSpec getSpec() {
        return this.spec;
    }

    public CommandResult execute(CommandSource src, CommandContext args, final PixelWarzone plugin) throws CommandException {
        if (src != null) {
            Task.builder().execute(() -> {
                List<WarzonePlayer> playerList = plugin.getDatabase().getTopPlayers();
                Optional<UserStorageService> userStorage = Sponge.getServiceManager().provide(UserStorageService.class);
                if (playerList != null && userStorage.isPresent()) {
                    src.sendMessage(Utils.toText("&7&l=== &4&lWarzone Leaderboard &7&l==="));
                    showPlayers(src, playerList, userStorage.get());
                }
            }).async().submit(plugin);
        }
        return CommandResult.success();
    }

    private void showPlayers(CommandSource src, List<WarzonePlayer> playerList, UserStorageService userStorage) {
        List<WarzonePlayer> sortedPlayers = new ArrayList<>();
        playerList.forEach(wp -> {
            WarzonePlayer onlinePlayer = PixelWarzone.getInstance().playerDataMap.get(wp.getUser().getUniqueId());
            if (onlinePlayer != null) {
                sortedPlayers.add(onlinePlayer);
            } else {
                sortedPlayers.add(wp);
            }
        });
        sortedPlayers.sort(Comparator.comparingDouble(WarzonePlayer::getWins).reversed());
        for (int x = 0; x < sortedPlayers.size(); x++) {
            WarzonePlayer warzonePlayer = sortedPlayers.get(x);
            UUID uuid = warzonePlayer.getUser() != null ? warzonePlayer.getUser().getUniqueId() : warzonePlayer.getSpongePlayer().getUniqueId();
            User user = userStorage.get(uuid).orElse(null);
            if (user != null) {
                int pos = x + 1;
                src.sendMessage(Utils.toText(
                        "&7[&f" + pos + "&7]&e " +
                                user.getName() +
                                ": &aWins " + warzonePlayer.getWins() +
                                " &f-&c Losses " + warzonePlayer.getLosses()
                ));
            }
        }
    }
}
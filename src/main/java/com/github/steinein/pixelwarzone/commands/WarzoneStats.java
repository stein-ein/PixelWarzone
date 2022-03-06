package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.config.PixelmonItemsTools;
import net.minecraft.item.Item;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WarzoneStats {

    private final CommandSpec spec;

    public WarzoneStats(final PixelWarzone plugin) {

        this.spec = CommandSpec.builder()
                .description(Text.of("Checks your warzone stats."))
                .permission(WarzonePermission.WARZONE_STATS)
                .executor((src, args) -> {

                    if (src instanceof Player) {
                        Player player = (Player) src;
                        WarzonePlayer warzonePlayer = plugin.playerDataMap.get(player.getUniqueId());
                        int wins = warzonePlayer.getWins();
                        int losses = warzonePlayer.getLosses();

                        player.sendMessage(Utils.toText("&4&lWarzone Stats:"));
                        player.sendMessage(Utils.toText("&f - &aWins: " + wins));
                        player.sendMessage(Utils.toText("&f - &cLosses: " + losses));
                        player.sendMessage(Utils.toText("&f - &bRatio: (" + Utils.formatDouble(Utils.ratio(wins, losses)) + ")"));
                    }

                    return CommandResult.success();
                })
                .build();

    }

    public CommandSpec getSpec() {
        return this.spec;
    }
}

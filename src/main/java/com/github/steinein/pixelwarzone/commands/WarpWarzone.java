package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.config.PixelmonItemsTools;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

public class WarpWarzone {
    private final CommandSpec spec;

    public WarpWarzone(final PixelWarzone plugin) {

        this.spec = CommandSpec.builder()
                .description(Text.of("Warps to warzone."))
                .permission(WarzonePermission.WARP_WARZONE)
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Player player = (Player) src;
                        WarzonePlayer warzonePlayer = WarzonePlayer.fromSponge(plugin, player);

                        if (warzonePlayer == null) {
                            return CommandResult.success();
                        }

                        if (Utils.checkWarzonePlayer(plugin, player, warzonePlayer)) {
                            player.offer(Keys.POTION_EFFECTS, Collections.emptyList());
                            Sponge.getCommandManager().process(player, "warzonerandomrtp");
                        }
                    }

                    return CommandResult.success();
                })
                .build();

    }

    public CommandSpec getSpec() {
        return this.spec;
    }
}

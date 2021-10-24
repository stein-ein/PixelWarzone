package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.pixelmonmod.pixelmon.config.PixelmonItems;
import com.pixelmonmod.pixelmon.config.PixelmonItemsTools;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WarpWarzone {

    private final CommandSpec spec;
    private final List<Item> duskItems = Arrays.asList(
            PixelmonItemsTools.duskstoneHelm,
            PixelmonItemsTools.duskstoneAxeItem,
            PixelmonItemsTools.duskstoneBoots,
            PixelmonItemsTools.duskstoneHammerItem,
            PixelmonItemsTools.duskstoneHoeItem,
            PixelmonItemsTools.duskstoneLegs,
            PixelmonItemsTools.duskstonePickaxeItem,
            PixelmonItemsTools.duskstonePlate,
            PixelmonItemsTools.duskstoneShovelItem,
            PixelmonItemsTools.duskstoneSwordItem
    );

    public WarpWarzone(final PixelWarzone plugin) {

        this.spec = CommandSpec.builder()
                .description(Text.of("Warps to warzone."))
                .permission(WarzonePermission.WARP_WARZONE)
                .executor((src, args) -> {

                    if (src instanceof Player) {
                        Player player = (Player) src;
                        WarzonePlayer warzonePlayer = WarzonePlayer.fromSponge(plugin, player);
                        if (warzonePlayer.getParty().getTeam().size() < 6) {
                            player.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize("&cYou must have 6 Pokemon to warp to warzone.")));
                            return CommandResult.success();
                        }

                        AtomicBoolean above100 = new AtomicBoolean(true);
                        warzonePlayer.getParty().getTeam().forEach(pokemon -> {
                            if (pokemon.getLevel() < 100) {
                                above100.set(false);
                            }
                        });

                        if (!above100.get()) {
                            player.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize("&cAll of your pokemon must be level 100 to warp to warzone.")));
                            return CommandResult.success();
                        }

                        AtomicBoolean hasDuskItem = new AtomicBoolean(false);
                        InventoryPlayer inventory = warzonePlayer.getForgePlayer().inventory;
                        duskItems.forEach(item -> {
                            if (inventory.hasItemStack(new ItemStack(item))) {
                                hasDuskItem.set(true);
                            }
                        });

                        if (hasDuskItem.get()) {
                            player.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize("&cYou cannot have any dusk items in your inventory.")));
                            return CommandResult.success();
                        }

                        player.offer(Keys.POTION_EFFECTS, Collections.emptyList());
                        Sponge.getCommandManager().process(player, "warzonerandomrtp");
                    }

                    return CommandResult.success();
                })
                .build();

    }

    public CommandSpec getSpec() {
        return this.spec;
    }

}

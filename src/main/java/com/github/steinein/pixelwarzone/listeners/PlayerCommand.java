package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class PlayerCommand {

    private final PixelWarzone plugin;

    public PlayerCommand(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @Listener(order = Order.FIRST)
    public void SendCommand(SendCommandEvent src, @First Player player) {
        WarzonePlayer warzonePlayer = WarzonePlayer.fromSponge(plugin, player);

        if (src.getCommand().equalsIgnoreCase("warzone") || src.getCommand().equalsIgnoreCase("endbattle")) {
            return;
        }

        if (warzonePlayer != null && warzonePlayer.inWarzone()) {
            if (BattleRegistry.getBattle((EntityPlayer) player) != null) {
                player.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize("&aUse &e/warzone endbattle &ato end the battle.")));
                src.setCancelled(true);
            }
        }
    }
}

package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.api.events.raids.StartRaidEvent;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import net.neoforged.bus.api.SubscribeEvent;

public class RaidDenEvents {

    private final PixelWarzone plugin;

    public RaidDenEvents(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onStartRaid(StartRaidEvent event) {
        if (!plugin.getPluginConfig().broadcastDen()) return;

        DenEntity den = event.getDen();
        String world = den.level().dimension().location().toString();
        int locX = (int) den.getX();
        int locZ = (int) den.getZ();

        boolean inWarzone = plugin.getWarzoneList().stream()
                .anyMatch(warzone -> warzone.hasLocation(world, locX, locZ));

        if (inWarzone) {
            event.getRaidPixelmon().remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            den.getServer().getPlayerList().broadcastSystemMessage(
                    Message.parseColors(plugin.getPluginConfig().getPrefix() + "A raid den has started!"),
                    false
            );
        }
    }
}

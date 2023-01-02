package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.utils.Utils;
import com.pixelmonmod.pixelmon.api.events.raids.StartRaidEvent;
import com.pixelmonmod.pixelmon.entities.EntityDen;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.spongepowered.api.Sponge;

public class RaidDenEvents {

    private final PixelWarzone plugin;

    public RaidDenEvents(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void register(StartRaidEvent startRaidEvent) {
        if (this.plugin.getPluginConfig().broadcastDen()) {
            EntityDen entityDen = startRaidEvent.getDen();
            String world = entityDen.world.getWorldInfo().getWorldName();
            int locX = (int) entityDen.posX;
            int locZ = (int) entityDen.posZ;

            boolean inWarzone = this.plugin.getWarzoneList().stream()
                    .anyMatch(warzone -> warzone.hasLocation(world, locX, locZ));

            if (inWarzone) {
                startRaidEvent.getRaidPixelmon().setDead();
                Sponge.getServer().getBroadcastChannel().send(Utils.toText(
                        PixelWarzone.getInstance().getPluginConfig().getPrefix() +
                                "A raid den has started!"
                ));
            }
        }
    }
}

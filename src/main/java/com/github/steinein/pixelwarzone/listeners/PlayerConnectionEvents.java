package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;

public class PlayerConnectionEvents {

    @Listener
    public void PlayerLogin(ClientConnectionEvent.Login e) {
        User p = e.getTargetUser();
        if (PixelWarzone.getInstance().playerDataMap.get(p.getUniqueId()) == null) {
            Task.builder().execute(() -> PixelWarzone.getInstance().getDatabase().loadPlayer(p))
                    .async()
                    .submit(PixelWarzone.getInstance());
        }
    }

    @Listener
    public void PlayerDisconnect(ClientConnectionEvent.Disconnect e) {
        Player spongePlayer = e.getTargetEntity();
        WarzonePlayer warzonePlayer = PixelWarzone.getInstance().playerDataMap.get(spongePlayer.getUniqueId());
        if (warzonePlayer != null) {
            Task.builder().execute(() -> PixelWarzone.getInstance().getDatabase().savePlayer(warzonePlayer))
                    .async()
                    .submit(PixelWarzone.getInstance());
        } else {
            PixelWarzone.getInstance().getLogger().warn("Failed to save " + Sponge.getServer().getPlayer(spongePlayer.getUniqueId()).get().getName());
        }
    }
}

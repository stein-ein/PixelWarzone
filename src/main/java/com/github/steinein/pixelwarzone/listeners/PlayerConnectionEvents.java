package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

public class PlayerConnectionEvents {

    private final PixelWarzone plugin;

    public PlayerConnectionEvents(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID uuid = sp.getUUID();

        // Register a WarzonePlayer entry first (wins/losses start at 0)
        WarzonePlayer wp = new WarzonePlayer(plugin, sp);
        plugin.playerDataMap.put(uuid, wp);

        // Load stats from DB
        plugin.getDatabase().loadPlayer(uuid);

        // Safety net: clear any stuck guiOpened flag from a previous broken battle.
        // guiOpened=true blocks all battle starts with "Player is busy!".
        // It should reset on reconnect but can get stuck if /endbattle was used.
        var party = StorageProxy.getPartyNow(sp);
        if (party != null) party.guiOpened = false;

        // Also clean up any orphaned battle registry entry
        if (BattleRegistry.inBattle(sp)) {
            var bc = BattleRegistry.getBattle(sp);
            if (bc != null) BattleRegistry.deRegisterBattle(bc);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID uuid = sp.getUUID();

        WarzonePlayer wp = plugin.playerDataMap.get(uuid);
        if (wp != null) {
            Thread.ofVirtual().start(() -> plugin.getDatabase().savePlayer(wp));
            plugin.playerDataMap.remove(uuid);
        } else {
            plugin.getLogger().warn("Failed to save {}: not in playerDataMap", sp.getName().getString());
        }
    }
}

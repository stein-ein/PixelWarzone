package com.github.steinein.pixelwarzone.listeners;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.commands.EndBattle;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;

public class PlayerCommand {

    private final PixelWarzone plugin;

    public PlayerCommand(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) return;

        String raw = event.getParseResults().getReader().getString().trim();
        String input = raw.startsWith("/") ? raw.substring(1) : raw;

        // Intercept Pixelmon's /endbattle when the player is in a warzone PvP battle
        if (input.equals("endbattle")) {
            // Use direct bounds check — inWarzone() exempts ops but they should still forfeit
            boolean inWarzone = plugin.getWarzoneList().stream().anyMatch(w -> w.hasPlayer(player));
            WarzonePlayer warzonePlayer = new WarzonePlayer(plugin, player);
            boolean inBattle = BattleRegistry.inBattle(player);
            plugin.log("PlayerCommand: %s ran /endbattle inWarzone=%s inBattle=%s pos=(%d,%d,%d)",
                    player.getName().getString(), inWarzone, inBattle,
                    player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ());
            if (inWarzone && inBattle) {
                var bc = warzonePlayer.getBattle();
                boolean isPvP = bc != null && bc.participants.stream()
                        .filter(p -> p instanceof PlayerParticipant).count() > 1;
                plugin.log("PlayerCommand: %s isPvP=%s", player.getName().getString(), isPvP);
                if (isPvP) {
                    event.setCanceled(true);
                    new EndBattle(plugin).forfeit(player);
                    return;
                }
            }
            // Not in warzone PvP — let Pixelmon's /endbattle run normally
            return;
        }

        // Block all other commands while in a warzone battle
        if (input.startsWith("warzone") || input.startsWith("wzone")) return;

        WarzonePlayer warzonePlayer = new WarzonePlayer(plugin, player);
        if (warzonePlayer.inWarzone() && BattleRegistry.inBattle(player)) {
            player.sendSystemMessage(Message.parseColors("&aUse &e/warzone endbattle &ato end the battle."));
            event.setCanceled(true);
        }
    }
}

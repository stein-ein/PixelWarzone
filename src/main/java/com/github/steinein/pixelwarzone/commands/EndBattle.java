package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePlayer;
import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.api.battles.BattleEndCause;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class EndBattle {

    private final PixelWarzone plugin;

    public EndBattle(final PixelWarzone plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when a player runs /warzone endbattle.
     * If they are in a PvP battle in the warzone, they forfeit — treated as the loser.
     * If they are in a wild/NPC battle, it just ends cleanly with no consequences.
     */
    public int execute(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            // Console/non-player — admin force-end is not supported via this path
            ctx.getSource().sendSystemMessage(Component.literal("Only players can use this command."));
            return 0;
        }

        // Use a direct bounds check — inWarzone() exempts ops (level 3+), but players
        // should still be able to forfeit regardless of op status.
        boolean inWarzone = plugin.getWarzoneList().stream().anyMatch(w -> w.hasPlayer(player));
        plugin.log("EndBattle: %s inWarzone=%s pos=(%d,%d,%d)", player.getName().getString(), inWarzone,
                player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ());
        if (!inWarzone) {
            player.sendSystemMessage(Component.literal("You are not in the warzone!"));
            return 0;
        }

        return forfeit(player);
    }

    /**
     * Applies warzone forfeit logic for a player who ended the battle (via any command).
     * PvP → steal a pokemon and teleport loser. Wild/NPC → end cleanly.
     * Returns 1 on success, 0 on failure.
     */
    public int forfeit(ServerPlayer player) {
        WarzonePlayer warzonePlayer = new WarzonePlayer(plugin, player);

        boolean inBattle = warzonePlayer.inBattle();
        plugin.log("EndBattle: %s inBattle=%s", player.getName().getString(), inBattle);
        if (!inBattle) {
            warzonePlayer.sendMessage(Message.NOT_IN_BATTLE);
            return 1;
        }

        BattleController bc = warzonePlayer.getBattle();
        if (bc == null) {
            plugin.log("EndBattle: bc is null for %s", player.getName().getString());
            return 0;
        }

        long pvpCount = bc.participants.stream()
                .filter(p -> p instanceof PlayerParticipant).count();
        plugin.log("EndBattle: %s pvpCount=%d, total participants=%d, types=%s",
                player.getName().getString(), pvpCount, bc.participants.size(),
                bc.participants.stream().map(p -> p.getClass().getSimpleName()).toList().toString());

        if (pvpCount == bc.participants.size() && pvpCount == 2) {
            // PvP battle — forfeit. Initiator is the loser.
            WarzonePlayer opponent = warzonePlayer.getBattleOpponent();
            if (opponent == null) {
                plugin.log("EndBattle: opponent is null for %s", player.getName().getString());
                return 0;
            }

            // End the battle first (COMMAND is abnormal — won't re-trigger BattleEndEvent handler)
            bc.endBattle(BattleEndCause.COMMAND);
            BattleRegistry.deRegisterBattle(bc);

            // Explicitly clear guiOpened on both players — the client close packet
            // (GuiOpenClosePacket) may never arrive if the screen wasn't fully open,
            // leaving both players permanently stuck with "Player is busy!".
            var forfeiterParty = com.pixelmonmod.pixelmon.api.storage.StorageProxy.getPartyNow(player);
            if (forfeiterParty != null) forfeiterParty.guiOpened = false;
            var opponentParty = com.pixelmonmod.pixelmon.api.storage.StorageProxy.getPartyNow(opponent.getPlayer());
            if (opponentParty != null) opponentParty.guiOpened = false;

            // Apply loss consequences after battle is cleanly ended
            int losePokemon = plugin.getPluginConfig().losePokemon();
            plugin.log("EndBattle: attempting to remove %d pokemon from %s", losePokemon, player.getName().getString());
            var pokemon = warzonePlayer.removePokemon(losePokemon);
            plugin.log("EndBattle: removePokemon returned %s", pokemon == null ? "null" : pokemon.getDisplayName().getString());
            if (pokemon != null) {
                warzonePlayer.sendMessage(Message.LOST_POKEMON, pokemon.getDisplayName().getString());
                boolean gave = opponent.givePokemon(pokemon);
                if (gave) {
                    opponent.sendMessage(Message.GAINED_POKEMON, pokemon.getDisplayName().getString());
                }

                // Update win/loss records
                WarzonePlayer dbWinner = plugin.playerDataMap.get(opponent.getPlayer().getUUID());
                WarzonePlayer dbLoser = plugin.playerDataMap.get(player.getUUID());
                if (dbWinner != null) { dbWinner.setWins(dbWinner.getWins() + 1); plugin.getDatabase().savePlayer(dbWinner); }
                if (dbLoser != null)  { dbLoser.setLosses(dbLoser.getLosses() + 1); plugin.getDatabase().savePlayer(dbLoser); }

                // Heal winner
                if (opponent.getParty() != null) opponent.getParty().heal();
            }

            // Warp forfeiter back to spawn
            teleportToSpawn(player);

        } else {
            // Wild/NPC battle — end cleanly, no consequences
            bc.endBattle(BattleEndCause.COMMAND);
            BattleRegistry.deRegisterBattle(bc);
            player.sendSystemMessage(Message.parseColors(
                    plugin.getPluginConfig().getPrefix() + "&aBattle ended."));
        }

        return 1;
    }

    private void teleportToSpawn(ServerPlayer player) {
        var config = plugin.getPluginConfig();
        if (config == null) return;
        var warzone = plugin.getWarzoneList().stream()
                .filter(w -> w.hasPlayer(player)).findFirst().orElse(null);
        String warzoneName = warzone != null ? warzone.getName() : null;
        if (warzoneName == null || !config.hasSpawn(warzoneName)) return;
        double[] pos = config.getSpawn(warzoneName);
        net.minecraft.resources.ResourceLocation dim =
                net.minecraft.resources.ResourceLocation.parse(config.getSpawnWorld(warzoneName));
        net.minecraft.server.level.ServerLevel level = player.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION, dim));
        if (level == null) level = player.getServer().overworld();
        player.teleportTo(level, pos[0], pos[1], pos[2], player.getYRot(), player.getXRot());
    }
}

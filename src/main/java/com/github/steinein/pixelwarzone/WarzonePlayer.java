package com.github.steinein.pixelwarzone;

import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.api.battles.BattleEndCause;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class WarzonePlayer {

    private final PixelWarzone plugin;
    private final ServerPlayer player;
    private final String offlineName; // set for DB-only records

    private int wins;
    private int losses;

    // Constructor for online players
    public WarzonePlayer(final PixelWarzone plugin, final ServerPlayer player) {
        this.plugin = plugin;
        this.player = player;
        this.offlineName = null;
    }

    // Constructor for offline/DB-only records (no live player reference)
    public WarzonePlayer(UUID uuid, String name, int wins, int losses) {
        this.plugin = null;
        this.player = null;
        this.offlineName = name != null ? name : "Unknown";
        this.wins = wins;
        this.losses = losses;
    }

    public String getDisplayName() {
        if (player != null) return player.getName().getString();
        return offlineName != null ? offlineName : "Unknown";
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public void setWins(int wins) { this.wins = wins; }
    public void setLosses(int losses) { this.losses = losses; }

    public boolean inWarzone() {
        if (player == null) return false;
        // Op level 3+ are excluded from warzone detection
        if (player.hasPermissions(WarzonePermission.EXEMPT_LEVEL)) return false;
        return plugin.getWarzoneList().stream().anyMatch(w -> w.hasPlayer(player));
    }

    public boolean inBattle() {
        if (player == null) return false;
        return BattleRegistry.inBattle(player);
    }

    public BattleController getBattle() {
        if (player == null) return null;
        return BattleRegistry.getBattle(player);
    }

    public WarzonePlayer getBattleOpponent() {
        BattleController bc = getBattle();
        if (bc == null) return null;
        for (var p : bc.participants) {
            if (p instanceof PlayerParticipant pp && !pp.player.getUUID().equals(player.getUUID())) {
                return new WarzonePlayer(plugin, pp.player);
            }
        }
        return null;
    }

    /**
     * Removes one random Pokemon from this player's party and returns it.
     */
    public Pokemon removePokemon(final int howMany) {
        PlayerPartyStorage party = getParty();
        if (party == null) {
            if (plugin != null) plugin.log("removePokemon: party is null for %s — aborting.", this);
            return null;
        }

        List<Integer> validSlots = new ArrayList<>();
        Pokemon[] all = party.getAll();
        for (int i = 0; i < all.length; i++) {
            if (all[i] != null) validSlots.add(i);
        }

        if (plugin != null) plugin.log("removePokemon: %s has %d valid slots, need more than %d to remove.", this, validSlots.size(), howMany);

        if (validSlots.size() <= howMany) {
            if (plugin != null) plugin.log("removePokemon: %s has too few Pokemon (%d <= %d) — aborting.", this, validSlots.size(), howMany);
            return null;
        }

        int slot = validSlots.get(new Random().nextInt(validSlots.size()));
        Pokemon removed = party.get(slot);
        if (removed == null) {
            if (plugin != null) plugin.log("removePokemon: slot %d returned null Pokemon for %s — aborting.", slot, this);
            return null;
        }

        if (plugin != null) plugin.log("removePokemon: removing %s (slot %d) from %s.", removed.getDisplayName().getString(), slot, player.getName().getString());
        party.set(slot, null);
        return removed;
    }

    public boolean givePokemon(final Pokemon pokemon) {
        PlayerPartyStorage party = getParty();
        if (party == null || pokemon == null) return false;
        var result = party.addSilently(pokemon);
        // TriBoolean: TRUE means success
        return result == com.pixelmonmod.pixelmon.api.enums.TriBoolean.TRUE;
    }

    public void sendMessage(final Message message, final Object... args) {
        if (player != null) player.sendSystemMessage(message.getMessage(args));
    }

    public void sendMessage(final Component component) {
        if (player != null) player.sendSystemMessage(component);
    }

    public PlayerPartyStorage getParty() {
        if (player == null) return null;
        return StorageProxy.getPartyNow(player);
    }

    @Override
    public String toString() {
        if (player == null) return "[Offline WarzonePlayer]";
        return "[Name: " + player.getName().getString() + "; UUID: " + player.getUUID() + "]";
    }
}

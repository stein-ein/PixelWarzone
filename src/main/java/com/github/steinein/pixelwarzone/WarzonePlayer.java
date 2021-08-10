package com.github.steinein.pixelwarzone;

import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleControllerBase;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.storage.PlayerPartyStorage;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;

public class WarzonePlayer {

    private final PixelWarzone plugin;

    private final Player spongePlayer;

    public EntityPlayerMP getForgePlayer() {
        return forgePlayer;
    }

    private final EntityPlayerMP forgePlayer;
    private final Location<World> location;

    private WarzonePlayer(final PixelWarzone plugin, final EntityPlayerMP forgePlayer) {
        this.plugin = plugin;
        this.spongePlayer = (Player) forgePlayer;
        this.location = ((Player) forgePlayer).getLocation();
        this.forgePlayer = forgePlayer;
    }

    private WarzonePlayer(final PixelWarzone plugin, final Player spongePlayer) {
        this.plugin = plugin;
        this.spongePlayer = spongePlayer;
        this.location = spongePlayer.getLocation();
        this.forgePlayer = (EntityPlayerMP) spongePlayer;
    }

    public static WarzonePlayer fromForge(final PixelWarzone plugin, final EntityPlayerMP player) {
        return new WarzonePlayer(plugin, player);
    }

    public static WarzonePlayer fromSponge(final PixelWarzone plugin, final Player player) {
        return new WarzonePlayer(plugin, player);
    }

    public boolean inWarzone() {

        if (this.spongePlayer.hasPermission(WarzonePermission.WARZONE_EXCLUDE)) {
            this.plugin.debug("%s has the %s permission, therefore they are not counted within warzones.",
                    this.toString(), WarzonePermission.WARZONE_EXCLUDE);
            return false;
        }

        return this.plugin.getWarzoneList().stream().anyMatch(warzone -> warzone.hasPlayer(this.spongePlayer));

    }

    public boolean inBattle() {
        return BattleRegistry.getBattle(this.forgePlayer) != null;
    }

    public BattleControllerBase getBattle() {
        return BattleRegistry.getBattle(this.forgePlayer);
    }

    public WarzonePlayer getBattleOpponent() {
        BattleControllerBase bcb = this.getBattle();
        List<PlayerParticipant> players = bcb.getPlayers();

        for (PlayerParticipant player : players) {
            if (player.player.equals(this.forgePlayer)) continue;

            return WarzonePlayer.fromForge(plugin, player.player);
        }

        return null;
    }

    public Pokemon removePokemon(final int howMany) {

        final PlayerPartyStorage party = this.getParty();
        final int partySize = party.getTeam().size();
        if (partySize <= howMany) {
            this.plugin.debug("%s has %d Pokemon in party, while the plugin would take %d. Aborting removal.",
                    this.toString(), partySize, howMany);
            return null; // Can't take more pokemon than the player has
        }

        final List<Integer> validSlots = this.getValidSlots();
        this.plugin.debug("%s has %d valid (non-empty) slots in their party.", this.toString(), validSlots.size());

        final int randomSlot = validSlots.get(new Random().nextInt(validSlots.size()));
        this.plugin.debug("Slot %d has been chosen for removal.", randomSlot);

        final Pokemon removedPokemon = party.get(randomSlot);
        if (removedPokemon == null) {
            this.plugin.debug("Something went wrong during Pokemon removal. Could not find any pokemon at slot %d.",
                    randomSlot);
            return null;
        }

        this.plugin.log("Removing Pokemon %s (slot: %d) from player %s.",
                removedPokemon.getDisplayName(),
                randomSlot,
                this.spongePlayer.getDisplayNameData().displayName());

        party.set(randomSlot, null);

        return removedPokemon;

    }

    public boolean givePokemon(final Pokemon pokemon) {
        return this.getParty().add(pokemon);
    }

    public void sendMessage(final Message message, final Object... args) {
        this.spongePlayer.sendMessage(message.getMessage(args));
    }

    public void sendMessage(final Text text) {
        this.spongePlayer.sendMessage(text);
    }

    private List<Integer> getValidSlots() {

        final Pokemon[] partyPokemon = this.getParty().getAll();
        final List<Integer> validSlots = new ArrayList<>();

        for (int pokeIndex = 0; pokeIndex < partyPokemon.length; pokeIndex++) {

            if (partyPokemon[pokeIndex] != null) {
                this.plugin.debug("Found a valid Pokemon slot at index %d.", pokeIndex);
                validSlots.add(pokeIndex);
            }

        }

        return validSlots;

    }

    public PlayerPartyStorage getParty() {
        return Pixelmon.storageManager.getParty(this.forgePlayer);
    }

    @Override
    public String toString() {
        return "[Name: " + this.spongePlayer.getName() +
                "; UUID: " + this.spongePlayer.getUniqueId() +
                "; Location: " + this.location + "]";
    }
}

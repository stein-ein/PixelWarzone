package com.github.steinein.pixelwarzone;

import com.github.steinein.pixelwarzone.messages.Message;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.storage.PlayerPartyStorage;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WarzonePlayer {

    private final PixelWarzone plugin;

    private final Player spongePlayer;
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

        this.plugin.debug("Removing Pokemon %s from slot position %d.", removedPokemon.getDisplayName(), randomSlot);
        party.set(randomSlot, null);

        return removedPokemon;

    }

    public boolean givePokemon(final Pokemon pokemon) {
        return this.getParty().add(pokemon);
    }

    public void sendMessage(final Message message, final Object... args) {
        this.spongePlayer.sendMessage(message.getMessage(args));
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

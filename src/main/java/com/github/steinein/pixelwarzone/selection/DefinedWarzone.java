package com.github.steinein.pixelwarzone.selection;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class DefinedWarzone {

    private final String name;
    private final WarzoneSelection selection;

    public DefinedWarzone(final String name, final WarzoneSelection selection) {
        this.name = name;
        this.selection = selection;
    }

    /*

    Since the rectangle is axis-aligned, we can just check whether the player's position conforms to bounds
    of its sides.

     */
    public boolean hasPlayer(final Player player) {
        final Location<World> loc = player.getLocation();

        final int locX = loc.getBlockX();
        final int locZ = loc.getBlockZ();

        final boolean withinXBound = (locX <= this.selection.greaterX()) && (locX >= this.selection.lesserX());
        final boolean withinZBound = (locZ <= this.selection.greaterZ()) && (locZ >= this.selection.lesserZ());

        return withinXBound && withinZBound && selection.getWorld() != null && selection.getWorld().equals(player.getWorld().getName());

    }

}

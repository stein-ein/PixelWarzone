package com.github.steinein.pixelwarzone.selection;

import net.minecraft.server.level.ServerPlayer;

public class DefinedWarzone {

    private final String name;
    private final WarzoneSelection selection;

    public DefinedWarzone(final String name, final WarzoneSelection selection) {
        this.name = name;
        this.selection = selection;
    }

    public String getName() {
        return this.name;
    }

    public WarzoneSelection getSelection() {
        return this.selection;
    }

    public boolean hasPlayer(final ServerPlayer player) {
        final int locX = player.blockPosition().getX();
        final int locZ = player.blockPosition().getZ();
        final String world = player.level().dimension().location().toString();

        final boolean withinXBound = (locX <= this.selection.greaterX()) && (locX >= this.selection.lesserX());
        final boolean withinZBound = (locZ <= this.selection.greaterZ()) && (locZ >= this.selection.lesserZ());

        return withinXBound && withinZBound
                && selection.getWorld() != null
                && selection.getWorld().equals(world);
    }

    public boolean hasLocation(final String world, final int locX, final int locZ) {
        final boolean withinXBound = (locX <= this.selection.greaterX()) && (locX >= this.selection.lesserX());
        final boolean withinZBound = (locZ <= this.selection.greaterZ()) && (locZ >= this.selection.lesserZ());

        return withinXBound && withinZBound
                && selection.getWorld() != null
                && selection.getWorld().equals(world);
    }
}

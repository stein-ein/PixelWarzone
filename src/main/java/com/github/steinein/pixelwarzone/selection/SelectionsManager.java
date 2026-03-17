package com.github.steinein.pixelwarzone.selection;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SelectionsManager {

    private final Map<UUID, WarzoneSelection> selections = new HashMap<>();

    public void addSelection(final ServerPlayer player, final WarzoneSelection selection) {
        this.selections.put(player.getUUID(), selection);
    }

    public void removeSelection(final ServerPlayer player) {
        this.selections.remove(player.getUUID());
    }

    public Optional<WarzoneSelection> getSelection(final ServerPlayer player) {
        return Optional.ofNullable(this.selections.get(player.getUUID()));
    }
}

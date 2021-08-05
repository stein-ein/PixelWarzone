package com.github.steinein.pixelwarzone.selection;

import org.spongepowered.api.entity.living.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SelectionsManager {

    private final Map<Player, WarzoneSelection> selections = new HashMap<>();

    public void addSelection(final Player player, final WarzoneSelection selection) {
        this.selections.put(player, selection);
    }

    public void removeSelection(final Player player) {
        if (this.hasSelection(player)) {
            this.selections.remove(player);
        }
    }

    public Optional<WarzoneSelection> getSelection(final Player player) {
        return Optional.ofNullable(this.selections.get(player));
    }

    private boolean hasSelection(final Player player) {
        return this.selections.containsKey(player);
    }

}

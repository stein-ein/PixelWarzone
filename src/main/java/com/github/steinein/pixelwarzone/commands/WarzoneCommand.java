package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

public class WarzoneCommand {

    private final CommandSpec spec;

    public WarzoneCommand(final PixelWarzone plugin) {

        SetWarzonePosition setPos = new SetWarzonePosition(plugin);

        spec = CommandSpec.builder()
                .description(Text.of("Basic command for PixelWarzone"))
                .child(new ListWarzones(plugin).getSpec(), "list", "l")
                .child(new DefineWarzone(plugin).getSpec(), "define", "def", "new")
                .child(new DeleteWarzone(plugin).getSpec(), "delete", "del")
                .child(setPos.getSpecPosFirst(), "pos1", "first")
                .child(setPos.getSpecPosSecond(), "pos2", "second")
                .build();

    }

    public CommandSpec getSpec() {
        return this.spec;
    }

}

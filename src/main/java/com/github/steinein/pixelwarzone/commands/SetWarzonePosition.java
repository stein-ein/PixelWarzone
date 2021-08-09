package com.github.steinein.pixelwarzone.commands;

import com.github.steinein.pixelwarzone.PixelWarzone;
import com.github.steinein.pixelwarzone.WarzonePermission;
import com.github.steinein.pixelwarzone.messages.Message;
import com.github.steinein.pixelwarzone.selection.Point;
import com.github.steinein.pixelwarzone.selection.SelectionsManager;
import com.github.steinein.pixelwarzone.selection.WarzoneSelection;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class SetWarzonePosition {

    private final CommandSpec specPosFirst;
    private final CommandSpec specPosSecond;

    public SetWarzonePosition(final PixelWarzone plugin) {

        specPosFirst = CommandSpec.builder()
                .description(Text.of("Sets first position for a new Warzone."))
                .permission(WarzonePermission.DEFINE_WARZONE)
                .executor((src, args) -> {

                    if (src instanceof Player) {

                        this.setPosition(SelectionType.FIRST, (Player) src, plugin.getSelectionsManager());

                        return CommandResult.success();

                    } else {

                        src.sendMessage(Message.NOT_A_PLAYER_ERROR.getMessage());

                        return CommandResult.empty();

                    }

                })
                .build();

        specPosSecond = CommandSpec.builder()
                .description(Text.of("Sets second position for a new Warzone."))
                .permission(WarzonePermission.DEFINE_WARZONE)
                .executor((src, args) -> {

                    if (src instanceof Player) {

                        this.setPosition(SelectionType.SECOND, (Player) src, plugin.getSelectionsManager());

                        return CommandResult.success();

                    } else {

                        src.sendMessage(Message.NOT_A_PLAYER_ERROR.getMessage());

                        return CommandResult.empty();

                    }

                })
                .build();

    }

    private void setPosition(final SelectionType type, final Player player, final SelectionsManager selMgr) {

        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        Point playerLoc = new Point(x, z);

        Optional<WarzoneSelection> selection = selMgr.getSelection(player);

        if (selection.isPresent()) {

            selection.get().setWorld(player.getWorld().getName());

            if (type == SelectionType.FIRST) {
                selection.get().setFirstPos(playerLoc);
            } else {
                selection.get().setSecondPos(playerLoc);
            }


        } else {

            WarzoneSelection newSel = new WarzoneSelection();

            if (type == SelectionType.FIRST) {
                newSel.setFirstPos(playerLoc);
            } else {
                newSel.setSecondPos(playerLoc);
            }

            selMgr.addSelection(player, newSel);

        }

        int num = (type == SelectionType.FIRST) ? 1 : 2;

        player.sendMessage(Message.SELECTION_DEFINE.getMessage(num, playerLoc.toString()));

    }

    private enum SelectionType {
        FIRST,
        SECOND
    }

    public CommandSpec getSpecPosFirst() {
        return this.specPosFirst;
    }

    public CommandSpec getSpecPosSecond() {
        return this.specPosSecond;
    }

}

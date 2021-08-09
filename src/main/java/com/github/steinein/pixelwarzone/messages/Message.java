package com.github.steinein.pixelwarzone.messages;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public enum Message {

    // Errors

    NOT_A_PLAYER_ERROR("&cOnly players may execute this command!"),

    NO_DEFINED_WARZONES("&cThere are no currently defined warzones!"),

    WARZONE_NOT_EXIST("&cNo warzone with the name &6%s &cexists!"),

    INVALID_SELECTION("&cYour selection is invalid! Make sure to select two diagonally opposing points!"),

    MISSING_ARGUMENT("&cYou have not provided argument &6%s&c!"),

    NOT_IN_BATTLE("&cYou are not in a battle!"),

    // Success

    ZONE_DEFINE_SUCCESS("&aYou have successfully defined zone &6%s&a!"),

    SELECTION_DEFINE("&aYou have successfully set position &6%s &ato &6%s&a!"),

    LIST_WARZONES("&aCurrently defined warzones are: &6%s"),

    DELET_WARZONE("&aSuccessfully deleted warzone &6%s&a!"),

    LOST_POKEMON("&aYou have lost your &6%s&a!"),

    GAINED_POKEMON("&aYou have won your opponent's &6%s&a!"),

    // Proposal

    END_BATTLE_PROPOSE("CLICK HERE TO END THIS BATTLE (YOU WILL NOT LOSE YOUR POKEMON!)"); // Use as raw & unformatted

    private final String message;

    Message(final String message) {
        this.message = message;
    }

    public Text getMessage(final Object... args) {
        final String argumentedMessage = this.withArgs(this.message, args);

        return TextSerializers.FORMATTING_CODE.deserialize(argumentedMessage);
    }

    public String getRaw() {
        return this.message;
    }

    private String withArgs(String message, final Object... args) {
        return String.format(message, args);
    }

}

package com.github.steinein.pixelwarzone.messages;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

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
    END_BATTLE_PROPOSE("&aYou have received a request to end the battle. Use &e/warzone endbattle accept &c(YOU WILL NOT LOSE YOUR POKEMON!)");

    private final String message;

    Message(final String message) {
        this.message = message;
    }

    public Component getMessage(final Object... args) {
        return parseColors(String.format(this.message, args));
    }

    public String getRaw() {
        return this.message;
    }

    public static Component parseColors(String input) {
        if (input == null) return Component.empty();
        MutableComponent result = Component.empty();
        Style currentStyle = Style.EMPTY;
        int i = 0;
        StringBuilder buf = new StringBuilder();
        while (i < input.length()) {
            if (input.charAt(i) == '&' && i + 1 < input.length()) {
                if (buf.length() > 0) {
                    result = result.append(Component.literal(buf.toString()).withStyle(currentStyle));
                    buf.setLength(0);
                }
                char code = input.charAt(i + 1);
                currentStyle = applyCode(currentStyle, code);
                i += 2;
            } else {
                buf.append(input.charAt(i));
                i++;
            }
        }
        if (buf.length() > 0) {
            result = result.append(Component.literal(buf.toString()).withStyle(currentStyle));
        }
        return result;
    }

    private static Style applyCode(Style style, char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> Style.EMPTY.withColor(ChatFormatting.BLACK);
            case '1' -> Style.EMPTY.withColor(ChatFormatting.DARK_BLUE);
            case '2' -> Style.EMPTY.withColor(ChatFormatting.DARK_GREEN);
            case '3' -> Style.EMPTY.withColor(ChatFormatting.DARK_AQUA);
            case '4' -> Style.EMPTY.withColor(ChatFormatting.DARK_RED);
            case '5' -> Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE);
            case '6' -> Style.EMPTY.withColor(ChatFormatting.GOLD);
            case '7' -> Style.EMPTY.withColor(ChatFormatting.GRAY);
            case '8' -> Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
            case '9' -> Style.EMPTY.withColor(ChatFormatting.BLUE);
            case 'a' -> Style.EMPTY.withColor(ChatFormatting.GREEN);
            case 'b' -> Style.EMPTY.withColor(ChatFormatting.AQUA);
            case 'c' -> Style.EMPTY.withColor(ChatFormatting.RED);
            case 'd' -> Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
            case 'e' -> Style.EMPTY.withColor(ChatFormatting.YELLOW);
            case 'f' -> Style.EMPTY.withColor(ChatFormatting.WHITE);
            case 'l' -> style.withBold(true);
            case 'o' -> style.withItalic(true);
            case 'n' -> style.withUnderlined(true);
            case 'm' -> style.withStrikethrough(true);
            case 'k' -> style.withObfuscated(true);
            case 'r' -> Style.EMPTY;
            default -> style;
        };
    }
}

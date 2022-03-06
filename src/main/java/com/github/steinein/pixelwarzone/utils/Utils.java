package com.github.steinein.pixelwarzone.utils;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class Utils {
    public static Text toText(final String str) {
        if (str == null) {
            return (Text) Text.of("");
        }
        return TextSerializers.FORMATTING_CODE.deserialize(str);
    }

    public static double ratio(final int wins, final int losses) {
        if ((wins + losses) == 0) {
            return 0.0;
        }
        return wins / (wins + losses) * 100;
    }

    public static String formatDouble(final double ratio) {
        return String.format("%.2f", ratio) + "%";
    }
}
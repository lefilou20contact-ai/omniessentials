package dev.monserveur.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Petits utilitaires de texte (MiniMessage, nombres, barres de progression). */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component mm(String input) {
        return MM.deserialize(input);
    }

    public static Component mm(String input, TagResolver... resolvers) {
        return MM.deserialize(input, resolvers);
    }

    /** Pour les noms/lore d'items : pas d'italique par défaut. */
    public static Component item(String input) {
        return MM.deserialize(input).decoration(TextDecoration.ITALIC, false);
    }

    /** 12500.5 -> "12 500,5" */
    public static String number(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", symbols).format(value);
    }

    /** Barre de progression MiniMessage, ex : ||||||||------ */
    public static String bar(double ratio, int length) {
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        int filled = (int) Math.round(clamped * length);
        return "<green>" + "|".repeat(filled) + "<dark_gray>" + "|".repeat(length - filled);
    }
}

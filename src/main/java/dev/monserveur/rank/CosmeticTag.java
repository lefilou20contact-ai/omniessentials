package dev.monserveur.rank;

/** Un tag cosmétique affiché dans le chat (ex : ★, ❤, [Fondateur]). */
public record CosmeticTag(String id, String display, double price, String permission) {
}

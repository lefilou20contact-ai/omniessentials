package dev.monserveur.rank;

public record Rank(
        String id,
        String display,
        String prefix,
        String nameColor,
        String chatColor,
        String teamColor,
        int weight,
        int maxHomes,
        double sellMultiplier) {
}

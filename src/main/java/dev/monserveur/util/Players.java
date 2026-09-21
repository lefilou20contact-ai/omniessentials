package dev.monserveur.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Recherche de joueurs et aide à l'auto-complétion. */
public final class Players {

    private Players() {
    }

    /** Joueur en ligne, sinon joueur connu du serveur (sans requête réseau). Null si introuvable. */
    public static OfflinePlayer find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    public static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    public static List<String> filter(List<String> options, String typed) {
        String lower = typed.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}

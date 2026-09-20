package dev.monserveur.home;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.regex.Pattern;

public final class HomeManager {

    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,16}");

    private final MonServeurPlugin plugin;

    public HomeManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    /** Homes autorisés = grade + bonus de niveau. */
    public int maxHomes(Player player) {
        int base = plugin.ranks().of(player).maxHomes();
        int every = plugin.getConfig().getInt("homes.extra-home-every-levels", 10);
        int extra = every > 0 ? plugin.data().get(player).getLevel() / every : 0;
        return base + extra;
    }

    /** Premier nom libre : home, home2, home3... */
    public String nextFreeName(Player player) {
        PlayerData data = plugin.data().get(player);
        if (!data.getHomes().containsKey("home")) {
            return "home";
        }
        for (int i = 2; i < 100; i++) {
            if (!data.getHomes().containsKey("home" + i)) {
                return "home" + i;
            }
        }
        return "home";
    }

    public boolean set(Player player, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (!VALID_NAME.matcher(name).matches()) {
            plugin.msg(player, "home-invalid-name");
            return false;
        }
        PlayerData data = plugin.data().get(player);
        boolean exists = data.getHomes().containsKey(name);
        if (!exists && data.getHomes().size() >= maxHomes(player)) {
            plugin.msg(player, "home-limit", "max", maxHomes(player));
            return false;
        }
        data.getHomes().put(name, player.getLocation().clone());
        plugin.msg(player, "home-set", "name", name);
        return true;
    }

    public void go(Player player, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        Location location = plugin.data().get(player).getHomes().get(name);
        if (location == null) {
            plugin.msg(player, "home-not-found", "name", name);
            return;
        }
        plugin.teleports().teleport(player, location, name);
    }

    public boolean delete(Player player, String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (plugin.data().get(player).getHomes().remove(name) == null) {
            plugin.msg(player, "home-not-found", "name", name);
            return false;
        }
        plugin.msg(player, "home-deleted", "name", name);
        return true;
    }
}

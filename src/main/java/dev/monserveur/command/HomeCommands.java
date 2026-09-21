package dev.monserveur.command;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.gui.HomesMenu;
import dev.monserveur.util.Players;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /sethome /home /delhome /homes */
public final class HomeCommands implements TabExecutor {

    private final MonServeurPlugin plugin;

    public HomeCommands(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return true;
        }
        PlayerData data = plugin.data().get(player);
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "sethome" -> plugin.homes().set(player, args.length > 0 ? args[0] : "home");
            case "home" -> {
                if (args.length > 0) {
                    plugin.homes().go(player, args[0]);
                } else if (data.getHomes().containsKey("home")) {
                    plugin.homes().go(player, "home");
                } else if (data.getHomes().size() == 1) {
                    plugin.homes().go(player, data.getHomes().keySet().iterator().next());
                } else {
                    new HomesMenu(plugin, player).open();
                }
            }
            case "delhome" -> {
                if (args.length == 0) {
                    plugin.msg(sender, "usage", "usage", "/delhome <nom>");
                } else {
                    plugin.homes().delete(player, args[0]);
                }
            }
            case "homes" -> new HomesMenu(plugin, player).open();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (sender instanceof Player player && args.length == 1 && (name.equals("home") || name.equals("delhome"))) {
            return Players.filter(new ArrayList<>(plugin.data().get(player).getHomes().keySet()), args[0]);
        }
        return List.of();
    }
}

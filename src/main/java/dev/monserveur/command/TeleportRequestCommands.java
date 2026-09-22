package dev.monserveur.command;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.util.Players;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /tpa /tpahere /tpaccept /tpdeny /tpacancel /back */
public final class TeleportRequestCommands implements TabExecutor {

    private final MonServeurPlugin plugin;

    public TeleportRequestCommands(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "tpa", "tpahere" -> {
                if (args.length < 1) {
                    plugin.msg(player, "usage", "usage", "/" + command.getName() + " <joueur>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    plugin.msg(player, "player-not-found", "player", args[0]);
                    return true;
                }
                plugin.tpa().request(player, target, command.getName().equalsIgnoreCase("tpahere"));
            }
            case "tpaccept" -> plugin.tpa().accept(player);
            case "tpdeny" -> plugin.tpa().deny(player);
            case "tpacancel" -> plugin.tpa().cancel(player);
            case "back" -> plugin.tpa().back(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && (command.getName().equalsIgnoreCase("tpa") || command.getName().equalsIgnoreCase("tpahere"))) {
            return Players.filter(Players.onlineNames(), args[0]);
        }
        return List.of();
    }
}

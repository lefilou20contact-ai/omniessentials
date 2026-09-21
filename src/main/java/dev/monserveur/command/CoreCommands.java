package dev.monserveur.command;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.gui.MainMenu;
import dev.monserveur.gui.QuestsMenu;
import dev.monserveur.gui.ShopMenu;
import dev.monserveur.gui.TagsMenu;
import dev.monserveur.rank.Rank;
import dev.monserveur.util.Players;
import dev.monserveur.util.Text;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** /menu /shop /spawn /setspawn /level /quests /tag /rank /monserveur */
public final class CoreCommands implements TabExecutor {

    private final MonServeurPlugin plugin;

    public CoreCommands(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        // Commandes réservées aux joueurs
        if (List.of("menu", "shop", "spawn", "setspawn", "level", "quests", "tag").contains(name)) {
            if (!(sender instanceof Player player)) {
                plugin.msg(sender, "player-only");
                return true;
            }
            switch (name) {
                case "menu" -> new MainMenu(plugin, player).open();
                case "shop" -> new ShopMenu(plugin, player).open();
                case "quests" -> new QuestsMenu(plugin, player).open();
                case "tag" -> new TagsMenu(plugin, player).open();
                case "spawn" -> plugin.teleports().teleport(player, plugin.spawn().get(), "spawn");
                case "level" -> level(player);
                default -> setSpawn(player);
            }
            return true;
        }

        switch (name) {
            case "rank" -> rank(sender, args);
            case "monserveur" -> admin(sender, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void setSpawn(Player player) {
        if (!player.hasPermission("monserveur.admin")) {
            plugin.msg(player, "no-permission");
            return;
        }
        plugin.spawn().set(player.getLocation());
        plugin.msg(player, "spawn-set");
    }

    private void level(Player player) {
        PlayerData data = plugin.data().get(player);
        if (data.getLevel() >= plugin.levels().maxLevel()) {
            plugin.msg(player, "level-info-max", "level", data.getLevel());
            return;
        }
        long needed = plugin.levels().xpNeeded(data.getLevel());
        plugin.msg(player, "level-info",
                "level", data.getLevel(),
                "xp", data.getXp(),
                "needed", needed,
                "bar", Text.mm(Text.bar((double) data.getXp() / needed, 20)));
    }

    private void rank(CommandSender sender, String[] args) {
        if (!sender.hasPermission("monserveur.admin")) {
            plugin.msg(sender, "no-permission");
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            String ids = plugin.ranks().all().stream().map(Rank::id).collect(Collectors.joining(", "));
            plugin.msg(sender, "rank-list", "ranks", ids);
            return;
        }
        if (args.length < 3 || !args[0].equalsIgnoreCase("set")) {
            plugin.msg(sender, "usage", "usage", "/rank <list | set <joueur> <grade>>");
            return;
        }
        OfflinePlayer target = Players.find(args[1]);
        if (target == null) {
            plugin.msg(sender, "player-not-found", "player", args[1]);
            return;
        }
        if (!plugin.ranks().exists(args[2])) {
            plugin.msg(sender, "rank-unknown", "rank", args[2]);
            return;
        }
        PlayerData data = plugin.data().getOrLoad(target.getUniqueId(), target.getName());
        data.setRank(args[2].toLowerCase(Locale.ROOT));
        plugin.data().save(data);
        if (target instanceof Player online) {
            plugin.tab().apply(online);
            plugin.tab().updateHeaderFooter(online);
        }
        plugin.msg(sender, "rank-set", "player", data.getName(), "rank", plugin.ranks().get(args[2]).display());
    }

    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("monserveur.admin")) {
            plugin.msg(sender, "no-permission");
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("buildspawn")) {
            buildSpawn(sender, args);
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            plugin.msg(sender, "reloaded");
            return;
        }
        plugin.msg(sender, "usage", "usage", "/monserveur <reload | buildspawn>");
    }

    private void buildSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return;
        }
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "confirm" -> plugin.spawnBuilder().build(player);
            case "undo" -> plugin.spawnBuilder().undo(player);
            default -> plugin.msg(player, "build-warning", "size", plugin.spawnBuilder().size());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("monserveur") && args.length == 1) {
            return Players.filter(List.of("reload", "buildspawn"), args[0]);
        }
        if (name.equals("monserveur") && args.length == 2 && args[0].equalsIgnoreCase("buildspawn")) {
            return Players.filter(List.of("confirm", "undo"), args[1]);
        }
        if (name.equals("rank")) {
            if (args.length == 1) {
                return Players.filter(List.of("list", "set"), args[0]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                return Players.filter(Players.onlineNames(), args[1]);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                List<String> ids = new ArrayList<>();
                plugin.ranks().all().forEach(r -> ids.add(r.id()));
                return Players.filter(ids, args[2]);
            }
        }
        return List.of();
    }
}

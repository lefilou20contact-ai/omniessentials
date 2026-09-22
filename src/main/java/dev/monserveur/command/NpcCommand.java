package dev.monserveur.command;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.npc.Npc;
import dev.monserveur.util.Players;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** /npc create|name|subtitle|action|profession|move|remove|list|hologram|respawn */
public final class NpcCommand implements TabExecutor {

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,24}");
    private static final List<String> SUBCOMMANDS = List.of(
            "create", "name", "subtitle", "action", "profession", "move", "remove", "list", "hologram", "respawn");
    private static final List<String> TYPES = List.of("MENU", "COMMAND", "LINK", "NONE");
    private static final List<String> PROFESSIONS = List.of(
            "armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher",
            "leatherworker", "librarian", "mason", "nitwit", "none", "shepherd", "toolsmith", "weaponsmith");
    private static final List<String> MENUS = List.of("main", "shop", "homes", "quests", "tags");

    private final MonServeurPlugin plugin;

    public NpcCommand(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("monserveur.admin")) {
            plugin.msg(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.msg(sender, "npc-usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "name" -> edit(sender, args, (npc, text) -> npc.name = text);
            case "subtitle" -> edit(sender, args, (npc, text) -> npc.subtitle = text.equals("-") ? "" : text);
            case "profession" -> edit(sender, args, (npc, text) -> npc.profession = text.toLowerCase(Locale.ROOT));
            case "action" -> action(sender, args);
            case "move" -> move(sender, args);
            case "remove" -> remove(sender, args);
            case "list" -> list(sender);
            case "hologram" -> hologram(sender, args);
            case "respawn" -> {
                plugin.npcs().respawnAll();
                plugin.msg(sender, "npc-updated", "id", "*");
            }
            default -> plugin.msg(sender, "npc-usage");
        }
        return true;
    }

    private interface Edit {
        void apply(Npc npc, String text);
    }

    private static String join(String[] args, int from) {
        return args.length > from ? String.join(" ", Arrays.copyOfRange(args, from, args.length)) : "";
    }

    private void create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return;
        }
        if (args.length < 4) {
            plugin.msg(sender, "npc-usage");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!ID.matcher(id).matches()) {
            plugin.msg(sender, "npc-invalid-id");
            return;
        }
        if (plugin.npcs().get(id) != null) {
            plugin.msg(sender, "npc-exists", "id", id);
            return;
        }
        String type = args[3].toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) {
            plugin.msg(sender, "npc-invalid");
            return;
        }
        Location location = player.getLocation();
        Npc npc = new Npc(id);
        npc.world = location.getWorld().getName();
        npc.x = location.getBlockX() + 0.5;
        npc.y = location.getY();
        npc.z = location.getBlockZ() + 0.5;
        npc.yaw = location.getYaw() + 180f; // le PNJ te regarde
        npc.profession = args[2].toLowerCase(Locale.ROOT);
        npc.actionType = type;
        npc.actionValue = join(args, 4);
        if (type.equals("MENU") && npc.actionValue.isBlank()) {
            npc.actionValue = "main";
        }
        plugin.npcs().add(npc);
        plugin.msg(sender, "npc-created", "id", id);
    }

    private void edit(CommandSender sender, String[] args, Edit edit) {
        if (args.length < 3) {
            plugin.msg(sender, "npc-usage");
            return;
        }
        Npc npc = plugin.npcs().get(args[1].toLowerCase(Locale.ROOT));
        if (npc == null) {
            plugin.msg(sender, "npc-not-found", "id", args[1]);
            return;
        }
        edit.apply(npc, join(args, 2));
        plugin.npcs().update(npc);
        plugin.msg(sender, "npc-updated", "id", npc.id);
    }

    private void action(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.msg(sender, "npc-usage");
            return;
        }
        Npc npc = plugin.npcs().get(args[1].toLowerCase(Locale.ROOT));
        if (npc == null) {
            plugin.msg(sender, "npc-not-found", "id", args[1]);
            return;
        }
        String type = args[2].toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) {
            plugin.msg(sender, "npc-invalid");
            return;
        }
        npc.actionType = type;
        npc.actionValue = join(args, 3);
        plugin.npcs().update(npc);
        plugin.msg(sender, "npc-updated", "id", npc.id);
    }

    private void move(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            plugin.msg(sender, "npc-usage");
            return;
        }
        Npc npc = plugin.npcs().get(args[1].toLowerCase(Locale.ROOT));
        if (npc == null) {
            plugin.msg(sender, "npc-not-found", "id", args[1]);
            return;
        }
        Location location = player.getLocation();
        location.setX(location.getBlockX() + 0.5);
        location.setZ(location.getBlockZ() + 0.5);
        location.setYaw(location.getYaw() + 180f);
        plugin.npcs().relocate(npc, location);
        plugin.msg(sender, "npc-updated", "id", npc.id);
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.msg(sender, "npc-usage");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (plugin.npcs().remove(id)) {
            plugin.msg(sender, "npc-removed", "id", id);
        } else {
            plugin.msg(sender, "npc-not-found", "id", id);
        }
    }

    private void list(CommandSender sender) {
        sender.sendMessage(plugin.messages().plain("npc-list-header"));
        for (Npc npc : plugin.npcs().all()) {
            sender.sendMessage(plugin.messages().plain("npc-list-line",
                    "id", npc.id,
                    "kind", npc.kind,
                    "action", npc.actionType + (npc.actionValue.isBlank() ? "" : " " + npc.actionValue)));
        }
    }

    private void hologram(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return;
        }
        if (args.length < 3) {
            plugin.msg(sender, "npc-usage");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!ID.matcher(id).matches()) {
            plugin.msg(sender, "npc-invalid-id");
            return;
        }
        Location location = player.getLocation();
        Npc npc = new Npc(id);
        npc.kind = "HOLOGRAM";
        npc.world = location.getWorld().getName();
        npc.x = location.getX();
        npc.y = location.getY() + 1.6;
        npc.z = location.getZ();
        npc.name = join(args, 2);
        plugin.npcs().add(npc);
        plugin.msg(sender, "npc-created", "id", id);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("monserveur.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return Players.filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        List<String> ids = new ArrayList<>();
        plugin.npcs().all().forEach(npc -> ids.add(npc.id));
        if (args.length == 2 && List.of("name", "subtitle", "action", "profession", "move", "remove").contains(sub)) {
            return Players.filter(ids, args[1]);
        }
        if (sub.equals("create")) {
            if (args.length == 3) {
                return Players.filter(PROFESSIONS, args[2]);
            }
            if (args.length == 4) {
                return Players.filter(TYPES, args[3]);
            }
            if (args.length == 5 && args[3].equalsIgnoreCase("MENU")) {
                return Players.filter(MENUS, args[4]);
            }
        }
        if (sub.equals("profession") && args.length == 3) {
            return Players.filter(PROFESSIONS, args[2]);
        }
        if (sub.equals("action")) {
            if (args.length == 3) {
                return Players.filter(TYPES, args[2]);
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("MENU")) {
                return Players.filter(MENUS, args[3]);
            }
        }
        return List.of();
    }
}

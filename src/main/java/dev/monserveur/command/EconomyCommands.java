package dev.monserveur.command;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.data.PlayerDataManager;
import dev.monserveur.util.Players;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /balance /pay /baltop /eco */
public final class EconomyCommands implements TabExecutor {

    private final MonServeurPlugin plugin;

    public EconomyCommands(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "balance" -> balance(sender, args);
            case "pay" -> pay(sender, args);
            case "baltop" -> baltop(sender);
            case "eco" -> eco(sender, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void balance(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.msg(sender, "player-only");
                return;
            }
            plugin.msg(player, "balance", "amount", plugin.economy().format(plugin.data().get(player).getBalance()));
            return;
        }
        OfflinePlayer target = Players.find(args[0]);
        if (target == null) {
            plugin.msg(sender, "player-not-found", "player", args[0]);
            return;
        }
        PlayerData data = plugin.data().getOrLoad(target.getUniqueId(), target.getName());
        plugin.msg(sender, "balance-other", "player", data.getName(), "amount", plugin.economy().format(data.getBalance()));
    }

    private void pay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            plugin.msg(sender, "usage", "usage", "/pay <joueur> <montant>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.msg(sender, "player-not-found", "player", args[0]);
            return;
        }
        if (target.equals(player)) {
            plugin.msg(sender, "pay-self");
            return;
        }
        Double amount = plugin.economy().parseAmount(args[1], false);
        if (amount == null) {
            plugin.msg(sender, "invalid-amount");
            return;
        }
        PlayerData from = plugin.data().get(player);
        PlayerData to = plugin.data().get(target);
        if (!plugin.economy().withdraw(from, amount)) {
            plugin.msg(sender, "not-enough-money", "amount", plugin.economy().format(amount));
            return;
        }
        plugin.economy().deposit(to, amount);
        plugin.msg(player, "pay-sent", "amount", plugin.economy().format(amount), "player", target.getName());
        plugin.msg(target, "pay-received", "amount", plugin.economy().format(amount), "player", player.getName());
    }

    private void baltop(CommandSender sender) {
        List<PlayerDataManager.BalanceEntry> top = plugin.data().allBalances();
        sender.sendMessage(plugin.messages().plain("baltop-header"));
        for (int i = 0; i < Math.min(10, top.size()); i++) {
            PlayerDataManager.BalanceEntry entry = top.get(i);
            sender.sendMessage(plugin.messages().plain("baltop-line",
                    "position", i + 1,
                    "player", entry.name(),
                    "amount", plugin.economy().format(entry.balance())));
        }
    }

    private void eco(CommandSender sender, String[] args) {
        if (!sender.hasPermission("monserveur.admin")) {
            plugin.msg(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            plugin.msg(sender, "usage", "usage", "/eco <give|take|set> <joueur> <montant>");
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (!List.of("give", "take", "set").contains(action)) {
            plugin.msg(sender, "usage", "usage", "/eco <give|take|set> <joueur> <montant>");
            return;
        }
        OfflinePlayer target = Players.find(args[1]);
        if (target == null) {
            plugin.msg(sender, "player-not-found", "player", args[1]);
            return;
        }
        Double amount = plugin.economy().parseAmount(args[2], action.equals("set"));
        if (amount == null) {
            plugin.msg(sender, "invalid-amount");
            return;
        }
        PlayerData data = plugin.data().getOrLoad(target.getUniqueId(), target.getName());
        switch (action) {
            case "give" -> plugin.economy().deposit(data, amount);
            case "take" -> data.setBalance(Math.max(0, data.getBalance() - amount));
            default -> data.setBalance(amount);
        }
        plugin.data().save(data);
        plugin.msg(sender, "eco-done", "player", data.getName(), "amount", plugin.economy().format(data.getBalance()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("eco")) {
            if (args.length == 1) {
                return Players.filter(List.of("give", "take", "set"), args[0]);
            }
            if (args.length == 2) {
                return Players.filter(Players.onlineNames(), args[1]);
            }
        } else if ((name.equals("pay") || name.equals("balance")) && args.length == 1) {
            return Players.filter(Players.onlineNames(), args[0]);
        }
        return List.of();
    }
}

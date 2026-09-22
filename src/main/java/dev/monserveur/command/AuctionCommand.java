package dev.monserveur.command;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.gui.AuctionHouseMenu;
import dev.monserveur.gui.MailboxMenu;
import dev.monserveur.gui.MyListingsMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /ah [my|collect|sell <prix>] */
public final class AuctionCommand implements TabExecutor {

    private final MonServeurPlugin plugin;

    public AuctionCommand(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return true;
        }
        if (args.length == 0) {
            new AuctionHouseMenu(plugin, player).open();
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "my" -> new MyListingsMenu(plugin, player).open();
            case "collect" -> plugin.auctions().collectAll(player);
            case "mailbox" -> new MailboxMenu(plugin, player).open();
            case "sell" -> {
                if (args.length < 2) {
                    plugin.msg(player, "usage", "usage", "/ah sell <prix>");
                    return true;
                }
                Double price = plugin.economy().parseAmount(args[1], false);
                if (price == null) {
                    plugin.msg(player, "invalid-amount");
                    return true;
                }
                plugin.auctions().list(player, price);
            }
            default -> new AuctionHouseMenu(plugin, player).open();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("my", "collect", "mailbox", "sell");
        }
        return List.of();
    }
}

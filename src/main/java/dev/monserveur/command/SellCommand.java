package dev.monserveur.command;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.shop.ShopManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;

/** /sell hand : vend l'objet en main. /sell all : vend tout l'inventaire vendable. */
public final class SellCommand implements TabExecutor {

    private final MonServeurPlugin plugin;

    public SellCommand(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return true;
        }
        if (args.length == 0) {
            plugin.msg(player, "usage", "usage", "/sell <hand|all>");
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "hand" -> sellHand(player);
            case "all" -> sellAll(player);
            default -> plugin.msg(player, "usage", "usage", "/sell <hand|all>");
        }
        return true;
    }

    private void sellHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            plugin.msg(player, "shop-no-items");
            return;
        }
        ShopManager.ShopItem item = plugin.shop().findSellable(hand.getType());
        if (item == null) {
            plugin.msg(player, "shop-not-sellable");
            return;
        }
        if (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
            plugin.msg(player, "sell-custom-item-blocked");
            return;
        }
        plugin.shop().sell(player, item, false);
    }

    private void sellAll(Player player) {
        PlayerInventory inventory = player.getInventory();
        double total = 0;
        int stacks = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null || stack.getType().isAir() || (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName())) {
                continue;
            }
            ShopManager.ShopItem item = plugin.shop().findSellable(stack.getType());
            if (item == null) {
                continue;
            }
            double gain = plugin.shop().sellPrice(player, item) * stack.getAmount();
            total += gain;
            stacks++;
            plugin.economy().deposit(plugin.data().get(player), gain);
            stack.setAmount(0);
        }
        if (stacks == 0) {
            plugin.msg(player, "sell-nothing");
            return;
        }
        plugin.msg(player, "sell-all-done", "amount", plugin.economy().format(total), "stacks", stacks);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("hand", "all");
        }
        return List.of();
    }
}

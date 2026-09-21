package dev.monserveur.shop;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.rank.Rank;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShopManager {

    public record ShopItem(Material material, double buy, double sell) {
    }

    public record Category(String id, String name, Material icon, List<ShopItem> items) {
    }

    private final MonServeurPlugin plugin;
    private final Map<String, Category> categories = new LinkedHashMap<>();

    public ShopManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        categories.clear();
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("shop.categories");
        if (cats == null) {
            return;
        }
        for (String id : cats.getKeys(false)) {
            ConfigurationSection c = cats.getConfigurationSection(id);
            if (c == null) {
                continue;
            }
            Material icon = Material.matchMaterial(c.getString("icon", "CHEST"));
            if (icon == null) {
                icon = Material.CHEST;
            }
            List<ShopItem> items = new ArrayList<>();
            ConfigurationSection section = c.getConfigurationSection("items");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    Material material = Material.matchMaterial(key);
                    if (material == null || !material.isItem()) {
                        plugin.getLogger().warning("Boutique : matériau inconnu '" + key + "' (catégorie " + id + ")");
                        continue;
                    }
                    items.add(new ShopItem(material, section.getDouble(key + ".buy", 0), section.getDouble(key + ".sell", 0)));
                }
            }
            if (items.size() > 45) {
                plugin.getLogger().warning("Boutique : la catégorie '" + id + "' dépasse 45 articles, les derniers sont ignorés.");
                items = new ArrayList<>(items.subList(0, 45));
            }
            categories.put(id, new Category(id, c.getString("name", id), icon, items));
        }
    }

    public Collection<Category> categories() {
        return categories.values();
    }

    /** Prix de vente réel pour ce joueur (bonus de grade inclus). */
    public double sellPrice(Player player, ShopItem item) {
        Rank rank = plugin.ranks().of(player);
        return Math.round(item.sell() * rank.sellMultiplier() * 100.0) / 100.0;
    }

    public void buy(Player player, ShopItem item, int amount) {
        if (item.buy() <= 0) {
            plugin.msg(player, "shop-not-buyable");
            return;
        }
        PlayerData data = plugin.data().get(player);
        double cost = item.buy() * amount;
        if (!plugin.economy().withdraw(data, cost)) {
            plugin.msg(player, "not-enough-money", "amount", plugin.economy().format(cost));
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(item.material(), amount));
        int notGiven = 0;
        for (ItemStack stack : leftover.values()) {
            notGiven += stack.getAmount();
        }
        if (notGiven > 0) {
            plugin.economy().deposit(data, item.buy() * notGiven); // remboursement
        }
        int given = amount - notGiven;
        if (given <= 0) {
            plugin.msg(player, "inventory-full");
            return;
        }
        plugin.msg(player, "shop-bought",
                "amount", given,
                "item", Component.translatable(item.material().translationKey()),
                "price", plugin.economy().format(item.buy() * given));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
    }

    public void sell(Player player, ShopItem item, boolean all) {
        if (item.sell() <= 0) {
            plugin.msg(player, "shop-not-sellable");
            return;
        }
        PlayerInventory inventory = player.getInventory();
        int have = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.getType() == item.material() && !stack.hasItemMeta()) {
                have += stack.getAmount();
            }
        }
        if (have <= 0) {
            plugin.msg(player, "shop-no-items");
            return;
        }
        int amount = all ? have : 1;
        int remaining = amount;
        while (remaining > 0) {
            int chunk = Math.min(64, remaining);
            inventory.removeItem(new ItemStack(item.material(), chunk));
            remaining -= chunk;
        }
        double gain = sellPrice(player, item) * amount;
        plugin.economy().deposit(plugin.data().get(player), gain);
        plugin.msg(player, "shop-sold",
                "amount", amount,
                "item", Component.translatable(item.material().translationKey()),
                "price", plugin.economy().format(gain));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 0.8f);
    }
}

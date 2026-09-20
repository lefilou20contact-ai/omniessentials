package dev.monserveur.gui;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.shop.ShopManager;
import dev.monserveur.util.ItemBuilder;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

/** Articles d'une catégorie : clic gauche = acheter, clic droit = vendre (Maj = x64 / tout). */
public final class ShopCategoryMenu extends Menu {

    private final ShopManager.Category category;

    public ShopCategoryMenu(MonServeurPlugin plugin, Player player, ShopManager.Category category) {
        super(plugin, player);
        this.category = category;
    }

    @Override
    protected Component title() {
        return Text.mm("<dark_gray>Boutique » ").append(Text.mm(category.name()));
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        List<ShopManager.ShopItem> items = category.items();
        for (int i = 0; i < items.size() && i < 45; i++) {
            ShopManager.ShopItem item = items.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(item.buy() > 0
                    ? "<gray>Achat : <green>" + plugin.economy().format(item.buy()) + " <dark_gray>(clic gauche)"
                    : "<dark_gray>Non achetable");
            lore.add(item.sell() > 0
                    ? "<gray>Vente : <gold>" + plugin.economy().format(plugin.shop().sellPrice(player, item)) + " <dark_gray>(clic droit)"
                    : "<dark_gray>Non vendable");
            lore.add("");
            lore.add("<dark_gray>Maj + clic : x64 / tout vendre");

            set(i, new ItemBuilder(item.material()).lore(lore).hideFlags().build(), event -> {
                ClickType click = event.getClick();
                switch (click) {
                    case LEFT -> plugin.shop().buy(player, item, 1);
                    case SHIFT_LEFT -> plugin.shop().buy(player, item, 64);
                    case RIGHT -> plugin.shop().sell(player, item, false);
                    case SHIFT_RIGHT -> plugin.shop().sell(player, item, true);
                    default -> {
                    }
                }
                refresh();
            });
        }
        back(45, () -> new ShopMenu(plugin, player).open());
        set(49, new ItemBuilder(Material.GOLD_INGOT)
                .name("<gold>Ton solde")
                .lore("<yellow>" + plugin.economy().format(plugin.data().get(player).getBalance()))
                .build());
    }
}

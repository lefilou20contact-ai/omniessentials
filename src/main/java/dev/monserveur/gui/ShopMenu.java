package dev.monserveur.gui;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.shop.ShopManager;
import dev.monserveur.util.ItemBuilder;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** Liste des catégories de la boutique. */
public final class ShopMenu extends Menu {

    public ShopMenu(MonServeurPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Text.mm("<dark_gray>Boutique");
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void build() {
        List<ShopManager.Category> categories = List.copyOf(plugin.shop().categories());
        for (int i = 0; i < categories.size() && i < GRID.length; i++) {
            ShopManager.Category category = categories.get(i);
            set(GRID[i], new ItemBuilder(category.icon())
                            .name(category.name())
                            .lore("<gray>" + category.items().size() + " articles", "", "<yellow>Clique pour ouvrir")
                            .hideFlags()
                            .build(),
                    event -> new ShopCategoryMenu(plugin, player, category).open());
        }
        set(44, new ItemBuilder(Material.GOLD_INGOT)
                .name("<gold>Ton solde")
                .lore("<yellow>" + plugin.economy().format(plugin.data().get(player).getBalance()))
                .build());
        back(40, () -> new MainMenu(plugin, player).open());
    }
}

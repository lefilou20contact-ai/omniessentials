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
        set(36, new ItemBuilder(Material.HOPPER)
                        .name("<green>Vente rapide")
                        .lore("<gray>Vend l'objet en main ou", "<gray>tout ton inventaire vendable.", "",
                                "<yellow>/sell hand <dark_gray>ou <yellow>/sell all")
                        .build(),
                event -> player.closeInventory());
        set(42, new ItemBuilder(Material.CHEST_MINECART)
                        .name("<aqua>Hôtel des ventes")
                        .lore("<gray>Achète et vends entre joueurs.", "", "<yellow>Clique pour ouvrir")
                        .build(),
                event -> new dev.monserveur.gui.AuctionHouseMenu(plugin, player).open());
        back(40, () -> new MainMenu(plugin, player).open());
    }
}

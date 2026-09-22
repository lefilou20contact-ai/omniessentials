package dev.monserveur.gui;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.auction.AuctionListing;
import dev.monserveur.util.ItemBuilder;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Parcourt les annonces actives, 21 par page. Clic = acheter. */
public final class AuctionHouseMenu extends Menu {

    private static final int PER_PAGE = 21;
    private int page;

    public AuctionHouseMenu(MonServeurPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public AuctionHouseMenu(MonServeurPlugin plugin, Player player, int page) {
        super(plugin, player);
        this.page = page;
    }

    @Override
    protected Component title() {
        return Text.mm("<dark_gray>Hôtel des ventes");
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        List<AuctionListing> all = plugin.auctions().active();
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        List<AuctionListing> shown = all.subList(page * PER_PAGE, Math.min(all.size(), (page + 1) * PER_PAGE));

        for (int i = 0; i < shown.size() && i < GRID.length; i++) {
            AuctionListing listing = shown.get(i);
            ItemStack display = listing.item.clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("<gray>Vendeur : <white>" + listing.sellerName);
            lore.add("<gray>Prix : <gold>" + plugin.economy().format(listing.price));
            lore.add("");
            lore.add(listing.seller.equals(player.getUniqueId())
                    ? "<yellow>C'est ton annonce (voir /ah my)"
                    : "<green>Clique pour acheter");
            meta.lore(mergeLore(meta, lore));
            display.setItemMeta(meta);

            set(GRID[i], display, event -> {
                plugin.auctions().buy(player, listing.id);
                refresh();
            });
        }

        if (page > 0) {
            set(45, new ItemBuilder(Material.ARROW).name("<yellow>« Page précédente").build(),
                    event -> new AuctionHouseMenu(plugin, player, page - 1).open());
        }
        if (page < pages - 1) {
            set(53, new ItemBuilder(Material.ARROW).name("<yellow>Page suivante »").build(),
                    event -> new AuctionHouseMenu(plugin, player, page + 1).open());
        }
        set(47, new ItemBuilder(Material.EMERALD).name("<green>Vendre un objet")
                        .lore("<gray>Tiens l'objet en main puis clique.", "<gray>Un prix te sera demandé dans le chat.")
                        .build(),
                event -> {
                    player.closeInventory();
                    plugin.auctions().startSellPrompt(player);
                });
        set(49, new ItemBuilder(Material.CHEST).name("<gold>Mes annonces").build(),
                event -> new MyListingsMenu(plugin, player).open());
        set(51, new ItemBuilder(Material.ENDER_CHEST).name("<aqua>Ma boîte de réception")
                        .lore(plugin.auctions().mailboxOf(player).isEmpty() ? "<gray>Vide" : "<yellow>" + plugin.auctions().mailboxOf(player).size() + " objet(s) à récupérer")
                        .build(),
                event -> new MailboxMenu(plugin, player).open());
        back(48, () -> new MainMenu(plugin, player).open());
    }

    private List<Component> mergeLore(ItemMeta meta, List<String> extra) {
        List<Component> lore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) {
            lore.addAll(meta.lore());
        }
        for (String line : extra) {
            lore.add(Text.item(line));
        }
        return lore;
    }
}

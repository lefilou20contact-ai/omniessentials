package dev.monserveur.gui;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.auction.AuctionListing;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class MyListingsMenu extends Menu {

    public MyListingsMenu(MonServeurPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Text.mm("<dark_gray>Mes annonces");
    }

    @Override
    protected int rows() {
        return 4;
    }

    @Override
    protected void build() {
        List<AuctionListing> mine = plugin.auctions().byPlayer(player.getUniqueId());
        for (int i = 0; i < mine.size() && i < GRID.length; i++) {
            AuctionListing listing = mine.get(i);
            ItemStack display = listing.item.clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore() && meta.lore() != null) {
                lore.addAll(meta.lore());
            }
            lore.add(Text.item(""));
            lore.add(Text.item("<gray>Prix : <gold>" + plugin.economy().format(listing.price)));
            lore.add(Text.item("<red>Clique pour retirer l'annonce"));
            meta.lore(lore);
            display.setItemMeta(meta);

            set(GRID[i], display, event -> {
                plugin.auctions().cancel(player, listing.id);
                refresh();
            });
        }
        back(31, () -> new AuctionHouseMenu(plugin, player).open());
    }
}

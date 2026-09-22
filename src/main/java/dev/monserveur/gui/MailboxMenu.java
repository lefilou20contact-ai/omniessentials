package dev.monserveur.gui;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.util.ItemBuilder;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Objets en attente : achats livrés hors-ligne, annonces expirées ou annulées. */
public final class MailboxMenu extends Menu {

    public MailboxMenu(MonServeurPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Text.mm("<dark_gray>Boîte de réception");
    }

    @Override
    protected int rows() {
        return 4;
    }

    @Override
    protected void build() {
        List<ItemStack> items = plugin.auctions().mailboxOf(player);
        for (int i = 0; i < items.size() && i < GRID.length; i++) {
            int index = i;
            ItemStack display = items.get(i).clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore() && meta.lore() != null) {
                lore.addAll(meta.lore());
            }
            lore.add(Text.item(""));
            lore.add(Text.item("<green>Clique pour récupérer"));
            meta.lore(lore);
            display.setItemMeta(meta);

            set(GRID[i], display, event -> {
                plugin.auctions().collectOne(player, index);
                refresh();
            });
        }
        if (items.isEmpty()) {
            set(22, new ItemBuilder(Material.BARRIER).name("<gray>Ta boîte est vide").build());
        } else {
            set(31, new ItemBuilder(Material.HOPPER).name("<green>Tout récupérer").build(),
                    event -> {
                        plugin.auctions().collectAll(player);
                        refresh();
                    });
        }
        back(27, () -> new AuctionHouseMenu(plugin, player).open());
    }
}

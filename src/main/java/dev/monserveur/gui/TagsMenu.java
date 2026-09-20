package dev.monserveur.gui;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.rank.CosmeticTag;
import dev.monserveur.util.ItemBuilder;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class TagsMenu extends Menu {

    public TagsMenu(MonServeurPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Text.mm("<dark_gray>Tags");
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void build() {
        PlayerData data = plugin.data().get(player);
        List<CosmeticTag> tags = List.copyOf(plugin.tags().all());

        for (int i = 0; i < tags.size() && i < GRID.length; i++) {
            CosmeticTag tag = tags.get(i);
            boolean access = plugin.tags().hasAccess(player, tag);
            boolean owned = plugin.tags().owns(data, tag);
            boolean active = tag.id().equals(data.getActiveTag());

            List<String> lore = new ArrayList<>();
            lore.add("<gray>Aperçu : " + player.getName() + " " + tag.display());
            lore.add("");
            if (active) {
                lore.add("<green>✔ Équipé");
            } else if (!access) {
                lore.add("<red>✖ Réservé");
            } else if (owned) {
                lore.add("<yellow>Clique pour équiper");
            } else {
                lore.add("<gray>Prix : <gold>" + plugin.economy().format(tag.price()));
                lore.add("<yellow>Clique pour acheter et équiper");
            }

            ItemBuilder builder = new ItemBuilder(Material.NAME_TAG).name("<white>Tag " + tag.display()).lore(lore);
            if (active) {
                builder.glow();
            }
            set(GRID[i], builder.build(), event -> {
                plugin.tags().equip(player, tag);
                refresh();
            });
        }

        set(40, new ItemBuilder(Material.BARRIER).name("<red>Retirer mon tag").build(), event -> {
            plugin.tags().unequip(player);
            refresh();
        });
        set(44, new ItemBuilder(Material.GOLD_INGOT)
                .name("<gold>Ton solde")
                .lore("<yellow>" + plugin.economy().format(data.getBalance()))
                .build());
        back(36, () -> new MainMenu(plugin, player).open());
    }
}

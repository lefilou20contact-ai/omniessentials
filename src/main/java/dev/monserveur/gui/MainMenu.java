package dev.monserveur.gui;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.rank.Rank;
import dev.monserveur.util.ItemBuilder;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class MainMenu extends Menu {

    public MainMenu(MonServeurPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Text.mm("<dark_gray>Menu principal");
    }

    @Override
    protected int rows() {
        return 3;
    }

    @Override
    protected void build() {
        PlayerData data = plugin.data().get(player);
        Rank rank = plugin.ranks().of(player);
        long needed = plugin.levels().xpNeeded(data.getLevel());

        set(4, new ItemBuilder(Material.PLAYER_HEAD).skull(player)
                .name("<yellow>" + player.getName())
                .lore("<gray>Grade : <white>" + rank.display(),
                        "<gray>Niveau : <green>" + data.getLevel(),
                        "<gray>XP : <yellow>" + data.getXp() + "<gray>/<yellow>" + needed,
                        "<gray>Argent : <gold>" + plugin.economy().format(data.getBalance()))
                .build());

        set(10, new ItemBuilder(Material.EMERALD).name("<green>Boutique")
                        .lore("<gray>Achète et vends des objets.", "", "<yellow>Clique pour ouvrir").build(),
                event -> new ShopMenu(plugin, player).open());

        set(12, new ItemBuilder(Material.RED_BED).name("<red>Homes")
                        .lore("<gray>Tes points de téléportation.", "", "<yellow>Clique pour ouvrir").build(),
                event -> new HomesMenu(plugin, player).open());

        set(14, new ItemBuilder(Material.WRITABLE_BOOK).name("<gold>Quêtes du jour")
                        .lore("<gray>Des récompenses chaque jour !", "", "<yellow>Clique pour ouvrir").build(),
                event -> new QuestsMenu(plugin, player).open());

        set(16, new ItemBuilder(Material.NAME_TAG).name("<aqua>Tags")
                        .lore("<gray>Personnalise ton pseudo dans le chat.", "", "<yellow>Clique pour ouvrir").build(),
                event -> new TagsMenu(plugin, player).open());

        set(22, new ItemBuilder(Material.COMPASS).name("<light_purple>Spawn")
                        .lore("<gray>Retourne au spawn.", "", "<yellow>Clique pour t'y téléporter").build(),
                event -> {
                    player.closeInventory();
                    plugin.teleports().teleport(player, plugin.spawn().get(), "spawn");
                });
    }
}

package dev.monserveur.gui;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.util.ItemBuilder;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class HomesMenu extends Menu {

    public HomesMenu(MonServeurPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Text.mm("<dark_gray>Mes homes");
    }

    @Override
    protected int rows() {
        return 4;
    }

    @Override
    protected void build() {
        PlayerData data = plugin.data().get(player);
        List<Map.Entry<String, Location>> homes = new ArrayList<>(data.getHomes().entrySet());

        for (int i = 0; i < homes.size() && i < 14; i++) {
            String name = homes.get(i).getKey();
            Location location = homes.get(i).getValue();
            String world = location.getWorld() == null ? "?" : location.getWorld().getName();

            set(GRID[i], new ItemBuilder(Material.RED_BED)
                            .name("<yellow>" + name)
                            .lore("<gray>Monde : <white>" + world,
                                    "<gray>Position : <white>" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(),
                                    "",
                                    "<yellow>Clic gauche : téléportation",
                                    "<red>Maj + clic droit : supprimer")
                            .build(),
                    event -> {
                        if (event.getClick() == ClickType.SHIFT_RIGHT) {
                            plugin.homes().delete(player, name);
                            refresh();
                        } else if (event.getClick() == ClickType.LEFT) {
                            player.closeInventory();
                            plugin.homes().go(player, name);
                        }
                    });
        }

        set(31, new ItemBuilder(Material.LIME_BED)
                        .name("<green>Définir un home ici")
                        .lore("<gray>Homes : <white>" + data.getHomes().size() + "<gray>/<white>" + plugin.homes().maxHomes(player),
                                "",
                                "<yellow>Clique pour créer")
                        .build(),
                event -> {
                    plugin.homes().set(player, plugin.homes().nextFreeName(player));
                    refresh();
                });
        back(27, () -> new MainMenu(plugin, player).open());
    }
}

package dev.monserveur.listener;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.gui.MainMenu;
import dev.monserveur.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** L'étoile du menu : donnée à la connexion, clic droit = ouvre le menu principal. */
public final class MenuItemListener implements Listener {

    private final MonServeurPlugin plugin;
    private final NamespacedKey key;

    public MenuItemListener(MonServeurPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "menu_item");
    }

    public boolean isMenuItem(ItemStack item) {
        return item != null
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public void give(Player player) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("menu-item.enabled", true)) {
            return;
        }
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isMenuItem(stack)) {
                return; // déjà en possession
            }
        }
        Material material = Material.matchMaterial(config.getString("menu-item.material", "NETHER_STAR"));
        if (material == null) {
            material = Material.NETHER_STAR;
        }
        ItemStack item = new ItemBuilder(material)
                .name(config.getString("menu-item.name", "<gold>Menu"))
                .lore("<gray>Clic droit pour ouvrir le menu")
                .glow()
                .tag(key, "1")
                .build();
        int slot = config.getInt("menu-item.slot", 8);
        if (player.getInventory().getItem(slot) == null) {
            player.getInventory().setItem(slot, item);
        } else {
            player.getInventory().addItem(item);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isMenuItem(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        new MainMenu(plugin, event.getPlayer()).open();
    }
}

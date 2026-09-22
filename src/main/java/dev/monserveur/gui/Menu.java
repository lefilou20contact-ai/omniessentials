package dev.monserveur.gui;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Base de tous les menus : un inventaire + des actions associées aux slots. */
public abstract class Menu implements InventoryHolder {

    /** Emplacements "grille" : 3 rangées de 7 cases au centre d'un menu. */
    protected static final int[] GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    protected final MonServeurPlugin plugin;
    protected final Player player;
    private Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(MonServeurPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected abstract Component title();

    protected abstract int rows();

    /** Remplit le menu (appelé à l'ouverture et à chaque refresh()). */
    protected abstract void build();

    public void open() {
        inventory = Bukkit.createInventory(this, rows() * 9, title());
        render();
        player.openInventory(inventory);
    }

    /** Reconstruit le contenu sans fermer le menu. */
    protected void refresh() {
        render();
    }

    private void render() {
        actions.clear();
        inventory.clear();
        build();
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    protected void set(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        actions.put(slot, action);
    }

    protected void back(int slot, Runnable action) {
        set(slot, new ItemBuilder(Material.ARROW).name("<yellow>← Retour").build(), event -> action.run());
    }

    public void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            action.accept(event);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

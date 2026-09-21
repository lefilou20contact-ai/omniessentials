package dev.monserveur.listener;

import dev.monserveur.MonServeurPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/** Protections du mode lobby (config : lobby.enabled). */
public final class LobbyListener implements Listener {

    private final MonServeurPlugin plugin;

    public LobbyListener(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean lobby() {
        return plugin.getConfig().getBoolean("lobby.enabled", false);
    }

    private boolean restricted(Player player) {
        return lobby() && !player.hasPermission("monserveur.lobby.bypass");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (restricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (restricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (restricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!lobby() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            player.teleportAsync(plugin.spawn().get()); // tombé dans le vide : retour au spawn
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (lobby()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && restricted(player)
                && plugin.getConfig().getBoolean("lobby.lock-inventory", true)) {
            event.setCancelled(true);
        }
    }
}

package dev.monserveur.teleport;

import dev.monserveur.MonServeurPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Téléportation avec délai, annulée si le joueur bouge ou prend des dégâts. */
public final class TeleportManager implements Listener {

    private final MonServeurPlugin plugin;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();

    public TeleportManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    public void teleport(Player player, Location destination, String label) {
        int warmup = plugin.getConfig().getInt("teleport.warmup-seconds", 3);
        Location target = destination.clone();
        if (warmup <= 0 || player.hasPermission("monserveur.teleport.bypass")) {
            player.teleportAsync(target);
            plugin.msg(player, "teleport-done", "target", label);
            return;
        }
        cancel(player, false);
        plugin.msg(player, "teleport-warmup", "seconds", warmup);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending.remove(player.getUniqueId());
            if (player.isOnline()) {
                player.teleportAsync(target);
                plugin.msg(player, "teleport-done", "target", label);
            }
        }, warmup * 20L);
        pending.put(player.getUniqueId(), task);
    }

    public void cancel(Player player, boolean notify) {
        BukkitTask task = pending.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            if (notify) {
                plugin.msg(player, "teleport-cancelled");
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (pending.containsKey(event.getPlayer().getUniqueId()) && event.hasChangedBlock()) {
            cancel(event.getPlayer(), true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && pending.containsKey(player.getUniqueId())) {
            cancel(player, true);
        }
    }
}

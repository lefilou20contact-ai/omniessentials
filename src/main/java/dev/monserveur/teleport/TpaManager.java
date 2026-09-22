package dev.monserveur.teleport;

import dev.monserveur.MonServeurPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Demandes de téléportation entre joueurs (/tpa, /tpahere) et /back
 * (retour au dernier endroit avant une téléportation ou une mort).
 */
public final class TpaManager implements Listener {

    private enum Type { TO, HERE }

    private record Request(UUID from, Type type, BukkitTask expiry) {
    }

    private final MonServeurPlugin plugin;
    private final Map<UUID, Request> pending = new HashMap<>(); // cible -> demande reçue
    private final Map<UUID, Location> lastLocation = new HashMap<>();

    public TpaManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    /** À appeler avant toute téléportation (home, spawn, tpa...) pour permettre /back. */
    public void rememberLocation(Player player) {
        lastLocation.put(player.getUniqueId(), player.getLocation().clone());
    }

    public void back(Player player) {
        Location location = lastLocation.get(player.getUniqueId());
        if (location == null || location.getWorld() == null) {
            plugin.msg(player, "back-none");
            return;
        }
        Location current = player.getLocation().clone();
        plugin.teleports().teleport(player, location, "back");
        lastLocation.put(player.getUniqueId(), current);
    }

    private int timeoutSeconds() {
        return Math.max(10, plugin.getConfig().getInt("teleport.request-timeout-seconds", 60));
    }

    private void expire(Player target, Player requester, boolean announce) {
        Request removed = pending.remove(target.getUniqueId());
        if (removed != null && announce && requester != null && requester.isOnline()) {
            plugin.msg(requester, "tpa-expired", "player", target.getName());
        }
    }

    public void request(Player from, Player target, boolean here) {
        if (from.equals(target)) {
            plugin.msg(from, "tpa-self");
            return;
        }
        if (pending.containsKey(target.getUniqueId())) {
            plugin.msg(from, "tpa-already-pending", "player", target.getName());
            return;
        }
        int timeout = timeoutSeconds();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin,
                () -> expire(target, from, true), timeout * 20L);
        pending.put(target.getUniqueId(), new Request(from.getUniqueId(), here ? Type.HERE : Type.TO, task));

        plugin.msg(from, "tpa-sent", "player", target.getName());
        if (here) {
            plugin.msg(target, "tpa-received-here", "player", from.getName(), "seconds", timeout);
        } else {
            plugin.msg(target, "tpa-received-to", "player", from.getName(), "seconds", timeout);
        }
    }

    public void cancel(Player from) {
        boolean found = pending.entrySet().removeIf(entry -> {
            if (entry.getValue().from().equals(from.getUniqueId())) {
                entry.getValue().expiry().cancel();
                Player target = Bukkit.getPlayer(entry.getKey());
                if (target != null) {
                    plugin.msg(target, "tpa-cancelled-by-sender", "player", from.getName());
                }
                return true;
            }
            return false;
        });
        plugin.msg(from, found ? "tpa-cancelled" : "tpa-nothing-to-cancel");
    }

    public void accept(Player target) {
        Request request = pending.remove(target.getUniqueId());
        if (request == null) {
            plugin.msg(target, "tpa-none-pending");
            return;
        }
        request.expiry().cancel();
        Player requester = Bukkit.getPlayer(request.from());
        if (requester == null) {
            plugin.msg(target, "tpa-player-left");
            return;
        }
        if (request.type() == Type.HERE) {
            rememberLocation(target);
            plugin.teleports().teleport(target, requester.getLocation(), requester.getName());
        } else {
            rememberLocation(requester);
            plugin.teleports().teleport(requester, target.getLocation(), target.getName());
        }
        plugin.msg(target, "tpa-accepted", "player", requester.getName());
        plugin.msg(requester, "tpa-accepted-by", "player", target.getName());
    }

    public void deny(Player target) {
        Request request = pending.remove(target.getUniqueId());
        if (request == null) {
            plugin.msg(target, "tpa-none-pending");
            return;
        }
        request.expiry().cancel();
        plugin.msg(target, "tpa-denied");
        Player requester = Bukkit.getPlayer(request.from());
        if (requester != null) {
            plugin.msg(requester, "tpa-denied-by", "player", target.getName());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        rememberLocation(event.getEntity());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Request request = pending.remove(id);
        if (request != null) {
            request.expiry().cancel();
        }
        pending.entrySet().removeIf(entry -> {
            if (entry.getValue().from().equals(id)) {
                entry.getValue().expiry().cancel();
                return true;
            }
            return false;
        });
    }
}

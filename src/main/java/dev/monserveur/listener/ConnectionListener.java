package dev.monserveur.listener;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ConnectionListener implements Listener {

    private final MonServeurPlugin plugin;

    public ConnectionListener(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.data().get(player);
        data.setName(player.getName());
        plugin.quests().checkReset(data);

        plugin.tab().apply(player);
        plugin.tab().updateHeaderFooter(player);
        plugin.menuItem().give(player);

        String join = plugin.getConfig().getString("join.join-message", "");
        event.joinMessage(join.isBlank() ? null : Text.mm(join, Placeholder.unparsed("player", player.getName())));

        if (!player.hasPlayedBefore() && plugin.getConfig().getBoolean("join.first-join-broadcast", true)) {
            Bukkit.broadcast(plugin.messages().get("first-join", "player", player.getName()));
        }
        if (plugin.getConfig().getBoolean("spawn.teleport-on-join", false)) {
            player.teleportAsync(plugin.spawn().get());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.teleports().cancel(player, false);
        plugin.tab().remove(player);
        plugin.data().save(plugin.data().get(player));
        plugin.data().unload(player.getUniqueId());

        String quit = plugin.getConfig().getString("join.quit-message", "");
        event.quitMessage(quit.isBlank() ? null : Text.mm(quit, Placeholder.unparsed("player", player.getName())));
    }
}

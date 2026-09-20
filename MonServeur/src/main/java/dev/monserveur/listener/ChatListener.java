package dev.monserveur.listener;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.rank.Rank;
import dev.monserveur.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Format du chat : niveau, grade, pseudo, tag, message. */
public final class ChatListener implements Listener {

    private final MonServeurPlugin plugin;

    public ChatListener(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.data().get(player);
        Rank rank = plugin.ranks().of(player);

        String format = plugin.getConfig().getString("chat.format",
                "<level><prefix><namecolor><name><tag><dark_gray> » <chatcolor><message>");
        String level = "";
        if (plugin.getConfig().getBoolean("chat.show-level", true)) {
            level = plugin.getConfig().getString("chat.level-format", "[{level}] ")
                    .replace("{level}", String.valueOf(data.getLevel()));
        }
        String tag = plugin.tags().chatDisplay(data.getActiveTag());
        String levelText = level;

        event.renderer((source, sourceDisplayName, message, viewer) -> Text.mm(format,
                Placeholder.parsed("level", levelText),
                Placeholder.parsed("prefix", rank.prefix()),
                Placeholder.parsed("namecolor", rank.nameColor()),
                Placeholder.unparsed("name", source.getName()),
                Placeholder.parsed("tag", tag),
                Placeholder.parsed("chatcolor", rank.chatColor()),
                Placeholder.component("message", message)));
    }
}

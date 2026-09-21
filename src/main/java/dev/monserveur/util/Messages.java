package dev.monserveur.util;

import dev.monserveur.MonServeurPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Messages configurables (section "messages" de config.yml).
 * Les placeholders se passent par paires : nom, valeur. Ex :
 * send(player, "balance", "amount", "12 $")  ->  remplace &lt;amount&gt;
 */
public final class Messages {

    private final MonServeurPlugin plugin;

    public Messages(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    /** Message avec le préfixe du serveur. */
    public Component get(String key, Object... placeholders) {
        return build(true, key, placeholders);
    }

    /** Message sans préfixe (titres, listes...). */
    public Component plain(String key, Object... placeholders) {
        return build(false, key, placeholders);
    }

    public void send(CommandSender to, String key, Object... placeholders) {
        to.sendMessage(get(key, placeholders));
    }

    private Component build(boolean withPrefix, String key, Object[] kv) {
        String raw = plugin.getConfig().getString("messages." + key);
        if (raw == null) {
            raw = "<red>Message manquant dans config.yml : messages." + key;
        } else if (withPrefix) {
            raw = plugin.getConfig().getString("messages.prefix", "") + raw;
        }
        List<TagResolver> resolvers = new ArrayList<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String name = String.valueOf(kv[i]);
            Object value = kv[i + 1];
            if (value instanceof ComponentLike component) {
                resolvers.add(Placeholder.component(name, component));
            } else {
                resolvers.add(Placeholder.unparsed(name, String.valueOf(value)));
            }
        }
        return Text.mm(raw, resolvers.toArray(new TagResolver[0]));
    }
}

package dev.monserveur.rank;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class TagManager {

    private final MonServeurPlugin plugin;
    private final Map<String, CosmeticTag> tags = new LinkedHashMap<>();

    public TagManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        tags.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("tags");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection t = section.getConfigurationSection(key);
            if (t == null) {
                continue;
            }
            String id = key.toLowerCase(Locale.ROOT);
            tags.put(id, new CosmeticTag(id, t.getString("display", key), t.getDouble("price", 0), t.getString("permission")));
        }
    }

    public Collection<CosmeticTag> all() {
        return tags.values();
    }

    public CosmeticTag get(String id) {
        return id == null ? null : tags.get(id.toLowerCase(Locale.ROOT));
    }

    /** Texte MiniMessage à insérer dans le chat (avec un espace devant), ou "" si aucun tag. */
    public String chatDisplay(String id) {
        CosmeticTag tag = get(id);
        return tag == null ? "" : " " + tag.display();
    }

    public boolean hasAccess(Player player, CosmeticTag tag) {
        return tag.permission() == null || tag.permission().isBlank() || player.hasPermission(tag.permission());
    }

    public boolean owns(PlayerData data, CosmeticTag tag) {
        return tag.price() <= 0 || data.getOwnedTags().contains(tag.id());
    }

    /** Équipe le tag, en l'achetant au besoin. */
    public void equip(Player player, CosmeticTag tag) {
        PlayerData data = plugin.data().get(player);
        if (!hasAccess(player, tag)) {
            plugin.msg(player, "tag-locked");
            return;
        }
        if (!owns(data, tag)) {
            if (!plugin.economy().withdraw(data, tag.price())) {
                plugin.msg(player, "not-enough-money", "amount", plugin.economy().format(tag.price()));
                return;
            }
            data.getOwnedTags().add(tag.id());
            plugin.msg(player, "tag-bought", "price", plugin.economy().format(tag.price()), "tag", Text.mm(tag.display()));
        }
        data.setActiveTag(tag.id());
        plugin.msg(player, "tag-equipped", "tag", Text.mm(tag.display()));
    }

    public void unequip(Player player) {
        plugin.data().get(player).setActiveTag(null);
        plugin.msg(player, "tag-removed");
    }
}

package dev.monserveur.rank;

import dev.monserveur.MonServeurPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class RankManager {

    private final MonServeurPlugin plugin;
    private final Map<String, Rank> ranks = new LinkedHashMap<>();
    private String defaultRank = "joueur";

    public RankManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        ranks.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ranks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection r = section.getConfigurationSection(key);
                if (r == null) {
                    continue;
                }
                String id = key.toLowerCase(Locale.ROOT);
                ranks.put(id, new Rank(
                        id,
                        r.getString("display", key),
                        r.getString("prefix", ""),
                        r.getString("name-color", "<white>"),
                        r.getString("chat-color", "<white>"),
                        r.getString("team-color", "white"),
                        r.getInt("weight", 1),
                        r.getInt("max-homes", 1),
                        r.getDouble("sell-multiplier", 1.0)));
            }
        }
        defaultRank = plugin.getConfig().getString("default-rank", "joueur").toLowerCase(Locale.ROOT);
        if (!ranks.containsKey(defaultRank)) {
            plugin.getLogger().warning("Le grade par défaut '" + defaultRank + "' n'existe pas dans config.yml, un grade de secours est créé.");
            ranks.put(defaultRank, new Rank(defaultRank, "Joueur", "", "<white>", "<white>", "white", 1, 1, 1.0));
        }
    }

    public Rank get(String id) {
        if (id != null) {
            Rank rank = ranks.get(id.toLowerCase(Locale.ROOT));
            if (rank != null) {
                return rank;
            }
        }
        return ranks.get(defaultRank);
    }

    public Rank of(Player player) {
        return get(plugin.data().get(player).getRank());
    }

    public boolean exists(String id) {
        return ranks.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Rank> all() {
        return ranks.values();
    }
}

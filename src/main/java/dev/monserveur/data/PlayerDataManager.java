package dev.monserveur.data;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.util.Locs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Chargement / sauvegarde des données joueurs (un fichier YAML par joueur dans plugins/MonServeur/players). */
public final class PlayerDataManager {

    public record BalanceEntry(String name, double balance) {
    }

    private final MonServeurPlugin plugin;
    private final File folder;
    // ConcurrentHashMap : le chat est asynchrone et lit ces données
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "players");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Impossible de créer le dossier " + folder.getPath());
        }
    }

    public PlayerData get(Player player) {
        return getOrLoad(player.getUniqueId(), player.getName());
    }

    public PlayerData getOrLoad(UUID id, String name) {
        return cache.computeIfAbsent(id, key -> read(key, name));
    }

    public void unload(UUID id) {
        cache.remove(id);
    }

    private File file(UUID id) {
        return new File(folder, id + ".yml");
    }

    private PlayerData read(UUID id, String name) {
        File file = file(id);
        PlayerData data = new PlayerData(id, name == null ? "?" : name);
        if (!file.exists()) {
            data.setBalance(plugin.economy().startingBalance());
            return data;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (name == null) {
            data.setName(yaml.getString("name", "?"));
        }
        data.setBalance(yaml.getDouble("balance", 0));
        data.setLevel(Math.max(1, yaml.getInt("level", 1)));
        data.setXp(yaml.getLong("xp", 0));
        data.setRank(yaml.getString("rank"));
        data.setActiveTag(yaml.getString("tag"));
        data.getOwnedTags().addAll(yaml.getStringList("owned-tags"));

        ConfigurationSection homes = yaml.getConfigurationSection("homes");
        if (homes != null) {
            for (String key : homes.getKeys(false)) {
                Location location = Locs.read(homes.getConfigurationSection(key));
                if (location != null) {
                    data.getHomes().put(key, location);
                }
            }
        }

        data.setQuestDay(yaml.getLong("quests.day", 0));
        ConfigurationSection progress = yaml.getConfigurationSection("quests.progress");
        if (progress != null) {
            for (String key : progress.getKeys(false)) {
                data.getQuestProgress().put(key, progress.getInt(key));
            }
        }
        data.getQuestsDone().addAll(yaml.getStringList("quests.done"));
        return data;
    }

    public void save(PlayerData data) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", data.getName());
        yaml.set("balance", data.getBalance());
        yaml.set("level", data.getLevel());
        yaml.set("xp", data.getXp());
        yaml.set("rank", data.getRank());
        yaml.set("tag", data.getActiveTag());
        yaml.set("owned-tags", new ArrayList<>(data.getOwnedTags()));

        for (Map.Entry<String, Location> home : data.getHomes().entrySet()) {
            Locs.write(yaml.createSection("homes." + home.getKey()), home.getValue());
        }

        yaml.set("quests.day", data.getQuestDay());
        for (Map.Entry<String, Integer> entry : data.getQuestProgress().entrySet()) {
            yaml.set("quests.progress." + entry.getKey(), entry.getValue());
        }
        yaml.set("quests.done", new ArrayList<>(data.getQuestsDone()));

        try {
            yaml.save(file(data.getUuid()));
        } catch (IOException ex) {
            plugin.getLogger().severe("Impossible de sauvegarder " + data.getName() + " : " + ex.getMessage());
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            save(data);
        }
    }

    /** Sauvegarde tout et libère la mémoire des joueurs hors-ligne. */
    public void autosave() {
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            save(entry.getValue());
            if (Bukkit.getPlayer(entry.getKey()) == null) {
                cache.remove(entry.getKey());
            }
        }
    }

    /** Classement complet (fichiers + cache), du plus riche au plus pauvre. */
    public List<BalanceEntry> allBalances() {
        Map<UUID, BalanceEntry> all = new HashMap<>();
        File[] files = folder.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    String base = file.getName().substring(0, file.getName().length() - 4);
                    UUID id = UUID.fromString(base);
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    all.put(id, new BalanceEntry(yaml.getString("name", "?"), yaml.getDouble("balance")));
                } catch (IllegalArgumentException ignored) {
                    // fichier qui n'est pas un joueur
                }
            }
        }
        for (PlayerData data : cache.values()) {
            all.put(data.getUuid(), new BalanceEntry(data.getName(), data.getBalance()));
        }
        List<BalanceEntry> list = new ArrayList<>(all.values());
        list.sort((a, b) -> Double.compare(b.balance(), a.balance()));
        return list;
    }
}

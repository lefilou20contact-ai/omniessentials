package dev.monserveur.teleport;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.util.Locs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class SpawnManager {

    private final MonServeurPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public SpawnManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    /** Spawn défini avec /setspawn, sinon le spawn du monde principal. */
    public Location get() {
        Location location = Locs.read(yaml.getConfigurationSection("spawn"));
        return location != null ? location : Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public void set(Location location) {
        yaml.set("spawn", null);
        Locs.write(yaml.createSection("spawn"), location);
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Impossible de sauvegarder spawn.yml : " + ex.getMessage());
        }
    }
}

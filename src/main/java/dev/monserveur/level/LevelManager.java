package dev.monserveur.level;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LevelManager {

    private final MonServeurPlugin plugin;
    private final Map<Material, Integer> blockXp = new HashMap<>();
    private final Map<EntityType, Integer> mobXp = new HashMap<>();

    public LevelManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        blockXp.clear();
        mobXp.clear();
        ConfigurationSection blocks = plugin.getConfig().getConfigurationSection("levels.xp.blocks");
        if (blocks != null) {
            for (String key : blocks.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material == null) {
                    plugin.getLogger().warning("Niveaux : bloc inconnu '" + key + "'");
                    continue;
                }
                blockXp.put(material, blocks.getInt(key));
            }
        }
        ConfigurationSection mobs = plugin.getConfig().getConfigurationSection("levels.xp.mobs");
        if (mobs != null) {
            for (String key : mobs.getKeys(false)) {
                try {
                    mobXp.put(EntityType.valueOf(key.toUpperCase(Locale.ROOT)), mobs.getInt(key));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Niveaux : mob inconnu '" + key + "'");
                }
            }
        }
    }

    public int maxLevel() {
        return Math.max(1, plugin.getConfig().getInt("levels.max-level", 50));
    }

    /** XP nécessaire pour passer du niveau donné au suivant. */
    public long xpNeeded(int level) {
        double base = plugin.getConfig().getDouble("levels.xp-base", 100);
        double exponent = plugin.getConfig().getDouble("levels.xp-exponent", 1.5);
        return Math.max(1, Math.round(base * Math.pow(level, exponent)));
    }

    public int xpForBlock(Material material) {
        return blockXp.getOrDefault(material, 0);
    }

    public int xpForMob(EntityType type) {
        return mobXp.getOrDefault(type, plugin.getConfig().getInt("levels.xp.mob-default", 5));
    }

    public int xpForFish() {
        return plugin.getConfig().getInt("levels.xp.fish", 10);
    }

    public void addXp(Player player, long amount) {
        if (amount <= 0) {
            return;
        }
        PlayerData data = plugin.data().get(player);
        if (data.getLevel() >= maxLevel()) {
            return;
        }
        data.setXp(data.getXp() + amount);
        while (data.getLevel() < maxLevel() && data.getXp() >= xpNeeded(data.getLevel())) {
            data.setXp(data.getXp() - xpNeeded(data.getLevel()));
            data.setLevel(data.getLevel() + 1);
            levelUp(player, data);
        }
        if (data.getLevel() >= maxLevel()) {
            data.setXp(0);
        }
    }

    private void levelUp(Player player, PlayerData data) {
        int level = data.getLevel();
        double reward = plugin.getConfig().getDouble("levels.money-per-level", 25) * level;
        plugin.economy().deposit(data, reward);

        player.showTitle(Title.title(
                plugin.messages().plain("level-up-title"),
                plugin.messages().plain("level-up-subtitle", "level", level),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        plugin.msg(player, "level-up-reward", "level", level, "amount", plugin.economy().format(reward));

        int every = plugin.getConfig().getInt("levels.broadcast-every", 10);
        if (every > 0 && level % every == 0) {
            Bukkit.broadcast(plugin.messages().get("level-up-broadcast", "player", player.getName(), "level", level));
        }
    }
}

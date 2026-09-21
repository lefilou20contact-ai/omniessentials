package dev.monserveur.npc;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.gui.HomesMenu;
import dev.monserveur.gui.MainMenu;
import dev.monserveur.gui.QuestsMenu;
import dev.monserveur.gui.ShopMenu;
import dev.monserveur.gui.TagsMenu;
import dev.monserveur.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * PNJ = villageois immobiles et invulnérables avec un hologramme au-dessus.
 * Clic droit ou gauche : exécute l'action (ouvrir un menu, lancer une commande, envoyer un lien).
 */
public final class NpcManager implements Listener {

    private final MonServeurPlugin plugin;
    private final NamespacedKey key;
    private final File file;
    private final Map<String, Npc> npcs = new LinkedHashMap<>();
    private final Map<UUID, Long> lastClick = new HashMap<>();

    public NpcManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "npc_id");
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
        load();
    }

    // ------------------------------------------------------------------ données

    private void load() {
        npcs.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("npcs");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection c = section.getConfigurationSection(id);
            if (c == null) {
                continue;
            }
            Npc npc = new Npc(id);
            npc.kind = c.getString("kind", "VILLAGER");
            npc.world = c.getString("world", "world");
            npc.x = c.getDouble("x");
            npc.y = c.getDouble("y");
            npc.z = c.getDouble("z");
            npc.yaw = (float) c.getDouble("yaw");
            npc.name = c.getString("name", id);
            npc.subtitle = c.getString("subtitle", "");
            npc.profession = c.getString("profession", "librarian");
            npc.actionType = c.getString("action", "NONE");
            npc.actionValue = c.getString("value", "");
            npcs.put(id, npc);
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Npc npc : npcs.values()) {
            String path = "npcs." + npc.id;
            yaml.set(path + ".kind", npc.kind);
            yaml.set(path + ".world", npc.world);
            yaml.set(path + ".x", npc.x);
            yaml.set(path + ".y", npc.y);
            yaml.set(path + ".z", npc.z);
            yaml.set(path + ".yaw", (double) npc.yaw);
            yaml.set(path + ".name", npc.name);
            yaml.set(path + ".subtitle", npc.subtitle);
            yaml.set(path + ".profession", npc.profession);
            yaml.set(path + ".action", npc.actionType);
            yaml.set(path + ".value", npc.actionValue);
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Impossible de sauvegarder npcs.yml : " + ex.getMessage());
        }
    }

    public Npc get(String id) {
        return npcs.get(id);
    }

    public Collection<Npc> all() {
        return npcs.values();
    }

    /** Ajoute (ou remplace) un PNJ, le sauvegarde et le fait apparaître. */
    public void add(Npc npc) {
        Npc old = npcs.get(npc.id);
        if (old != null) {
            removeEntities(old);
        }
        npcs.put(npc.id, npc);
        save();
        spawn(npc);
    }

    /** Sauvegarde les modifications d'un PNJ et le fait réapparaître. */
    public void update(Npc npc) {
        save();
        spawn(npc);
    }

    public void relocate(Npc npc, Location location) {
        removeEntities(npc);
        npc.world = location.getWorld().getName();
        npc.x = location.getX();
        npc.y = location.getY();
        npc.z = location.getZ();
        npc.yaw = location.getYaw();
        update(npc);
    }

    public boolean remove(String id) {
        Npc npc = npcs.remove(id);
        if (npc == null) {
            return false;
        }
        removeEntities(npc);
        save();
        return true;
    }

    // ------------------------------------------------------------------ apparition

    public void respawnAll() {
        for (Npc npc : npcs.values()) {
            spawn(npc);
        }
    }

    private String idOf(Entity entity) {
        return entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private void removeEntities(Npc npc) {
        World world = Bukkit.getWorld(npc.world);
        if (world == null) {
            return;
        }
        Location location = new Location(world, npc.x, npc.y, npc.z);
        location.getChunk().load();
        for (Entity entity : world.getNearbyEntities(location, 3, 6, 3, e -> npc.id.equals(idOf(e)))) {
            entity.remove();
        }
    }

    public void spawn(Npc npc) {
        World world = Bukkit.getWorld(npc.world);
        if (world == null) {
            plugin.getLogger().warning("PNJ '" + npc.id + "' : monde introuvable (" + npc.world + ")");
            return;
        }
        Location location = new Location(world, npc.x, npc.y, npc.z, npc.yaw, 0f);
        removeEntities(npc);

        if (npc.kind.equalsIgnoreCase("HOLOGRAM")) {
            spawnHologram(location, npc);
            return;
        }

        world.spawn(location, Villager.class, villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setCanPickupItems(false);
            villager.setCustomNameVisible(false);
            villager.customName(Text.mm(npc.name));
            villager.setRotation(npc.yaw, 0f);
            Villager.Profession profession = Registry.VILLAGER_PROFESSION.get(
                    NamespacedKey.minecraft(npc.profession.toLowerCase(Locale.ROOT)));
            if (profession != null) {
                villager.setProfession(profession);
            }
            villager.setVillagerLevel(5);
            villager.getPersistentDataContainer().set(key, PersistentDataType.STRING, npc.id);
        });
        spawnHologram(location.clone().add(0, 2.4, 0), npc);
    }

    private void spawnHologram(Location location, Npc npc) {
        String text = (npc.subtitle == null || npc.subtitle.isBlank())
                ? npc.name
                : npc.name + "<newline>" + npc.subtitle;
        String formatted = plugin.links().format(text, null);
        location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(Text.mm(formatted));
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowed(true);
            display.setPersistent(true);
            display.getPersistentDataContainer().set(key, PersistentDataType.STRING, npc.id);
        });
    }

    // ------------------------------------------------------------------ interactions

    private boolean throttled(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastClick.get(player.getUniqueId());
        if (last != null && now - last < 400) {
            return true;
        }
        lastClick.put(player.getUniqueId(), now);
        return false;
    }

    private void run(Player player, Npc npc) {
        String value = npc.actionValue == null ? "" : npc.actionValue;
        switch (npc.actionType.toUpperCase(Locale.ROOT)) {
            case "MENU" -> openMenu(player, value);
            case "COMMAND" -> player.performCommand(value.startsWith("/") ? value.substring(1) : value);
            case "LINK" -> plugin.links().sendLink(player, value);
            default -> {
            }
        }
    }

    private void openMenu(Player player, String menu) {
        switch (menu.toLowerCase(Locale.ROOT)) {
            case "shop" -> new ShopMenu(plugin, player).open();
            case "homes" -> new HomesMenu(plugin, player).open();
            case "quests" -> new QuestsMenu(plugin, player).open();
            case "tags" -> new TagsMenu(plugin, player).open();
            default -> new MainMenu(plugin, player).open();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        String id = idOf(event.getRightClicked());
        if (id == null) {
            return;
        }
        event.setCancelled(true); // pas de fenêtre d'échange du villageois
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Npc npc = npcs.get(id);
        if (npc != null && !throttled(event.getPlayer())) {
            run(event.getPlayer(), npc);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        String id = idOf(event.getEntity());
        if (id == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getDamager() instanceof Player player) {
            Npc npc = npcs.get(id);
            if (npc != null && !throttled(player)) {
                run(player, npc);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastClick.remove(event.getPlayer().getUniqueId());
    }
}

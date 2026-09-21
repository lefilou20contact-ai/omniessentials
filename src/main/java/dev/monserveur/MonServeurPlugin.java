package dev.monserveur;

import dev.monserveur.command.CoreCommands;
import dev.monserveur.command.EconomyCommands;
import dev.monserveur.build.SpawnBuilder;
import dev.monserveur.command.HomeCommands;
import dev.monserveur.command.NpcCommand;
import dev.monserveur.link.LinkManager;
import dev.monserveur.npc.NpcManager;
import dev.monserveur.data.PlayerDataManager;
import dev.monserveur.economy.EconomyManager;
import dev.monserveur.gui.MenuListener;
import dev.monserveur.home.HomeManager;
import dev.monserveur.level.LevelManager;
import dev.monserveur.listener.ActivityListener;
import dev.monserveur.listener.ChatListener;
import dev.monserveur.listener.ConnectionListener;
import dev.monserveur.listener.LobbyListener;
import dev.monserveur.listener.MenuItemListener;
import dev.monserveur.quest.QuestManager;
import dev.monserveur.rank.RankManager;
import dev.monserveur.rank.TagManager;
import dev.monserveur.shop.ShopManager;
import dev.monserveur.tab.TabManager;
import dev.monserveur.teleport.SpawnManager;
import dev.monserveur.teleport.TeleportManager;
import dev.monserveur.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class MonServeurPlugin extends JavaPlugin {

    private Messages messages;
    private PlayerDataManager data;
    private RankManager ranks;
    private TagManager tags;
    private EconomyManager economy;
    private ShopManager shop;
    private LevelManager levels;
    private QuestManager quests;
    private HomeManager homes;
    private SpawnManager spawn;
    private TeleportManager teleports;
    private TabManager tab;
    private MenuItemListener menuItem;
    private LinkManager links;
    private NpcManager npcs;
    private SpawnBuilder spawnBuilder;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new Messages(this);
        economy = new EconomyManager(this);
        data = new PlayerDataManager(this);
        ranks = new RankManager(this);
        tags = new TagManager(this);
        shop = new ShopManager(this);
        levels = new LevelManager(this);
        quests = new QuestManager(this);
        homes = new HomeManager(this);
        spawn = new SpawnManager(this);
        teleports = new TeleportManager(this);
        tab = new TabManager(this);
        menuItem = new MenuItemListener(this);
        links = new LinkManager(this);
        npcs = new NpcManager(this);
        spawnBuilder = new SpawnBuilder(this);

        ranks.reload();
        tags.reload();
        shop.reload();
        levels.reload();
        quests.reload();
        links.load();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ConnectionListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new ActivityListener(this), this);
        pm.registerEvents(new LobbyListener(this), this);
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(menuItem, this);
        pm.registerEvents(teleports, this);
        pm.registerEvents(npcs, this);

        EconomyCommands economyCommands = new EconomyCommands(this);
        for (String name : List.of("balance", "pay", "baltop", "eco")) {
            bind(name, economyCommands);
        }
        HomeCommands homeCommands = new HomeCommands(this);
        for (String name : List.of("sethome", "home", "delhome", "homes")) {
            bind(name, homeCommands);
        }
        bind("npc", new NpcCommand(this));
        CoreCommands coreCommands = new CoreCommands(this);
        for (String name : List.of("menu", "shop", "spawn", "setspawn", "level", "quests", "tag", "rank", "monserveur")) {
            bind(name, coreCommands);
        }

        // Sauvegarde automatique toutes les 5 minutes
        getServer().getScheduler().runTaskTimer(this, () -> data.autosave(), 20L * 60 * 5, 20L * 60 * 5);
        // En-tête / pied de page du TAB
        long tabTicks = Math.max(20L, getConfig().getLong("tab.update-ticks", 100));
        getServer().getScheduler().runTaskTimer(this, () -> tab.refreshAll(), 40L, tabTicks);

        // Fait apparaître les PNJ une fois les mondes chargés
        getServer().getScheduler().runTask(this, () -> npcs.respawnAll());

        // Cas d'un /reload : des joueurs sont déjà connectés
        for (Player player : Bukkit.getOnlinePlayers()) {
            tab.apply(player);
            tab.updateHeaderFooter(player);
        }
        getLogger().info("MonServeur activé.");
    }

    @Override
    public void onDisable() {
        if (data != null) {
            data.saveAll();
        }
        if (tab != null) {
            tab.cleanup();
        }
        if (links != null) {
            links.unregisterAll();
        }
    }

    private void bind(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Commande absente de plugin.yml : " + name);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    /** Recharge config.yml et reconstruit grades, tags, boutique, niveaux et quêtes. */
    public void reloadAll() {
        reloadConfig();
        ranks.reload();
        tags.reload();
        shop.reload();
        levels.reload();
        quests.reload();
        links.load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            tab.apply(player);
            tab.updateHeaderFooter(player);
        }
    }

    /** Envoie un message configurable (préfixé) à un joueur ou à la console. */
    public void msg(CommandSender to, String key, Object... placeholders) {
        messages.send(to, key, placeholders);
    }

    public Messages messages() { return messages; }
    public PlayerDataManager data() { return data; }
    public RankManager ranks() { return ranks; }
    public TagManager tags() { return tags; }
    public EconomyManager economy() { return economy; }
    public ShopManager shop() { return shop; }
    public LevelManager levels() { return levels; }
    public QuestManager quests() { return quests; }
    public HomeManager homes() { return homes; }
    public SpawnManager spawn() { return spawn; }
    public TeleportManager teleports() { return teleports; }
    public TabManager tab() { return tab; }
    public MenuItemListener menuItem() { return menuItem; }
    public LinkManager links() { return links; }
    public NpcManager npcs() { return npcs; }
    public SpawnBuilder spawnBuilder() { return spawnBuilder; }
}

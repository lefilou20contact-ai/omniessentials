package dev.monserveur.link;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gère links.yml : variables (liens), commandes personnalisées,
 * annonces automatiques et message d'accueil.
 */
public final class LinkManager {

    private final MonServeurPlugin plugin;
    private final File file;
    private final Map<String, String> variables = new LinkedHashMap<>();
    private YamlConfiguration yaml = new YamlConfiguration();
    private BukkitTask announceTask;
    private int announceIndex;

    public LinkManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "links.yml");
    }

    /** (Re)charge links.yml, réenregistre les commandes et relance les annonces. */
    public void load() {
        if (!file.exists()) {
            plugin.saveResource("links.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);

        variables.clear();
        ConfigurationSection vars = yaml.getConfigurationSection("variables");
        if (vars != null) {
            for (String key : vars.getKeys(false)) {
                variables.put(key, vars.getString(key, ""));
            }
        }
        registerCommands();
        startAnnouncements();
    }

    public String variable(String key) {
        return variables.getOrDefault(key, "");
    }

    private static final java.util.regex.Pattern LINK_TAG =
            java.util.regex.Pattern.compile("\\{link:([a-zA-Z0-9_-]+)(?:\\|([^}]+))?}");

    private static final Map<String, String> DEFAULT_LABELS = Map.of(
            "discord", "Rejoindre le Discord",
            "youtube", "Voir la chaîne YouTube",
            "store", "Ouvrir la boutique",
            "website", "Ouvrir le site");

    /**
     * Remplace {variables}, {player}, {online}, {max} et les liens cliquables
     * {link:cle} ou {link:cle|Texte affiché} (le texte affiché est un hypertexte,
     * pas l'URL). Le résultat est un fragment MiniMessage prêt à être affiché.
     */
    public String format(String text, CommandSender sender) {
        java.util.regex.Matcher matcher = LINK_TAG.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder,
                    java.util.regex.Matcher.quoteReplacement(buildLink(matcher.group(1), matcher.group(2))));
        }
        matcher.appendTail(builder);
        String out = builder.toString();

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue();
            if (!value.startsWith("http")) {
                out = out.replace("{" + entry.getKey() + "}", value);
            }
        }
        return out
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("{player}", sender instanceof Player ? sender.getName() : "Console");
    }

    private String escapeUrl(String url) {
        return url.replace("'", "%27").replace("<", "%3C").replace(">", "%3E");
    }

    /** Construit le fragment MiniMessage d'un lien : un texte cliquable, jamais l'URL en clair. */
    private String buildLink(String key, String customLabel) {
        String url = variables.get(key);
        if (url == null || url.isBlank()) {
            return "<red>[lien manquant: " + key + "]";
        }
        String label = (customLabel == null || customLabel.isBlank())
                ? DEFAULT_LABELS.getOrDefault(key, "Cliquer ici")
                : customLabel;
        String template = yaml.getString("link-format",
                "<click:open_url:'{url}'><hover:show_text:'<gray>{url}'><aqua><underlined>{label}</underlined></hover></click>");
        return template.replace("{url}", escapeUrl(url)).replace("{label}", label);
    }

    /** Envoie un lien cliquable (utilisé par les PNJ de type LINK). Le joueur ne voit jamais l'URL brute. */
    public void sendLink(CommandSender to, String variable) {
        if (variables.get(variable) == null || variables.get(variable).isBlank()) {
            plugin.msg(to, "link-not-found", "link", variable);
            return;
        }
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        to.sendMessage(Text.mm(prefix + buildLink(variable, null)));
    }

    // ------------------------------------------------------------------ commandes

    private CommandMap commandMap() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) method.invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException | ClassCastException ex) {
            plugin.getLogger().warning("Impossible d'accéder à la CommandMap : " + ex.getMessage());
            return null;
        }
    }

    private void registerCommands() {
        CommandMap map = commandMap();
        if (map == null) {
            return;
        }
        map.getKnownCommands().values().removeIf(command -> command instanceof LinkCommand);

        ConfigurationSection commands = yaml.getConfigurationSection("commands");
        if (commands != null) {
            for (String name : commands.getKeys(false)) {
                ConfigurationSection c = commands.getConfigurationSection(name);
                if (c == null) {
                    continue;
                }
                map.register("monserveur", new LinkCommand(
                        name.toLowerCase(Locale.ROOT),
                        c.getString("description", "Commande personnalisée"),
                        c.getStringList("aliases"),
                        c.getStringList("lines"),
                        c.getString("permission")));
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    /** À l'arrêt du plugin : retire les commandes créées. */
    public void unregisterAll() {
        CommandMap map = commandMap();
        if (map != null) {
            map.getKnownCommands().values().removeIf(command -> command instanceof LinkCommand);
        }
        if (announceTask != null) {
            announceTask.cancel();
            announceTask = null;
        }
    }

    private final class LinkCommand extends Command {

        private final List<String> lines;

        LinkCommand(String name, String description, List<String> aliases, List<String> lines, String permission) {
            super(name, description, "/" + name, aliases);
            this.lines = lines;
            if (permission != null && !permission.isBlank()) {
                setPermission(permission);
            }
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (getPermission() != null && !sender.hasPermission(getPermission())) {
                plugin.msg(sender, "no-permission");
                return true;
            }
            for (String line : lines) {
                sender.sendMessage(Text.mm(format(line, sender)));
            }
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return List.of();
        }
    }

    // ------------------------------------------------------------------ annonces

    private void startAnnouncements() {
        if (announceTask != null) {
            announceTask.cancel();
            announceTask = null;
        }
        ConfigurationSection section = yaml.getConfigurationSection("announcements");
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }
        List<String> messages = section.getStringList("messages");
        if (messages.isEmpty()) {
            return;
        }
        long ticks = Math.max(30L, section.getLong("interval-seconds", 300)) * 20L;
        int minPlayers = section.getInt("min-players", 1);
        String prefix = section.getString("prefix", "");

        announceTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getOnlinePlayers().size() < minPlayers) {
                return;
            }
            String raw = messages.get(announceIndex % messages.size());
            announceIndex++;
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Text.mm(format(prefix + raw, player)));
            }
        }, ticks, ticks);
    }

    // ------------------------------------------------------------------ accueil

    public void welcome(Player player) {
        ConfigurationSection section = yaml.getConfigurationSection("welcome");
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }
        long delay = Math.max(1L, section.getLong("delay-ticks", 30));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            for (String line : section.getStringList("lines")) {
                player.sendMessage(Text.mm(format(line, player)));
            }
            String title = section.getString("title", "");
            String subtitle = section.getString("subtitle", "");
            if (!title.isBlank() || !subtitle.isBlank()) {
                player.showTitle(Title.title(
                        Text.mm(format(title, player)),
                        Text.mm(format(subtitle, player)),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(800))));
            }
            if (section.getBoolean("sound", true)) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
            }
        }, delay);
    }
}

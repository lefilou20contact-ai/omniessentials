package dev.monserveur.tab;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.rank.Rank;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Gère la liste TAB (tri par grade, préfixe, en-tête/pied de page)
 * et le préfixe affiché au-dessus de la tête, via des teams du scoreboard principal.
 */
public final class TabManager {

    private static final String TEAM_PREFIX = "ms";

    private final MonServeurPlugin plugin;

    public TabManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    private Scoreboard board() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    private String teamName(Rank rank) {
        // Les teams sont triées alphabétiquement dans le TAB : poids élevé => nom plus petit => plus haut.
        int order = 999 - Math.max(0, Math.min(999, rank.weight()));
        String name = TEAM_PREFIX + String.format("%03d", order) + rank.id();
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    /** Place le joueur dans la team de son grade (préfixe TAB + tête). */
    public void apply(Player player) {
        Scoreboard board = board();
        Rank rank = plugin.ranks().of(player);
        String wanted = teamName(rank);

        for (Team team : board.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX) && !team.getName().equals(wanted) && team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }

        Team team = board.getTeam(wanted);
        if (team == null) {
            team = board.registerNewTeam(wanted);
        }
        team.prefix(Text.mm(rank.prefix()));
        NamedTextColor color = NamedTextColor.NAMES.value(rank.teamColor().toLowerCase(Locale.ROOT));
        if (color != null) {
            team.color(color);
        }
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public void remove(Player player) {
        for (Team team : board().getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX) && team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }
    }

    public void updateHeaderFooter(Player player) {
        if (!plugin.getConfig().getBoolean("tab.enabled", true)) {
            return;
        }
        player.sendPlayerListHeaderAndFooter(
                Text.mm(join(plugin.getConfig().getStringList("tab.header"), player)),
                Text.mm(join(plugin.getConfig().getStringList("tab.footer"), player)));
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateHeaderFooter(player);
        }
    }

    /** Supprime nos teams à l'arrêt du plugin. */
    public void cleanup() {
        for (Team team : List.copyOf(board().getTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }
    }

    private String join(List<String> lines, Player player) {
        return lines.stream().map(line -> fill(line, player)).collect(Collectors.joining("<newline>"));
    }

    private String fill(String text, Player player) {
        PlayerData data = plugin.data().get(player);
        return text
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("{balance}", plugin.economy().format(data.getBalance()))
                .replace("{level}", String.valueOf(data.getLevel()))
                .replace("{rank}", plugin.ranks().of(player).display())
                .replace("{player}", player.getName())
                .replace("{ping}", String.valueOf(player.getPing()));
    }
}

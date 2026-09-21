package dev.monserveur.quest;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.util.Text;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class QuestManager {

    private final MonServeurPlugin plugin;
    private final Map<String, Quest> quests = new LinkedHashMap<>();

    public QuestManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        quests.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("quests");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection q = section.getConfigurationSection(id);
            if (q == null) {
                continue;
            }
            Quest.Type type;
            try {
                type = Quest.Type.valueOf(q.getString("type", "").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Quête '" + id + "' : type invalide (BREAK_BLOCK, KILL_MOB ou FISH).");
                continue;
            }
            Set<String> targets = new HashSet<>();
            for (String target : q.getString("target", "ANY").split(",")) {
                if (!target.isBlank()) {
                    targets.add(target.trim().toUpperCase(Locale.ROOT));
                }
            }
            quests.put(id, new Quest(
                    id,
                    q.getString("name", id),
                    q.getString("description", ""),
                    type,
                    targets,
                    Math.max(1, q.getInt("amount", 1)),
                    q.getDouble("reward.money", 0),
                    q.getLong("reward.xp", 0)));
        }
    }

    public Collection<Quest> all() {
        return quests.values();
    }

    /** Remet les quêtes à zéro si on est un nouveau jour. */
    public void checkReset(PlayerData data) {
        long today = LocalDate.now().toEpochDay();
        if (data.getQuestDay() != today) {
            data.getQuestProgress().clear();
            data.getQuestsDone().clear();
            data.setQuestDay(today);
        }
    }

    public void progress(Player player, Quest.Type type, String target, int amount) {
        PlayerData data = plugin.data().get(player);
        checkReset(data);
        for (Quest quest : quests.values()) {
            if (quest.type() != type || data.getQuestsDone().contains(quest.id()) || !quest.matches(target)) {
                continue;
            }
            int now = data.getQuestProgress().merge(quest.id(), amount, Integer::sum);
            if (now >= quest.amount()) {
                complete(player, data, quest);
            }
        }
    }

    private void complete(Player player, PlayerData data, Quest quest) {
        data.getQuestsDone().add(quest.id());
        plugin.economy().deposit(data, quest.rewardMoney());
        plugin.msg(player, "quest-complete",
                "quest", Text.mm(quest.name()),
                "money", plugin.economy().format(quest.rewardMoney()),
                "xp", quest.rewardXp());
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        plugin.levels().addXp(player, quest.rewardXp());
    }
}

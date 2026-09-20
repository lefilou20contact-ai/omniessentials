package dev.monserveur.gui;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.quest.Quest;
import dev.monserveur.util.ItemBuilder;
import dev.monserveur.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class QuestsMenu extends Menu {

    public QuestsMenu(MonServeurPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return Text.mm("<dark_gray>Quêtes du jour");
    }

    @Override
    protected int rows() {
        return 4;
    }

    @Override
    protected void build() {
        PlayerData data = plugin.data().get(player);
        plugin.quests().checkReset(data);
        List<Quest> quests = List.copyOf(plugin.quests().all());

        for (int i = 0; i < quests.size() && i < 14; i++) {
            Quest quest = quests.get(i);
            boolean done = data.getQuestsDone().contains(quest.id());
            int progress = Math.min(quest.amount(), data.getQuestProgress().getOrDefault(quest.id(), 0));

            List<String> lore = new ArrayList<>();
            lore.add("<gray>" + quest.description());
            lore.add("");
            lore.add("<gray>Progression : <yellow>" + progress + "<gray>/<yellow>" + quest.amount());
            lore.add(Text.bar((double) progress / quest.amount(), 20));
            lore.add("");
            lore.add("<gray>Récompense : <gold>" + plugin.economy().format(quest.rewardMoney())
                    + " <gray>+ <aqua>" + quest.rewardXp() + " XP");
            lore.add(done ? "<green>✔ Terminée" : "<yellow>En cours...");

            ItemBuilder builder = new ItemBuilder(done ? Material.ENCHANTED_BOOK : Material.BOOK)
                    .name(quest.name())
                    .lore(lore);
            if (done) {
                builder.glow();
            }
            set(GRID[i], builder.build());
        }

        set(35, new ItemBuilder(Material.CLOCK)
                .name("<yellow>Réinitialisation")
                .lore("<gray>Les quêtes se remettent à zéro", "<gray>chaque jour à minuit.")
                .build());
        back(31, () -> new MainMenu(plugin, player).open());
    }
}

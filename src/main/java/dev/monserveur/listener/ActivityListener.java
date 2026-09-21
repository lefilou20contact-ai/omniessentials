package dev.monserveur.listener;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.quest.Quest;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

/** Donne de l'XP et fait avancer les quêtes selon l'activité du joueur. */
public final class ActivityListener implements Listener {

    private final MonServeurPlugin plugin;

    public ActivityListener(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        Material material = event.getBlock().getType();
        plugin.quests().progress(player, Quest.Type.BREAK_BLOCK, material.name(), 1);
        plugin.levels().addXp(player, plugin.levels().xpForBlock(material));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntity() instanceof Player) {
            return;
        }
        EntityType type = event.getEntityType();
        plugin.quests().progress(killer, Quest.Type.KILL_MOB, type.name(), 1);
        plugin.levels().addXp(killer, plugin.levels().xpForMob(type));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        plugin.quests().progress(player, Quest.Type.FISH, "ANY", 1);
        plugin.levels().addXp(player, plugin.levels().xpForFish());
    }
}

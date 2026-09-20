package dev.monserveur.data;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Toutes les données persistantes d'un joueur. */
public final class PlayerData {

    private final UUID uuid;
    private String name;
    private double balance;
    private int level = 1;
    private long xp;
    private String rank;        // null = grade par défaut
    private String activeTag;   // null = aucun tag
    private final Set<String> ownedTags = new LinkedHashSet<>();
    private final Map<String, Location> homes = new LinkedHashMap<>();
    private long questDay;
    private final Map<String, Integer> questProgress = new HashMap<>();
    private final Set<String> questsDone = new HashSet<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() { return uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public long getXp() { return xp; }
    public void setXp(long xp) { this.xp = xp; }

    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }

    public String getActiveTag() { return activeTag; }
    public void setActiveTag(String activeTag) { this.activeTag = activeTag; }

    public Set<String> getOwnedTags() { return ownedTags; }

    public Map<String, Location> getHomes() { return homes; }

    public long getQuestDay() { return questDay; }
    public void setQuestDay(long questDay) { this.questDay = questDay; }

    public Map<String, Integer> getQuestProgress() { return questProgress; }

    public Set<String> getQuestsDone() { return questsDone; }
}

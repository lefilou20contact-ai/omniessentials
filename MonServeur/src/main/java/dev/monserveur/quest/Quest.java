package dev.monserveur.quest;

import java.util.Set;

public record Quest(
        String id,
        String name,
        String description,
        Type type,
        Set<String> targets,
        int amount,
        double rewardMoney,
        long rewardXp) {

    public enum Type {
        BREAK_BLOCK,
        KILL_MOB,
        FISH
    }

    public boolean matches(String target) {
        return targets.contains("ANY") || targets.contains(target.toUpperCase(java.util.Locale.ROOT));
    }
}

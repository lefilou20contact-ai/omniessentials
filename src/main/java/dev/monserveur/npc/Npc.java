package dev.monserveur.npc;

/** Définition d'un PNJ ou d'un hologramme (sauvegardée dans npcs.yml). */
public final class Npc {

    public final String id;
    public String kind = "VILLAGER";      // VILLAGER ou HOLOGRAM
    public String world;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public String name;
    public String subtitle = "";
    public String profession = "librarian";
    public String actionType = "NONE";    // NONE, MENU, COMMAND, LINK
    public String actionValue = "";

    public Npc(String id) {
        this.id = id;
        this.name = id;
    }
}

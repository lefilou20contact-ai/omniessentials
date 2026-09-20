package dev.monserveur.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/** Sauvegarde / lecture de Location dans un fichier YAML (sans planter si un monde a disparu). */
public final class Locs {

    private Locs() {
    }

    public static void write(ConfigurationSection section, Location location) {
        if (location.getWorld() == null) {
            return;
        }
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", (double) location.getYaw());
        section.set("pitch", (double) location.getPitch());
    }

    /** Retourne null si la section est absente ou si le monde n'existe plus. */
    public static Location read(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        World world = Bukkit.getWorld(section.getString("world", ""));
        if (world == null) {
            return null;
        }
        return new Location(world,
                section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }
}

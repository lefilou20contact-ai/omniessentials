package dev.monserveur.build;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.npc.Npc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Génère un spawn complet autour du joueur : place ronde en damier, fontaine, 4 chemins,
 * 4 portails, 4 pavillons, lampadaires, cerisiers, PNJ et hologramme.
 * La construction est étalée sur plusieurs ticks pour ne pas faire laguer le serveur,
 * et peut être annulée (/monserveur buildspawn undo) tant que le serveur n'a pas redémarré.
 */
public final class SpawnBuilder {

    private static final int PER_TICK = 1500;

    private record Placement(int x, int y, int z, BlockData data) {
    }

    private record Change(int x, int y, int z, BlockData old) {
    }

    private final MonServeurPlugin plugin;
    private final Map<String, BlockData> cache = new HashMap<>();
    private final Deque<Change> undoStack = new ArrayDeque<>();
    private final List<String> createdNpcIds = new ArrayList<>();
    private World undoWorld;
    private boolean busy;
    private YamlConfiguration cfg = new YamlConfiguration();

    public SpawnBuilder(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ config

    private void load() {
        File file = new File(plugin.getDataFolder(), "spawn-build.yml");
        if (!file.exists()) {
            plugin.saveResource("spawn-build.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        cache.clear();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int radius() {
        return clamp(cfg.getInt("radius", 20), 16, 28);
    }

    /** Largeur de la zone remplacée (pour le message d'avertissement). */
    public int size() {
        load();
        return radius() * 2 + 1;
    }

    private BlockData pal(String key, String fallback) {
        String raw = cfg.getString("palette." + key, fallback).trim().toLowerCase(Locale.ROOT);
        return cache.computeIfAbsent(raw, r -> {
            try {
                return Bukkit.createBlockData(r.contains(":") ? r : "minecraft:" + r);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("spawn-build.yml : matériau invalide '" + r + "', remplacé par stone_bricks.");
                return Bukkit.createBlockData(Material.STONE_BRICKS);
            }
        });
    }

    private BlockData data(String state) {
        return cache.computeIfAbsent(state, Bukkit::createBlockData);
    }

    // ------------------------------------------------------------------ construction

    public void build(Player player) {
        if (busy) {
            plugin.msg(player, "build-busy");
            return;
        }
        load();
        World world = player.getWorld();
        int cx = player.getLocation().getBlockX();
        int cz = player.getLocation().getBlockZ();
        int fy = player.getLocation().getBlockY() - 1;

        List<Placement> plan = new ArrayList<>();
        List<int[]> treeSpots = new ArrayList<>();
        makePlan(world, cx, cz, fy, plan, treeSpots);

        busy = true;
        undoStack.clear();
        undoWorld = world;
        plugin.msg(player, "build-start", "blocks", plan.size());

        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                try {
                    int end = Math.min(plan.size(), index + PER_TICK);
                    for (; index < end; index++) {
                        Placement p = plan.get(index);
                        Block block = world.getBlockAt(p.x(), p.y(), p.z());
                        undoStack.push(new Change(p.x(), p.y(), p.z(), block.getBlockData()));
                        block.setBlockData(p.data(), false);
                    }
                    if (index >= plan.size()) {
                        cancel();
                        finish(player, world, cx, cz, fy, treeSpots);
                        busy = false;
                    }
                } catch (RuntimeException ex) {
                    cancel();
                    busy = false;
                    plugin.getLogger().severe("Erreur pendant la construction du spawn : " + ex);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void undo(Player player) {
        if (busy) {
            plugin.msg(player, "build-busy");
            return;
        }
        if (undoStack.isEmpty() || undoWorld == null) {
            plugin.msg(player, "build-nothing");
            return;
        }
        busy = true;
        World world = undoWorld;
        for (String id : createdNpcIds) {
            plugin.npcs().remove(id);
        }
        createdNpcIds.clear();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < PER_TICK && !undoStack.isEmpty(); i++) {
                        Change c = undoStack.pop();
                        world.getBlockAt(c.x(), c.y(), c.z()).setBlockData(c.old(), false);
                    }
                    if (undoStack.isEmpty()) {
                        cancel();
                        busy = false;
                        plugin.msg(player, "build-undone");
                    }
                } catch (RuntimeException ex) {
                    cancel();
                    busy = false;
                    plugin.getLogger().severe("Erreur pendant l'annulation : " + ex);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Dernière étape (thread principal) : arbres, point de spawn, hologramme et PNJ. */
    private void finish(Player player, World world, int cx, int cz, int fy, List<int[]> treeSpots) {
        int r = radius();

        if (cfg.getBoolean("trees", true)) {
            for (int[] spot : treeSpots) {
                // On mémorise la zone avant l'arbre pour pouvoir l'annuler proprement
                for (int ox = -6; ox <= 6; ox++) {
                    for (int oz = -6; oz <= 6; oz++) {
                        for (int oy = 1; oy <= 12; oy++) {
                            int x = cx + spot[0] + ox;
                            int y = fy + oy;
                            int z = cz + spot[1] + oz;
                            undoStack.push(new Change(x, y, z, world.getBlockAt(x, y, z).getBlockData()));
                        }
                    }
                }
                Location base = new Location(world, cx + spot[0], fy + 1, cz + spot[1]);
                if (!world.generateTree(base, TreeType.CHERRY)) {
                    world.generateTree(base, TreeType.TREE);
                }
            }
        }

        Location spawn = new Location(world, cx + 0.5, fy + 1, cz + r / 2 + 0.5, 180f, 0f);
        world.setSpawnLocation(spawn);
        plugin.spawn().set(spawn);

        createdNpcIds.clear();

        Npc title = new Npc("spawn-title");
        title.kind = "HOLOGRAM";
        title.world = world.getName();
        title.x = cx + 0.5;
        title.y = fy + 5.7;
        title.z = cz + 0.5;
        title.name = cfg.getString("hologram-title", "<gold><bold>{server-name}");
        plugin.npcs().add(title);
        createdNpcIds.add(title.id);

        ConfigurationSection npcs = cfg.getConfigurationSection("npcs");
        if (npcs != null) {
            for (String key : npcs.getKeys(false)) {
                ConfigurationSection c = npcs.getConfigurationSection(key);
                if (c == null) {
                    continue;
                }
                int dx = c.getInt("dx");
                int dz = c.getInt("dz");
                Npc npc = new Npc(key.toLowerCase(Locale.ROOT));
                npc.world = world.getName();
                npc.x = cx + dx + 0.5;
                npc.y = fy + 1;
                npc.z = cz + dz + 0.5;
                npc.yaw = c.contains("yaw") ? (float) c.getDouble("yaw") : facingCenter(dx, dz);
                npc.name = c.getString("name", key);
                npc.subtitle = c.getString("subtitle", "");
                npc.profession = c.getString("profession", "librarian");
                npc.actionType = c.getString("action", "NONE").toUpperCase(Locale.ROOT);
                npc.actionValue = c.getString("value", "");
                plugin.npcs().add(npc);
                createdNpcIds.add(npc.id);
            }
        }

        plugin.msg(player, "build-done");
    }

    /** Angle (yaw Minecraft) pour regarder vers le centre depuis (dx, dz). */
    private static float facingCenter(int dx, int dz) {
        return (float) Math.toDegrees(Math.atan2(dx, -dz));
    }

    // ------------------------------------------------------------------ plan des blocs

    private void add(List<Placement> plan, World world, int x, int y, int z, BlockData data) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return;
        }
        plan.add(new Placement(x, y, z, data));
    }

    private static boolean isGate(int dx, int dz, int r) {
        return (Math.abs(dz) <= 2 && Math.abs(dx) > r / 2) || (Math.abs(dx) <= 2 && Math.abs(dz) > r / 2);
    }

    private void makePlan(World world, int cx, int cz, int fy, List<Placement> plan, List<int[]> treeSpots) {
        int r = radius();
        int clearHeight = clamp(cfg.getInt("clear-height", 16), 8, 30);
        int pavilion = clamp(cfg.getInt("pavilion-offset", 8), 6, r - 6);

        BlockData air = Bukkit.createBlockData(Material.AIR);
        BlockData center = pal("center", "smooth_quartz");
        BlockData inner = pal("inner", "stone_bricks");
        BlockData checkerA = pal("checker-a", "polished_andesite");
        BlockData checkerB = pal("checker-b", "smooth_stone");
        BlockData outer = pal("outer", "polished_diorite");
        BlockData path = pal("path", "smooth_sandstone");
        BlockData rim = pal("rim", "polished_blackstone_bricks");
        BlockData curb = pal("curb", "minecraft:polished_blackstone_brick_slab[type=bottom]");
        BlockData foundation = pal("foundation", "stone_bricks");
        BlockData fountainWall = pal("fountain-wall", "quartz_bricks");
        BlockData fountainSlab = pal("fountain-slab", "minecraft:quartz_slab[type=bottom]");
        BlockData water = pal("fountain-water", "water");
        BlockData fountainPillar = pal("fountain-pillar", "quartz_pillar");
        BlockData light = pal("light", "sea_lantern");
        BlockData pavilionFloor = pal("pavilion-floor", "polished_deepslate");
        BlockData post = pal("post", "minecraft:stripped_dark_oak_log[axis=y]");
        BlockData roof = pal("roof", "dark_oak_planks");
        BlockData roofSlab = pal("roof-slab", "minecraft:dark_oak_slab[type=bottom]");
        BlockData gatePillar = pal("gate-pillar", "stone_bricks");
        BlockData gateTop = pal("gate-top", "chiseled_stone_bricks");
        BlockData lampPost = pal("lamp-post", "stone_brick_wall");
        BlockData grass = pal("grass", "grass_block");
        BlockData lanternHanging = data("minecraft:lantern[hanging=true]");
        BlockData lanternStanding = data("minecraft:lantern[hanging=false]");

        // 1) Nettoyage, fondations, sol, bordure et bassin de la fontaine
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.5) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;

                for (int dy = 1; dy <= clearHeight; dy++) {
                    if (fy + dy >= world.getMaxHeight()) {
                        break;
                    }
                    if (!world.getBlockAt(x, fy + dy, z).getType().isAir()) {
                        add(plan, world, x, fy + dy, z, air);
                    }
                }

                for (int dy = 1; dy <= 30; dy++) {
                    if (fy - dy < world.getMinHeight()) {
                        break;
                    }
                    Material below = world.getBlockAt(x, fy - dy, z).getType();
                    if (below.isSolid() && !Tag.LEAVES.isTagged(below)) {
                        break;
                    }
                    add(plan, world, x, fy - dy, z, foundation);
                }

                BlockData floor;
                boolean onPath = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
                if (d < 2.5) {
                    floor = water;
                } else if (d < 3.5) {
                    floor = fountainWall;
                } else if (onPath) {
                    floor = path;
                } else if (d < 5) {
                    floor = center;
                } else if (d < 6) {
                    floor = inner;
                } else if (d < r - 3) {
                    floor = ((dx + dz) & 1) == 0 ? checkerA : checkerB;
                } else if (d < r - 0.5) {
                    floor = outer;
                } else {
                    floor = rim;
                }
                add(plan, world, x, fy, z, floor);

                if (d >= 2.5 && d < 3.5) {
                    add(plan, world, x, fy + 1, z, fountainSlab);
                }
                if (d >= r - 0.5 && !isGate(dx, dz, r)) {
                    add(plan, world, x, fy + 1, z, curb);
                }
            }
        }

        // 2) Colonne centrale de la fontaine
        for (int dy = 0; dy <= 3; dy++) {
            add(plan, world, cx, fy + dy, cz, fountainPillar);
        }
        add(plan, world, cx, fy + 4, cz, light);

        // 3) Quatre portails aux points cardinaux
        int[][] pillars = {{r, 2}, {r, -2}, {-r, 2}, {-r, -2}, {2, r}, {-2, r}, {2, -r}, {-2, -r}};
        for (int[] p : pillars) {
            for (int dy = 1; dy <= 4; dy++) {
                add(plan, world, cx + p[0], fy + dy, cz + p[1], gatePillar);
            }
            add(plan, world, cx + p[0], fy + 5, cz + p[1], gateTop);
        }
        for (int sign : new int[]{-1, 1}) {
            for (int k = -3; k <= 3; k++) {
                // portails est / ouest (poutre le long de z)
                add(plan, world, cx + sign * r, fy + 6, cz + k, roofSlab);
                // portails nord / sud (poutre le long de x)
                add(plan, world, cx + k, fy + 6, cz + sign * r, roofSlab);
                if (Math.abs(k) <= 1) {
                    add(plan, world, cx + sign * r, fy + 5, cz + k, roof);
                    add(plan, world, cx + k, fy + 5, cz + sign * r, roof);
                }
            }
            add(plan, world, cx + sign * r, fy + 4, cz, lanternHanging);
            add(plan, world, cx, fy + 4, cz + sign * r, lanternHanging);
        }

        // 4) Huit lampadaires
        for (int k = 0; k < 8; k++) {
            double angle = Math.toRadians(22.5 + 45.0 * k);
            int px = (int) Math.round((r - 2) * Math.cos(angle));
            int pz = (int) Math.round((r - 2) * Math.sin(angle));
            for (int dy = 1; dy <= 3; dy++) {
                add(plan, world, cx + px, fy + dy, cz + pz, lampPost);
            }
            add(plan, world, cx + px, fy + 4, cz + pz, lanternStanding);
        }

        // 5) Quatre pavillons
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int px = cx + sx * pavilion;
                int pz = cz + sz * pavilion;
                for (int ox = -2; ox <= 2; ox++) {
                    for (int oz = -2; oz <= 2; oz++) {
                        add(plan, world, px + ox, fy, pz + oz, pavilionFloor);
                    }
                }
                for (int[] corner : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
                    for (int dy = 1; dy <= 4; dy++) {
                        add(plan, world, px + corner[0], fy + dy, pz + corner[1], post);
                    }
                }
                for (int ox = -3; ox <= 3; ox++) {
                    for (int oz = -3; oz <= 3; oz++) {
                        add(plan, world, px + ox, fy + 5, pz + oz, roofSlab);
                    }
                }
                for (int ox = -2; ox <= 2; ox++) {
                    for (int oz = -2; oz <= 2; oz++) {
                        add(plan, world, px + ox, fy + 5, pz + oz, roof);
                    }
                }
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        add(plan, world, px + ox, fy + 6, pz + oz, roofSlab);
                    }
                }
                add(plan, world, px, fy + 4, pz, lanternHanging);
            }
        }

        // 6) Parterres d'herbe pour les cerisiers
        int t = Math.round(r * 0.65f);
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        add(plan, world, cx + sx * t + ox, fy, cz + sz * t + oz, grass);
                    }
                }
                treeSpots.add(new int[]{sx * t, sz * t});
            }
        }
    }
}

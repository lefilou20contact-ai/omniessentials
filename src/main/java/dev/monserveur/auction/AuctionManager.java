package dev.monserveur.auction;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hôtel des ventes entre joueurs. Les annonces sont dans auctions.yml.
 * Les objets à récupérer (achats livrés hors-ligne, annonces expirées ou annulées)
 * attendent dans une boîte de réception par joueur (mailbox.yml), ouverte avec /ah collect.
 */
public final class AuctionManager implements Listener {

    private final MonServeurPlugin plugin;
    private final File auctionsFile;
    private final File mailboxFile;
    private final Map<String, AuctionListing> listings = new LinkedHashMap<>();
    // boîte de réception : uuid du joueur -> liste d'items en attente
    private final Map<UUID, List<ItemStack>> mailbox = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();
    private final java.util.Set<UUID> awaitingPrice = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public AuctionManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
        this.auctionsFile = new File(plugin.getDataFolder(), "auctions.yml");
        this.mailboxFile = new File(plugin.getDataFolder(), "mailbox.yml");
        load();
    }

    // ------------------------------------------------------------------ réglages

    private int durationHours() {
        return Math.max(1, plugin.getConfig().getInt("auction.listing-duration-hours", 72));
    }

    private int maxActive() {
        return Math.max(1, plugin.getConfig().getInt("auction.max-active-listings", 3));
    }

    private double feePercent() {
        return Math.max(0, plugin.getConfig().getDouble("auction.fee-percent", 5));
    }

    public double minPrice() {
        return plugin.getConfig().getDouble("auction.min-price", 10);
    }

    public double maxPrice() {
        return plugin.getConfig().getDouble("auction.max-price", 1_000_000);
    }

    // ------------------------------------------------------------------ mise en vente via le chat

    /** Demande le prix dans le chat (utilisé par le bouton "Vendre" du menu). */
    public void startSellPrompt(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            plugin.msg(player, "shop-no-items");
            return;
        }
        awaitingPrice.add(player.getUniqueId());
        plugin.msg(player, "ah-sell-prompt",
                "min", plugin.economy().format(minPrice()), "max", plugin.economy().format(maxPrice()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingPrice.remove(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (raw.equalsIgnoreCase("annuler") || raw.equalsIgnoreCase("cancel")) {
            plugin.msg(player, "ah-sell-cancelled");
            return;
        }
        Double price = plugin.economy().parseAmount(raw, false);
        if (price == null) {
            plugin.msg(player, "invalid-amount");
            return;
        }
        // Modifier l'inventaire doit se faire sur le thread principal (le chat est asynchrone).
        Bukkit.getScheduler().runTask(plugin, () -> list(player, price));
    }

    // ------------------------------------------------------------------ persistance

    private void load() {
        listings.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(auctionsFile);
        ConfigurationSection section = yaml.getConfigurationSection("listings");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection c = section.getConfigurationSection(id);
                if (c == null) {
                    continue;
                }
                ItemStack item = decode(c.getString("item"));
                if (item == null) {
                    continue;
                }
                try {
                    UUID seller = UUID.fromString(c.getString("seller", ""));
                    listings.put(id, new AuctionListing(id, seller, c.getString("seller-name", "?"),
                            item, c.getDouble("price"), c.getLong("created"), c.getLong("expires")));
                } catch (IllegalArgumentException ignored) {
                    // uuid invalide, entrée ignorée
                }
            }
        }

        mailbox.clear();
        YamlConfiguration mail = YamlConfiguration.loadConfiguration(mailboxFile);
        ConfigurationSection mailSection = mail.getConfigurationSection("mailbox");
        if (mailSection != null) {
            for (String uuidStr : mailSection.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(uuidStr);
                    List<String> encoded = mail.getStringList("mailbox." + uuidStr);
                    List<ItemStack> items = new ArrayList<>();
                    for (String e : encoded) {
                        ItemStack item = decode(e);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                    if (!items.isEmpty()) {
                        mailbox.put(id, items);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (AuctionListing listing : listings.values()) {
            String path = "listings." + listing.id;
            yaml.set(path + ".seller", listing.seller.toString());
            yaml.set(path + ".seller-name", listing.sellerName);
            yaml.set(path + ".item", encode(listing.item));
            yaml.set(path + ".price", listing.price);
            yaml.set(path + ".created", listing.created);
            yaml.set(path + ".expires", listing.expires);
        }
        try {
            yaml.save(auctionsFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Impossible de sauvegarder auctions.yml : " + ex.getMessage());
        }
        saveMailbox();
    }

    private void saveMailbox() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, List<ItemStack>> entry : mailbox.entrySet()) {
            List<String> encoded = new ArrayList<>();
            for (ItemStack item : entry.getValue()) {
                encoded.add(encode(item));
            }
            yaml.set("mailbox." + entry.getKey(), encoded);
        }
        try {
            yaml.save(mailboxFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Impossible de sauvegarder mailbox.yml : " + ex.getMessage());
        }
    }

    private static String encode(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    private static ItemStack decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ annonces

    public List<AuctionListing> active() {
        List<AuctionListing> list = new ArrayList<>(listings.values());
        list.sort(Comparator.comparingLong((AuctionListing l) -> l.created).reversed());
        return list;
    }

    public List<AuctionListing> byPlayer(UUID uuid) {
        List<AuctionListing> list = new ArrayList<>();
        for (AuctionListing listing : listings.values()) {
            if (listing.seller.equals(uuid)) {
                list.add(listing);
            }
        }
        return list;
    }

    public int countActive(UUID uuid) {
        return byPlayer(uuid).size();
    }

    public AuctionListing get(String id) {
        return listings.get(id);
    }

    /** Met en vente l'objet en main du joueur. */
    public boolean list(Player seller, double price) {
        if (countActive(seller.getUniqueId()) >= maxActive()) {
            plugin.msg(seller, "ah-limit-reached", "max", maxActive());
            return false;
        }
        ItemStack hand = seller.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            plugin.msg(seller, "shop-no-items");
            return false;
        }
        if (price < minPrice() || price > maxPrice()) {
            plugin.msg(seller, "ah-price-out-of-range",
                    "min", plugin.economy().format(minPrice()), "max", plugin.economy().format(maxPrice()));
            return false;
        }
        String id = "l" + System.currentTimeMillis() + counter.incrementAndGet();
        ItemStack copy = hand.clone();
        long now = System.currentTimeMillis();
        listings.put(id, new AuctionListing(id, seller.getUniqueId(), seller.getName(), copy, price,
                now, now + durationHours() * 3_600_000L));
        seller.getInventory().setItemInMainHand(null);
        save();
        plugin.msg(seller, "ah-listed", "price", plugin.economy().format(price));
        return true;
    }

    public void buy(Player buyer, String id) {
        AuctionListing listing = listings.get(id);
        if (listing == null) {
            plugin.msg(buyer, "ah-not-found");
            return;
        }
        if (listing.seller.equals(buyer.getUniqueId())) {
            plugin.msg(buyer, "ah-own-listing");
            return;
        }
        PlayerData buyerData = plugin.data().get(buyer);
        if (!plugin.economy().withdraw(buyerData, listing.price)) {
            plugin.msg(buyer, "not-enough-money", "amount", plugin.economy().format(listing.price));
            return;
        }
        listings.remove(id);

        double fee = listing.price * feePercent() / 100.0;
        double earned = listing.price - fee;
        PlayerData sellerData = plugin.data().getOrLoad(listing.seller, listing.sellerName);
        plugin.economy().deposit(sellerData, earned);
        plugin.data().save(sellerData);

        deliverOrMailbox(buyer, listing.item);
        save();

        plugin.msg(buyer, "ah-bought",
                "item", listing.item.getType().name(),
                "price", plugin.economy().format(listing.price));
        Player online = Bukkit.getPlayer(listing.seller);
        if (online != null) {
            plugin.msg(online, "ah-sold", "player", buyer.getName(), "amount", plugin.economy().format(earned));
        }
    }

    public boolean cancel(Player seller, String id) {
        AuctionListing listing = listings.get(id);
        if (listing == null || !listing.seller.equals(seller.getUniqueId())) {
            plugin.msg(seller, "ah-not-found");
            return false;
        }
        listings.remove(id);
        deliverOrMailbox(seller, listing.item);
        save();
        plugin.msg(seller, "ah-cancelled");
        return true;
    }

    /** Vérifie les annonces expirées (appelée périodiquement). */
    public void checkExpired() {
        boolean changed = false;
        for (AuctionListing listing : new ArrayList<>(listings.values())) {
            if (listing.expired()) {
                listings.remove(listing.id);
                addToMailbox(listing.seller, listing.item);
                Player online = Bukkit.getPlayer(listing.seller);
                if (online != null) {
                    plugin.msg(online, "ah-expired-returned", "item", listing.item.getType().name());
                }
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    // ------------------------------------------------------------------ boîte de réception

    private void deliverOrMailbox(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        if (!leftover.isEmpty()) {
            for (ItemStack over : leftover.values()) {
                addToMailbox(player.getUniqueId(), over);
            }
            plugin.msg(player, "ah-mailbox-full-inventory");
        }
    }

    private void addToMailbox(UUID uuid, ItemStack item) {
        mailbox.computeIfAbsent(uuid, k -> new ArrayList<>()).add(item.clone());
        saveMailbox();
    }

    public List<ItemStack> mailboxOf(Player player) {
        return mailbox.getOrDefault(player.getUniqueId(), List.of());
    }

    public void collectOne(Player player, int index) {
        List<ItemStack> items = mailbox.get(player.getUniqueId());
        if (items == null || index < 0 || index >= items.size()) {
            return;
        }
        ItemStack item = items.get(index);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        if (leftover.isEmpty()) {
            items.remove(index);
            if (items.isEmpty()) {
                mailbox.remove(player.getUniqueId());
            }
            saveMailbox();
        } else {
            plugin.msg(player, "inventory-full");
        }
    }

    public void collectAll(Player player) {
        List<ItemStack> items = mailbox.get(player.getUniqueId());
        if (items == null || items.isEmpty()) {
            plugin.msg(player, "ah-mailbox-empty");
            return;
        }
        List<ItemStack> remaining = new ArrayList<>();
        int given = 0;
        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            if (leftover.isEmpty()) {
                given++;
            } else {
                remaining.add(item);
            }
        }
        if (remaining.isEmpty()) {
            mailbox.remove(player.getUniqueId());
        } else {
            mailbox.put(player.getUniqueId(), remaining);
        }
        saveMailbox();
        plugin.msg(player, "ah-collected", "amount", given);
    }
}

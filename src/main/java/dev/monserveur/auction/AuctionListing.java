package dev.monserveur.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class AuctionListing {

    public final String id;
    public final UUID seller;
    public String sellerName;
    public ItemStack item;
    public double price;
    public long created;
    public long expires;

    public AuctionListing(String id, UUID seller, String sellerName, ItemStack item, double price, long created, long expires) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.created = created;
        this.expires = expires;
    }

    public boolean expired() {
        return System.currentTimeMillis() >= expires;
    }
}

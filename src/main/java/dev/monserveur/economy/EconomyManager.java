package dev.monserveur.economy;

import dev.monserveur.MonServeurPlugin;
import dev.monserveur.data.PlayerData;
import dev.monserveur.util.Text;

public final class EconomyManager {

    private static final double MAX_AMOUNT = 1_000_000_000_000.0;

    private final MonServeurPlugin plugin;

    public EconomyManager(MonServeurPlugin plugin) {
        this.plugin = plugin;
    }

    public double startingBalance() {
        return plugin.getConfig().getDouble("economy.starting-balance", 100);
    }

    public String format(double amount) {
        return Text.number(amount) + " " + plugin.getConfig().getString("economy.currency-symbol", "$");
    }

    public void deposit(PlayerData data, double amount) {
        data.setBalance(round(data.getBalance() + amount));
    }

    /** Retire l'argent si le joueur en a assez. */
    public boolean withdraw(PlayerData data, double amount) {
        if (data.getBalance() + 0.001 < amount) {
            return false;
        }
        data.setBalance(Math.max(0, round(data.getBalance() - amount)));
        return true;
    }

    /** Lit un montant saisi par un joueur. Retourne null si invalide. */
    public Double parseAmount(String input, boolean allowZero) {
        try {
            double value = Double.parseDouble(input.replace(',', '.'));
            if (!Double.isFinite(value) || value > MAX_AMOUNT || value < 0) {
                return null;
            }
            value = round(value);
            if (value <= 0 && !allowZero) {
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

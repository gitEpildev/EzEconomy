package com.skyblockexp.ezeconomy.manager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BankInterestManager {
    // plugin reference is obtained from Registry when needed
    private int taskId = -1;

    // THREAD SAFETY NOTE:
    // payInterestToAll() is called from a BukkitRunnable (main server thread),
    // so storage operations are not concurrent by default. However, if storage
    // providers are accessed from async tasks elsewhere, or if future changes
    // introduce async interest payout, all storage operations here must be thread-safe.
    //
    // If you plan to run payInterestToAll() asynchronously, ensure:
    // 1. All storageProvider methods (get/setBalance, getBankBalance, getBankMembers, etc.) are thread-safe.
    // 2. Use synchronization or locks if the underlying storage is not thread-safe.
    // 3. Consider using Bukkit's scheduler to run only thread-safe code async, and all Bukkit API calls sync.

    public BankInterestManager(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(long intervalTicks) {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                payInterestToAll();
            }
        }.runTaskTimer(com.skyblockexp.ezeconomy.core.Registry.getPlugin(), intervalTicks, intervalTicks).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void payInterestToAll() {
        com.skyblockexp.ezeconomy.api.storage.StorageProvider storage = com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.api.storage.StorageProvider.class);
        if (storage == null) {
            return;
        }
        org.bukkit.configuration.file.FileConfiguration config = com.skyblockexp.ezeconomy.core.Registry.getPlugin().getConfig();
        boolean multiEnabled = config.getBoolean("multi-currency.enabled", false);
        Set<String> currencies;
        if (multiEnabled) {
            var section = config.getConfigurationSection("multi-currency.currencies");
            currencies = section != null ? section.getKeys(false) : java.util.Collections.singleton("dollar");
        } else {
            currencies = java.util.Collections.singleton("dollar");
        }
        for (String currency : currencies) {
            for (String bank : storage.getBanks()) {
                double bankBalance = storage.getBankBalance(bank, currency);
                Set<UUID> members = storage.getBankMembers(bank);
                if (members == null || members.isEmpty()) continue;
                double grossInterest = calculateInterest(bankBalance);
                double perMemberInterest = grossInterest / members.size();
                for (UUID uuid : members) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    if (perMemberInterest > 0) {
                        // If storage.setBalance is not thread-safe, synchronize here or in the provider.
                        storage.setBalance(uuid, currency, storage.getBalance(uuid, currency) + perMemberInterest);
                        if (player.isOnline()) {
                            String formatted = com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.manager.CurrencyManager.class).format(perMemberInterest, currency);
                            player.getPlayer().sendMessage("You received " + formatted + " " + currency + " interest from bank '" + bank + "'");
                        }
                    }
                }
            }
        }
    }

    // Example interest calculation (1% per payout)
    private double calculateInterest(double balance) {
        return Math.round(balance * 0.01 * 100.0) / 100.0;
    }
}

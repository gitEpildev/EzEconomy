package com.skyblockexp.ezeconomy.gui;

import org.bukkit.entity.Player;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BalanceAction extends GuiAction {
    public BalanceAction() {
        super("balance", "\u00A7aBalance");
    }

    @Override
    public void open(EzEconomyPlugin plugin, Player player) {
        StorageProvider storage = com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.api.storage.StorageProvider.class);
        if (storage == null) {
            return;
        }
        var config = com.skyblockexp.ezeconomy.core.Registry.getPlugin().getConfig();
        Map<String, Double> currencies = new HashMap<>();
        Map<String, Object> currencySection = config.getConfigurationSection("multi-currency.currencies") != null
            ? config.getConfigurationSection("multi-currency.currencies").getValues(false)
            : Collections.emptyMap();
        if (config.getBoolean("multi-currency.enabled", false) && !currencySection.isEmpty()) {
            for (String currency : currencySection.keySet()) {
                double balance = storage.getBalance(player.getUniqueId(), currency);
                currencies.put(currency, balance);
            }
        } else {
            String currency = com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.manager.CurrencyManager.class).getDefaultCurrency();
            double balance = storage.getBalance(player.getUniqueId(), currency);
            currencies.put(currency, balance);
        }
        Map<String, Double> banks = new HashMap<>();
        for (String bank : storage.getBanks()) {
            if (storage.isBankMember(bank, player.getUniqueId())) {
                if (config.getBoolean("multi-currency.enabled", false) && !currencySection.isEmpty()) {
                    for (String currency : currencySection.keySet()) {
                        double bankBalance = storage.getBankBalance(bank, currency);
                        banks.put(bank + " (" + currency + ")", bankBalance);
                    }
                } else {
                    String currency = com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.manager.CurrencyManager.class).getDefaultCurrency();
                    double bankBalance = storage.getBankBalance(bank, currency);
                    banks.put(bank, bankBalance);
                }
            }
        }
        BalanceGui.open(plugin, player, currencies, banks);
    }
}

package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.Registry;

public class ManagersComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public ManagersComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        // Initialize managers previously owned by the main plugin class
        com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager pref = new com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager(plugin);
        com.skyblockexp.ezeconomy.manager.CurrencyManager cm = new com.skyblockexp.ezeconomy.manager.CurrencyManager(plugin);
        com.skyblockexp.ezeconomy.manager.BankInterestManager bank = new com.skyblockexp.ezeconomy.manager.BankInterestManager(plugin);
        long interval = plugin.getConfig().getLong("bank-interest-interval-ticks", 72_000L);
        bank.start(interval);
        com.skyblockexp.ezeconomy.manager.DailyRewardManager drm = new com.skyblockexp.ezeconomy.manager.DailyRewardManager(plugin);
        com.skyblockexp.ezeconomy.gui.PayFlowManager pfm = new com.skyblockexp.ezeconomy.gui.PayFlowManager();

        // Register services in Registry for centralized access
        Registry.register(com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager.class, pref);
        Registry.register(com.skyblockexp.ezeconomy.manager.CurrencyManager.class, cm);
        Registry.register(com.skyblockexp.ezeconomy.manager.BankInterestManager.class, bank);
        Registry.register(com.skyblockexp.ezeconomy.manager.DailyRewardManager.class, drm);
        Registry.register(com.skyblockexp.ezeconomy.gui.PayFlowManager.class, pfm);
    }

    @Override
    public void stop() {
        com.skyblockexp.ezeconomy.manager.BankInterestManager bank = Registry.get(com.skyblockexp.ezeconomy.manager.BankInterestManager.class);
        if (bank != null) {
            try { bank.stop(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void reload() {
        // re-initialize managers if required
        plugin.initializeManagers();
    }
}

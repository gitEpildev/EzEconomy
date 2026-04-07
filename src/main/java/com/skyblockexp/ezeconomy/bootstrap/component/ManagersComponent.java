package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class ManagersComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public ManagersComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        initManagers();
    }

    @Override
    public void stop() {
        if (plugin.getBankInterestManager() != null) {
            try { plugin.getBankInterestManager().stop(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void reload() {
        // Re-initialize managers in-place; keep lifecycle inside this component
        initManagers();
    }

    private void initManagers() {
        // Initialize managers previously owned by the main plugin class
        com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager pref = new com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager(plugin);
        com.skyblockexp.ezeconomy.manager.CurrencyManager cm = new com.skyblockexp.ezeconomy.manager.CurrencyManager(plugin);
        com.skyblockexp.ezeconomy.manager.BankInterestManager bank = null;
        boolean bankingEnabled = plugin.getConfig().getBoolean("banking.enabled", true);
        if (bankingEnabled) {
            bank = new com.skyblockexp.ezeconomy.manager.BankInterestManager(plugin);
            long interval = plugin.getConfig().getLong("bank-interest-interval-ticks", 72_000L);
            bank.start(interval);
        }
        com.skyblockexp.ezeconomy.manager.DailyRewardManager drm = new com.skyblockexp.ezeconomy.manager.DailyRewardManager(plugin);
        com.skyblockexp.ezeconomy.gui.PayFlowManager pfm = new com.skyblockexp.ezeconomy.gui.PayFlowManager();

        plugin.setCurrencyPreferenceManager(pref);
        plugin.setCurrencyManager(cm);
        plugin.setBankInterestManager(bank);
        plugin.setDailyRewardManager(drm);
        plugin.setPayFlowManager(pfm);
    }
}

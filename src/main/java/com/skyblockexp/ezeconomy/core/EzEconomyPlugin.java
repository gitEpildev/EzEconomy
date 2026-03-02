package com.skyblockexp.ezeconomy.core;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.command.BalanceCommand;
import com.skyblockexp.ezeconomy.command.BaltopCommand;
import com.skyblockexp.ezeconomy.command.BankCommand;
import com.skyblockexp.ezeconomy.command.CurrencyCommand;
import com.skyblockexp.ezeconomy.command.EcoCommand;
import com.skyblockexp.ezeconomy.command.EzEconomyCommand;
import com.skyblockexp.ezeconomy.command.PayCommand;
import com.skyblockexp.ezeconomy.listener.DailyRewardListener;
import com.skyblockexp.ezeconomy.gui.GuiListener;
import com.skyblockexp.ezeconomy.manager.BankInterestManager;
import com.skyblockexp.ezeconomy.manager.CurrencyManager;
import com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager;
import com.skyblockexp.ezeconomy.manager.DailyRewardManager;
import com.skyblockexp.ezeconomy.storage.MongoDBStorageProvider;
import com.skyblockexp.ezeconomy.storage.MySQLStorageProvider;
import com.skyblockexp.ezeconomy.storage.SQLiteStorageProvider;
import com.skyblockexp.ezeconomy.storage.YMLStorageProvider;
import com.skyblockexp.ezeconomy.tabcomplete.BankTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.CurrencyTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.EcoTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.EzEconomyCommandTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.PayTabCompleter;
import com.skyblockexp.ezeconomy.update.SpigotUpdateChecker;
import com.skyblockexp.ezeconomy.placeholder.EzEconomyPlaceholderExpansion;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import com.skyblockexp.ezeconomy.core.Registry;

public class EzEconomyPlugin extends JavaPlugin {
    private static final int SPIGOT_RESOURCE_ID = 130975;
    private static final long DEFAULT_INTEREST_INTERVAL_TICKS = 72_000L;
        private static final List<String> DEFAULT_CONFIGS = List.of(
            "config-yml.yml",
            "config-mysql.yml",
            "config-sqlite.yml",
            "config-mongodb.yml",
            "languages/en.yml",
            "languages/nl.yml",
            "languages/es.yml",
            "languages/fr.yml",
            "languages/zh.yml",
            "user-gui.yml"
        );

    private StorageProvider storage;
    private boolean storageWarningLogged;
    private CurrencyPreferenceManager currencyPreferenceManager;
    private CurrencyManager currencyManager;
    private EzEconomyMetrics metrics;
    private BankInterestManager bankInterestManager;
    private DailyRewardManager dailyRewardManager;
    private MessageProvider messageProvider;
    private VaultEconomyImpl vaultEconomy;
    private FileConfiguration messagesConfig;
    private FileConfiguration userGuiConfig;
    private com.skyblockexp.ezeconomy.gui.PayFlowManager payFlowManager;
    private com.skyblockexp.ezeconomy.bootstrap.Bootstrap bootstrap;

    public String format(double amount) {
        com.skyblockexp.ezeconomy.manager.CurrencyManager cm = Registry.get(com.skyblockexp.ezeconomy.manager.CurrencyManager.class);
        return cm == null ? String.valueOf(amount) : cm.format(amount);
    }

    /**
     * Format an amount for a specific currency using configured decimals and symbol.
     */
    public String format(double amount, String currency) {
        com.skyblockexp.ezeconomy.manager.CurrencyManager cm = Registry.get(com.skyblockexp.ezeconomy.manager.CurrencyManager.class);
        return cm == null ? String.valueOf(amount) : cm.format(amount, currency);
    }

    public VaultEconomyImpl getEconomy() {
        return Registry.get(VaultEconomyImpl.class);
    }

    @Override
    public void onEnable() {
        Registry.initialize(this);
        this.bootstrap = new com.skyblockexp.ezeconomy.bootstrap.Bootstrap(this);
        try {
            this.bootstrap.start();
            new SpigotUpdateChecker(this, SPIGOT_RESOURCE_ID).checkForUpdates();
            getLogger().info("EzEconomy enabled and registered as Vault provider.");
        } catch (RuntimeException ex) {
            getLogger().severe("Bootstrap failed: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public EzEconomyMetrics getMetrics() {
        return Registry.get(EzEconomyMetrics.class);
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stop();
        } else {
            Bukkit.getServicesManager().unregister(Economy.class, vaultEconomy);
        }
        // Ensure any registered managers/services are shut down.
        try {
            Registry.shutdownAll();
        } catch (Exception ignored) {}
        getLogger().info("EzEconomy disabled.");
    }

    public MessageProvider getMessageProvider() {
        return Registry.get(MessageProvider.class);
    }
    public void loadMessageProvider() {
        // Delegate runtime reload of messages to the ConfigComponent implementation.
        new com.skyblockexp.ezeconomy.bootstrap.component.ConfigComponent(this).start();
    }

    public VaultEconomyImpl getVaultEconomy() {
        return Registry.get(VaultEconomyImpl.class);
    }
    
    public BankInterestManager getBankInterestManager() {
        return bankInterestManager;
    }

    /**
     * Returns the default currency as defined in config or "dollar" if not set.
     */
    public String getDefaultCurrency() {
        CurrencyManager cm = Registry.get(CurrencyManager.class);
        return cm == null ? "dollar" : cm.getDefaultCurrency();
    }

    /**
     * Returns the CurrencyManager instance.
     */
    public com.skyblockexp.ezeconomy.manager.CurrencyManager getCurrencyManager() {
        return Registry.get(CurrencyManager.class);
    }

    /**
     * Returns the storage provider, logging a warning if not available.
     */
    public StorageProvider getStorageOrWarn() {
        StorageProvider sp = Registry.get(StorageProvider.class);
        if (sp == null && !storageWarningLogged) {
            getLogger().warning("Storage provider is not initialized!");
            storageWarningLogged = true;
        }
        return sp;
    }

    /**
     * Returns the storage provider (may be null if not initialized).
     */
    public StorageProvider getStorage() {
        return Registry.get(StorageProvider.class);
    }

    /**
     * Returns the CurrencyPreferenceManager instance.
     */
    public CurrencyPreferenceManager getCurrencyPreferenceManager() {
        return Registry.get(CurrencyPreferenceManager.class);
    }

    /**
     * Logs a transaction using the storage provider.
     */
    public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction transaction) {
        StorageProvider sp = Registry.get(StorageProvider.class);
        if (sp != null) sp.logTransaction(transaction);
    }

    /**
     * Retrieves transaction history for a player and currency.
     */
    public java.util.List<com.skyblockexp.ezeconomy.api.storage.models.Transaction> getTransactions(java.util.UUID uuid, String currency) {
        StorageProvider sp = Registry.get(StorageProvider.class);
        if (sp != null) return sp.getTransactions(uuid, currency);
        return java.util.Collections.emptyList();
    }

    public void ensureDefaultConfigs() {
        // Default config/resource creation moved to ConfigComponent during bootstrap.
        // Retained for compatibility; no-op here.
    }

    public void loadMessages() {
        // Message provider initialization moved to ConfigComponent during bootstrap.
        // This method remains for runtime reloads and is intentionally a no-op here.
    }

    public void setMessagesConfig(FileConfiguration messagesConfig) {
        this.messagesConfig = messagesConfig;
    }

    public void setMessageProvider(MessageProvider provider) {
        Registry.register(MessageProvider.class, provider);
    }

    public boolean initializeStorage() {
        // Storage initialization moved to StorageComponent during bootstrap.
        // Keep method for compatibility; actual initialization is no-op here.
        return Registry.get(StorageProvider.class) != null;
    }

    public YamlConfiguration loadStorageConfig(String fileName) {
        File file = new File(getDataFolder(), fileName);
        return YamlConfiguration.loadConfiguration(file);
    }

    public void setStorage(StorageProvider provider) {
        Registry.register(StorageProvider.class, provider);
    }

    public void initializeManagers() {
        // Manager initialization moved to ManagersComponent during bootstrap.
        new com.skyblockexp.ezeconomy.bootstrap.component.ManagersComponent(this).start();
    }

    public void setCurrencyPreferenceManager(CurrencyPreferenceManager m) {
        Registry.register(CurrencyPreferenceManager.class, m);
    }

    public void setCurrencyManager(CurrencyManager m) {
        Registry.register(CurrencyManager.class, m);
    }

    public void setBankInterestManager(BankInterestManager m) {
        Registry.register(BankInterestManager.class, m);
    }

    public void setDailyRewardManager(DailyRewardManager m) {
        Registry.register(DailyRewardManager.class, m);
    }

    public void setPayFlowManager(com.skyblockexp.ezeconomy.gui.PayFlowManager m) {
        Registry.register(com.skyblockexp.ezeconomy.gui.PayFlowManager.class, m);
    }

    public com.skyblockexp.ezeconomy.gui.PayFlowManager getPayFlowManager() {
        return Registry.get(com.skyblockexp.ezeconomy.gui.PayFlowManager.class);
    }

    public DailyRewardManager getDailyRewardManager() {
        return Registry.get(DailyRewardManager.class);
    }

    public void setMetrics(EzEconomyMetrics metrics) {
        Registry.register(EzEconomyMetrics.class, metrics);
    }

    public void registerEconomy() {
        // Economy registration moved to EconomyComponent during bootstrap.
        new com.skyblockexp.ezeconomy.bootstrap.component.EconomyComponent(this).start();
    }

    public void setVaultEconomy(VaultEconomyImpl impl) {
        Registry.register(VaultEconomyImpl.class, impl);
    }

    public void registerCommands() {
        // Command registration moved to CommandsComponent during bootstrap.
        new com.skyblockexp.ezeconomy.bootstrap.component.CommandsComponent(this).start();
    }

    public void registerListeners() {
        // Listener registration moved to ListenersComponent during bootstrap.
        new com.skyblockexp.ezeconomy.bootstrap.component.ListenersComponent(this).start();
    }

    public void loadUserGuiConfig() {
        // Gui loading moved to GuiComponent during bootstrap.
        // Delegate runtime reload to GuiComponent implementation.
        new com.skyblockexp.ezeconomy.bootstrap.component.GuiComponent(this).start();
    }

    public FileConfiguration getUserGuiConfig() {
        return this.userGuiConfig == null ? YamlConfiguration.loadConfiguration(new File(getDataFolder(), "user-gui.yml")) : this.userGuiConfig;
    }

    public void setUserGuiConfig(FileConfiguration cfg) {
        this.userGuiConfig = cfg;
    }

    public void registerPlaceholderExpansion() {
        // Placeholder registration moved to PlaceholderComponent during bootstrap.
        new com.skyblockexp.ezeconomy.bootstrap.component.PlaceholderComponent(this).start();
    }
}

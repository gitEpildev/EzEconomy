package com.skyblockexp.ezeconomy.core;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.command.BalanceCommand;
import com.skyblockexp.ezeconomy.command.BaltopCommand;
import com.skyblockexp.ezeconomy.command.BankCommand;
import com.skyblockexp.ezeconomy.command.CurrencyCommand;
import com.skyblockexp.ezeconomy.command.EcoCommand;
import com.skyblockexp.ezeconomy.command.EzEconomyCommand;
import com.skyblockexp.ezeconomy.command.PayCommand;
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
    private com.skyblockexp.ezeconomy.lock.LockManager lockManager;
    private static EzEconomyPlugin INSTANCE;
    // Metrics counters (stored as cents to avoid floating point drift)
    private final java.util.concurrent.atomic.AtomicLong totalDepositedCents = new java.util.concurrent.atomic.AtomicLong(0L);
    private final java.util.concurrent.atomic.AtomicLong totalWithdrawnCents = new java.util.concurrent.atomic.AtomicLong(0L);
    private final java.util.concurrent.atomic.AtomicLong totalConvertedCents = new java.util.concurrent.atomic.AtomicLong(0L);

    public String format(double amount) {
        return format(amount, getDefaultCurrency());
    }

    /**
     * Format an amount for a specific currency using configured decimals and symbol.
     */
    public String format(double amount, String currency) {
        if (currency == null) {
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault());
            nf.setGroupingUsed(true);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(amount);
        }
        var cfg = getConfig();
        if (cfg.getConfigurationSection("multi-currency.currencies") == null) {
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault());
            nf.setGroupingUsed(true);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(amount);
        }
        String key = currency.toLowerCase();
        String symbol = cfg.getString("multi-currency.currencies." + key + ".symbol", "");
        int decimals = cfg.getInt("multi-currency.currencies." + key + ".decimals", 2);

        // Locale configuration: server-wide override optional
        String localeCfg = cfg.getString("currency.format.locale", "");
        java.util.Locale locale = java.util.Locale.getDefault();
        if (localeCfg != null && !localeCfg.isBlank()) {
            String[] parts = localeCfg.split("[_-]");
            if (parts.length == 1) locale = new java.util.Locale(parts[0]);
            else locale = new java.util.Locale(parts[0], parts[1]);
        }

        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(locale);
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(decimals);
        nf.setMaximumFractionDigits(decimals);
        String formatted = nf.format(java.math.BigDecimal.valueOf(amount).setScale(decimals, java.math.RoundingMode.HALF_UP));

        // Symbol placement: optional per-currency setting (prefix/suffix)
        String placement = cfg.getString("multi-currency.currencies." + key + ".symbol_placement", "suffix").toLowerCase();
        boolean prefix = placement.equals("prefix") || placement.equals("before");
        if (symbol == null || symbol.isEmpty()) {
            return formatted;
        }
        return prefix ? (symbol + " " + formatted) : (formatted + " " + symbol);
    }

    /**
     * Format only the numeric amount part (no currency symbol or placement).
     * This preserves locale and decimals but excludes the symbol so message
     * templates can control placement.
     */
    public String formatAmountOnly(double amount, String currency) {
        if (currency == null) {
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault());
            nf.setGroupingUsed(true);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(amount);
        }
        var cfg = getConfig();
        if (cfg.getConfigurationSection("multi-currency.currencies") == null) {
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault());
            nf.setGroupingUsed(true);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(amount);
        }
        String key = currency.toLowerCase();
        int decimals = cfg.getInt("multi-currency.currencies." + key + ".decimals", 2);

        // Locale configuration: server-wide override optional
        String localeCfg = cfg.getString("currency.format.locale", "");
        java.util.Locale locale = java.util.Locale.getDefault();
        if (localeCfg != null && !localeCfg.isBlank()) {
            String[] parts = localeCfg.split("[_-]");
            if (parts.length == 1) locale = new java.util.Locale(parts[0]);
            else locale = new java.util.Locale(parts[0], parts[1]);
        }

        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(locale);
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(decimals);
        nf.setMaximumFractionDigits(decimals);
        String formatted = nf.format(java.math.BigDecimal.valueOf(amount).setScale(decimals, java.math.RoundingMode.HALF_UP));
        return formatted;
    }

    /**
     * Format an amount using short suffixes (k, m, b, t) while preserving
     * currency symbol placement according to config. Delegates numeric
     * shortening to NumberUtil.formatShort.
     */
    public String formatShort(double amount, String currency) {
        var cfg = getConfig();
        boolean enabled = cfg.getBoolean("currency.format.short.enabled", true);
        double threshold = cfg.getDouble("currency.format.short.threshold", 1000.0);
        // If short-format is disabled or amount below threshold, fall back to full format
        if (!enabled || Math.abs(amount) < threshold) {
            return format(amount, currency);
        }

        // global default decimals
        int decimals = cfg.getInt("currency.format.short.decimals", 1);
        // prefer per-currency override if present
        if (currency != null && cfg.getConfigurationSection("multi-currency.currencies") != null) {
            String perKey = "multi-currency.currencies." + currency.toLowerCase() + ".short.decimals";
            if (cfg.contains(perKey)) {
                decimals = cfg.getInt(perKey, decimals);
            }
        }

        // short.enabled and short.threshold may be configured per-currency; evaluate them
        boolean enabled = cfg.getBoolean("currency.format.short.enabled", true);
        double threshold = cfg.getDouble("currency.format.short.threshold", 1000.0);
        if (currency != null && cfg.getConfigurationSection("multi-currency.currencies") != null) {
            String enabledKey = "multi-currency.currencies." + currency.toLowerCase() + ".short.enabled";
            String thresholdKey = "multi-currency.currencies." + currency.toLowerCase() + ".short.threshold";
            if (cfg.contains(enabledKey)) {
                enabled = cfg.getBoolean(enabledKey, enabled);
            }
            if (cfg.contains(thresholdKey)) {
                threshold = cfg.getDouble(thresholdKey, threshold);
            }
        }

        // If short-format is disabled or amount below threshold, fall back to full format
        if (!enabled || Math.abs(amount) < threshold) {
            return format(amount, currency);
        }

        String numeric = com.skyblockexp.ezeconomy.util.NumberUtil.formatShort(java.math.BigDecimal.valueOf(amount), decimals);
        if (currency == null) return numeric;
        if (cfg.getConfigurationSection("multi-currency.currencies") == null) {
            return numeric;
        }
        String key = currency.toLowerCase();
        String symbol = cfg.getString("multi-currency.currencies." + key + ".symbol", "");
        String placement = cfg.getString("multi-currency.currencies." + key + ".symbol_placement", "suffix").toLowerCase();
        boolean prefix = placement.equals("prefix") || placement.equals("before");
        if (symbol == null || symbol.isEmpty()) {
            return numeric;
        }
        return prefix ? (symbol + " " + numeric) : (numeric + " " + symbol);
    }

    /**
     * Get the raw currency symbol for a currency key (no surrounding whitespace).
     */
    public String getCurrencySymbol(String currency) {
        if (currency == null) return "";
        var cfg = getConfig();
        String key = currency.toLowerCase();
        return cfg.getString("multi-currency.currencies." + key + ".symbol", "");
    }

    /**
     * Format a price string suitable for messages. Uses the loaded MessageProvider's
     * `price_message_format` template when available; otherwise falls back to the
     * existing combined `format(...)` behavior.
     */
    public String formatPriceForMessage(double amount, String currency) {
        if (this.messageProvider != null) {
            return this.messageProvider.formatPrice(this, amount, currency);
        }
        return format(amount, currency);
    }

    public VaultEconomyImpl getEconomy() {
        return vaultEconomy;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
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
        return metrics;
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stop();
        } else {
            Bukkit.getServicesManager().unregister(Economy.class, vaultEconomy);
        }
        getLogger().info("EzEconomy disabled.");
        INSTANCE = null;
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }
    public void loadMessageProvider() {
        // Delegate runtime reload of messages to the ConfigComponent implementation.
        new com.skyblockexp.ezeconomy.bootstrap.component.ConfigComponent(this).start();
    }

    public VaultEconomyImpl getVaultEconomy() {
        return vaultEconomy;
    }
    
    public BankInterestManager getBankInterestManager() {
        return bankInterestManager;
    }

    /**
     * Returns the default currency as defined in config or "dollar" if not set.
     */
    public String getDefaultCurrency() {
        return currencyManager.getDefaultCurrency();
    }

    /**
     * Returns the CurrencyManager instance.
     */
    public com.skyblockexp.ezeconomy.manager.CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    /**
     * Returns the storage provider, logging a warning if not available.
     */
    public StorageProvider getStorageOrWarn() {
        if (storage == null && !storageWarningLogged) {
            getLogger().warning("Storage provider is not initialized!");
            storageWarningLogged = true;
        }
        return storage;
    }

    /**
     * Returns the storage provider (may be null if not initialized).
     */
    public StorageProvider getStorage() {
        return storage;
    }

    /**
     * Returns the CurrencyPreferenceManager instance.
     */
    public CurrencyPreferenceManager getCurrencyPreferenceManager() {
        return currencyPreferenceManager;
    }

    /**
     * Logs a transaction using the storage provider.
     */
    public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction transaction) {
        if (storage != null) {
            // Update runtime metrics counters before persisting the transaction
            double amt = transaction.getAmount();
            try {
                long cents = Math.round(java.math.BigDecimal.valueOf(amt).movePointRight(2).doubleValue());
                if (amt > 0) {
                    totalDepositedCents.addAndGet(cents);
                } else if (amt < 0) {
                    totalWithdrawnCents.addAndGet(Math.abs(cents));
                }
            } catch (Exception ignored) {}
            storage.logTransaction(transaction);
        }
    }

    /**
     * Record a conversion amount (target currency amount). Called by conversion flows.
     */
    public void recordConversion(double amount) {
        try {
            long cents = Math.round(java.math.BigDecimal.valueOf(amount).movePointRight(2).doubleValue());
            totalConvertedCents.addAndGet(cents);
        } catch (Exception ignored) {}
    }

    public long getTotalDepositedCents() { return totalDepositedCents.get(); }
    public long getTotalWithdrawnCents() { return totalWithdrawnCents.get(); }
    public long getTotalConvertedCents() { return totalConvertedCents.get(); }

    /**
     * Retrieves transaction history for a player and currency.
     */
    public java.util.List<com.skyblockexp.ezeconomy.api.storage.models.Transaction> getTransactions(java.util.UUID uuid, String currency) {
        if (storage != null) {
            return storage.getTransactions(uuid, currency);
        }
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
        this.messageProvider = provider;
    }

    public boolean initializeStorage() {
        // Storage initialization moved to StorageComponent during bootstrap.
        // Keep method for compatibility; actual initialization is no-op here.
        return storage != null;
    }

    public YamlConfiguration loadStorageConfig(String fileName) {
        File file = new File(getDataFolder(), fileName);
        return YamlConfiguration.loadConfiguration(file);
    }

    public void setStorage(StorageProvider provider) {
        this.storage = provider;
    }

    public void initializeManagers() {
        // Manager initialization moved to ManagersComponent during bootstrap.
        new com.skyblockexp.ezeconomy.bootstrap.component.ManagersComponent(this).start();
    }

    public void setCurrencyPreferenceManager(CurrencyPreferenceManager m) {
        this.currencyPreferenceManager = m;
    }

    public void setCurrencyManager(CurrencyManager m) {
        this.currencyManager = m;
    }

    public void setBankInterestManager(BankInterestManager m) {
        this.bankInterestManager = m;
    }

    public void setDailyRewardManager(DailyRewardManager m) {
        this.dailyRewardManager = m;
    }

    public void setPayFlowManager(com.skyblockexp.ezeconomy.gui.PayFlowManager m) {
        this.payFlowManager = m;
    }

    public com.skyblockexp.ezeconomy.gui.PayFlowManager getPayFlowManager() {
        return this.payFlowManager;
    }

    public DailyRewardManager getDailyRewardManager() {
        return this.dailyRewardManager;
    }

    public void setMetrics(EzEconomyMetrics metrics) {
        this.metrics = metrics;
    }

    public void registerEconomy() {
        // Economy registration moved to EconomyComponent during bootstrap.
        new com.skyblockexp.ezeconomy.bootstrap.component.EconomyComponent(this).start();
    }

    public void setVaultEconomy(VaultEconomyImpl impl) {
        this.vaultEconomy = impl;
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

    public com.skyblockexp.ezeconomy.lock.LockManager getLockManager() {
        return this.lockManager;
    }

    public void setLockManager(com.skyblockexp.ezeconomy.lock.LockManager m) {
        this.lockManager = m;
    }

    /**
     * Returns the active plugin instance, or null if not set.
     */
    public static EzEconomyPlugin getInstance() {
        return INSTANCE;
    }

    public void registerPlaceholderExpansion() {
        // Placeholder registration moved to PlaceholderComponent during bootstrap.
        new com.skyblockexp.ezeconomy.bootstrap.component.PlaceholderComponent(this).start();
    }
}

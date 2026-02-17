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

    public VaultEconomyImpl getEconomy() {
        return vaultEconomy;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureDefaultConfigs();
        loadMessages();

        if (!initializeStorage()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initializeManagers();
        registerEconomy();
        registerCommands();
        registerListeners();
        registerPlaceholderExpansion();

        // Load optional user GUI configuration
        loadUserGuiConfig();

        new SpigotUpdateChecker(this, SPIGOT_RESOURCE_ID).checkForUpdates();
        getLogger().info("EzEconomy enabled and registered as Vault provider.");
    }

    public EzEconomyMetrics getMetrics() {
        return metrics;
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregister(Economy.class, vaultEconomy);
        getLogger().info("EzEconomy disabled.");
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    public void loadMessageProvider() {
        // Reload currently configured language files with graceful fallback
        String language = getConfig().getString("language", "en");
        String resourcePath = "languages/" + language + ".yml";
        File langFile = new File(getDataFolder(), "languages" + File.separator + language + ".yml");

        FileConfiguration selected;
        if (getResource(resourcePath) != null) {
            if (!langFile.exists()) {
                saveResource(resourcePath, false);
            }
            selected = YamlConfiguration.loadConfiguration(langFile);
        } else {
            getLogger().warning("Language resource '" + resourcePath + "' not found in plugin jar; falling back to English.");
            File fallbackFile = new File(getDataFolder(), "languages" + File.separator + "en.yml");
            if (!fallbackFile.exists() && getResource("languages/en.yml") != null) {
                saveResource("languages/en.yml", false);
            }
            selected = YamlConfiguration.loadConfiguration(fallbackFile);
            language = "en";
        }

        File fallbackFile = new File(getDataFolder(), "languages" + File.separator + "en.yml");
        if (!fallbackFile.exists() && getResource("languages/en.yml") != null) {
            saveResource("languages/en.yml", false);
        }
        FileConfiguration fallback = YamlConfiguration.loadConfiguration(fallbackFile);
        this.messagesConfig = selected;
        this.messageProvider = new MessageProvider(selected, fallback, language);
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
            storage.logTransaction(transaction);
        }
    }

    /**
     * Retrieves transaction history for a player and currency.
     */
    public java.util.List<com.skyblockexp.ezeconomy.api.storage.models.Transaction> getTransactions(java.util.UUID uuid, String currency) {
        if (storage != null) {
            return storage.getTransactions(uuid, currency);
        }
        return java.util.Collections.emptyList();
    }

    private void ensureDefaultConfigs() {
        for (String fileName : DEFAULT_CONFIGS) {
            File outFile = new File(getDataFolder(), fileName);
            if (outFile.exists()) {
                continue;
            }
            try (InputStream in = getResource(fileName)) {
                if (in == null) {
                    continue;
                }
                Files.createDirectories(outFile.getParentFile().toPath());
                Files.copy(in, outFile.toPath());
                getLogger().info("Created default config: " + fileName);
            } catch (IOException ex) {
                getLogger().warning("Could not create default config " + fileName + ": " + ex.getMessage());
            }
        }
    }

    private void loadMessages() {
        // Load language files on plugin enable; fall back to English if missing
        String language = getConfig().getString("language", "en");
        String resourcePath = "languages/" + language + ".yml";
        File langFile = new File(getDataFolder(), "languages" + File.separator + language + ".yml");

        FileConfiguration selected;
        if (getResource(resourcePath) != null) {
            if (!langFile.exists()) {
                saveResource(resourcePath, false);
            }
            selected = YamlConfiguration.loadConfiguration(langFile);
        } else {
            getLogger().warning("Language resource '" + resourcePath + "' not found in plugin jar; falling back to English.");
            File fallbackFile = new File(getDataFolder(), "languages" + File.separator + "en.yml");
            if (!fallbackFile.exists() && getResource("languages/en.yml") != null) {
                saveResource("languages/en.yml", false);
            }
            selected = YamlConfiguration.loadConfiguration(fallbackFile);
            language = "en";
        }

        File fallbackFile = new File(getDataFolder(), "languages" + File.separator + "en.yml");
        if (!fallbackFile.exists() && getResource("languages/en.yml") != null) {
            saveResource("languages/en.yml", false);
        }
        FileConfiguration fallback = YamlConfiguration.loadConfiguration(fallbackFile);
        this.messagesConfig = selected;
        this.messageProvider = new MessageProvider(selected, fallback, language);
    }

    private boolean initializeStorage() {
        String storageType = getConfig().getString("storage", "yml").toLowerCase();
        try {
            switch (storageType) {
                case "yml":
                case "yaml":
                    storage = new YMLStorageProvider(this, loadStorageConfig("config-yml.yml"));
                    break;
                case "mysql":
                    storage = new MySQLStorageProvider(this, loadStorageConfig("config-mysql.yml"));
                    break;
                case "sqlite":
                    storage = new SQLiteStorageProvider(this, loadStorageConfig("config-sqlite.yml"));
                    break;
                case "mongodb":
                    storage = new MongoDBStorageProvider(this, loadStorageConfig("config-mongodb.yml"));
                    break;
                default:
                    getLogger().warning("Unknown storage type '" + storageType + "', defaulting to YML.");
                    storage = new YMLStorageProvider(this, loadStorageConfig("config-yml.yml"));
                    break;
            }
            getLogger().info("Using " + storage.getClass().getSimpleName() + " storage provider.");

            // In test mode we avoid performing real network or filesystem initialization
            // to keep unit/feature tests hermetic. Tests can set the system property
            // `ezeconomy.test=true` to opt into this behavior.
            if (Boolean.getBoolean("ezeconomy.test")) {
                getLogger().info("Test mode detected: skipping storage init/load for " + storage.getClass().getSimpleName());
            } else {
                getLogger().info("Initializing " + storage.getClass().getSimpleName() + " storage provider.");
                storage.init();

                getLogger().info("Loading connection with " + storage.getClass().getSimpleName() + " storage provider.");
                storage.load();
            }

            return true;
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize storage provider: " + ex.getMessage());
            storage = null;
            return false;
        }
    }

    private YamlConfiguration loadStorageConfig(String fileName) {
        File file = new File(getDataFolder(), fileName);
        return YamlConfiguration.loadConfiguration(file);
    }

    private void initializeManagers() {
        this.currencyPreferenceManager = new CurrencyPreferenceManager(this);
        this.currencyManager = new CurrencyManager(this);
        this.bankInterestManager = new BankInterestManager(this);
        long interval = getConfig().getLong("bank-interest-interval-ticks", DEFAULT_INTEREST_INTERVAL_TICKS);
        bankInterestManager.start(interval);
        this.dailyRewardManager = new DailyRewardManager(this);
        try {
            this.metrics = new EzEconomyMetrics(this);
        } catch (Exception ex) {
            getLogger().warning("Failed to initialize metrics: " + ex.getMessage());
            this.metrics = null;
        }
        // Pay flow manager handles custom amount entry state
        this.payFlowManager = new com.skyblockexp.ezeconomy.gui.PayFlowManager();
    }

    public com.skyblockexp.ezeconomy.gui.PayFlowManager getPayFlowManager() {
        return this.payFlowManager;
    }

    private void registerEconomy() {
        this.vaultEconomy = new VaultEconomyImpl(this);
        Bukkit.getServicesManager().register(Economy.class, vaultEconomy, this, ServicePriority.Highest);
    }

    private void registerCommands() {
        getCommand("balance").setExecutor(new BalanceCommand(this));
        getCommand("balance").setTabCompleter(new com.skyblockexp.ezeconomy.tabcomplete.BalanceTabCompleter(this));
        getCommand("eco").setExecutor(new EcoCommand(this));
        getCommand("eco").setTabCompleter(new com.skyblockexp.ezeconomy.tabcomplete.EcoTabCompleter(this));
        getCommand("baltop").setExecutor(new BaltopCommand(this));
        getCommand("baltop").setTabCompleter(new com.skyblockexp.ezeconomy.tabcomplete.BaltopTabCompleter(this));
        getCommand("bank").setExecutor(new BankCommand(this));
        getCommand("bank").setTabCompleter(new com.skyblockexp.ezeconomy.tabcomplete.BankTabCompleter(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("pay").setTabCompleter(new com.skyblockexp.ezeconomy.tabcomplete.PayTabCompleter(this));
        getCommand("currency").setExecutor(new CurrencyCommand(this));
        getCommand("currency").setTabCompleter(new com.skyblockexp.ezeconomy.tabcomplete.CurrencyTabCompleter(this));
        getCommand("ezeconomy").setExecutor(new EzEconomyCommand(this, dailyRewardManager));
        getCommand("ezeconomy").setTabCompleter(new EzEconomyCommandTabCompleter(this));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new DailyRewardListener(dailyRewardManager), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
    }

    private void loadUserGuiConfig() {
        File file = new File(getDataFolder(), "user-gui.yml");
        if (!file.exists()) {
            if (getResource("user-gui.yml") != null) {
                saveResource("user-gui.yml", false);
            }
        }
        this.userGuiConfig = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getUserGuiConfig() {
        return this.userGuiConfig == null ? YamlConfiguration.loadConfiguration(new File(getDataFolder(), "user-gui.yml")) : this.userGuiConfig;
    }

    private void registerPlaceholderExpansion() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        new EzEconomyPlaceholderExpansion(this).register();
        getLogger().info("Registered EzEconomy placeholders with PlaceholderAPI.");
    }
}

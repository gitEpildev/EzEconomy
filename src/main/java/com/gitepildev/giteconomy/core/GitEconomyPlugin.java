package com.gitepildev.giteconomy.core;

import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.manager.CurrencyManager;
import com.gitepildev.giteconomy.manager.CurrencyPreferenceManager;
import com.gitepildev.giteconomy.update.SpigotUpdateChecker;
import java.io.File;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import java.util.Locale;

public class GitEconomyPlugin extends JavaPlugin {
    private static final int SPIGOT_RESOURCE_ID = 130975;
        private static final List<String> DEFAULT_CONFIGS = List.of(
            "config-yml.yml",
            "config-mysql.yml",
            "config-sqlite.yml",
            "config-mongodb.yml",
            "languages/en.yml",
            "languages/nl.yml",
            "languages/es.yml",
            "languages/fr.yml",
            "languages/zh.yml"
        );

    private StorageProvider storage;
    private boolean storageWarningLogged;
    private CurrencyPreferenceManager currencyPreferenceManager;
    private CurrencyManager currencyManager;
    private GitEconomyMetrics metrics;
    private MessageProvider messageProvider;
    private VaultEconomyImpl vaultEconomy;
    private FileConfiguration messagesConfig;
    private com.gitepildev.giteconomy.bootstrap.Bootstrap bootstrap;
    private com.gitepildev.giteconomy.lock.LockManager lockManager;
    private static GitEconomyPlugin INSTANCE;
    // Extracted services
    private com.gitepildev.giteconomy.service.metrics.TransactionMetricsService transactionMetricsService;
    private com.gitepildev.giteconomy.service.format.CurrencyFormatter currencyFormatter;
    private com.gitepildev.giteconomy.service.storage.StorageConfigLoader storageConfigLoader;
    private com.gitepildev.giteconomy.messaging.CrossServerMessenger crossServerMessenger;

    public com.gitepildev.giteconomy.messaging.CrossServerMessenger getCrossServerMessenger() {
        return crossServerMessenger;
    }

    public void setCrossServerMessenger(com.gitepildev.giteconomy.messaging.CrossServerMessenger messenger) {
        this.crossServerMessenger = messenger;
    }

    

    public VaultEconomyImpl getEconomy() {
        return vaultEconomy;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.bootstrap = new com.gitepildev.giteconomy.bootstrap.Bootstrap(this);
        try {
            this.bootstrap.start();
            // Initialize extracted services after bootstrap so components/config/storage exist
            this.storageConfigLoader = new com.gitepildev.giteconomy.service.storage.StorageConfigLoader(this);
            this.currencyFormatter = new com.gitepildev.giteconomy.service.format.CurrencyFormatter(this);
            this.transactionMetricsService = new com.gitepildev.giteconomy.service.metrics.TransactionMetricsService(this);
            new SpigotUpdateChecker(this, SPIGOT_RESOURCE_ID).checkForUpdates();
            getLogger().info("GitEconomy enabled and registered as Vault provider.");
        } catch (RuntimeException ex) {
            getLogger().severe("Bootstrap failed: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public GitEconomyMetrics getMetrics() {
        return metrics;
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stop();
        } else {
            Bukkit.getServicesManager().unregister(Economy.class, vaultEconomy);
        }
        getLogger().info("GitEconomy disabled.");
        INSTANCE = null;
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }
    public void loadMessageProvider() {
        // Delegate runtime reload of messages to the ConfigComponent implementation.
        new com.gitepildev.giteconomy.bootstrap.component.ConfigComponent(this).start();
    }

    public VaultEconomyImpl getVaultEconomy() {
        return vaultEconomy;
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
    public com.gitepildev.giteconomy.manager.CurrencyManager getCurrencyManager() {
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
     * Retrieves transaction history for a player and currency.
     */
    public java.util.List<com.gitepildev.giteconomy.api.storage.models.Transaction> getTransactions(java.util.UUID uuid, String currency) {
        if (storage != null) {
            return storage.getTransactions(uuid, currency);
        }
        return java.util.Collections.emptyList();
    }

    public void setMessagesConfig(FileConfiguration messagesConfig) {
        this.messagesConfig = messagesConfig;
    }

    public void setMessageProvider(MessageProvider provider) {
        this.messageProvider = provider;
    }

    

    public void setStorage(StorageProvider provider) {
        this.storage = provider;
    }

    public void setCurrencyPreferenceManager(CurrencyPreferenceManager m) {
        this.currencyPreferenceManager = m;
    }

    public void setCurrencyManager(CurrencyManager m) {
        this.currencyManager = m;
    }

    public void setMetrics(GitEconomyMetrics metrics) {
        this.metrics = metrics;
    }

    public void setVaultEconomy(VaultEconomyImpl impl) {
        this.vaultEconomy = impl;
    }

    /**
     * @deprecated Economy registration is handled by the EconomyComponent during bootstrap.
     * This transitional shim delegates to the EconomyComponent to preserve test compatibility.
     */
    @Deprecated
    public void registerEconomy() {
        new com.gitepildev.giteconomy.bootstrap.component.EconomyComponent(this).start();
    }

    public com.gitepildev.giteconomy.lock.LockManager getLockManager() {
        return this.lockManager;
    }

    public void setLockManager(com.gitepildev.giteconomy.lock.LockManager m) {
        this.lockManager = m;
    }

    /**
     * Resolve lock timing/attempt settings according to configured locking strategy.
     * Strategy-specific files (redis.yml / bungeecord.yml) override generic config values.
     */
    public long getLockTtlMs() {
        return resolveLockSettings().ttlMs;
    }

    public long getLockRetryMs() {
        return resolveLockSettings().retryMs;
    }

    public int getLockMaxAttempts() {
        return resolveLockSettings().maxAttempts;
    }

    /**
     * Compute effective lock settings and mirror them into legacy redis.* config keys.
     * This keeps existing call sites strategy-aware without breaking compatibility.
     */
    public void refreshEffectiveLockSettings() {
        LockSettings settings = resolveLockSettings();
        FileConfiguration cfg = getConfig();
        cfg.set("redis.ttl-ms", settings.ttlMs);
        cfg.set("redis.retry-ms", settings.retryMs);
        cfg.set("redis.max-attempts", settings.maxAttempts);
    }

    private LockSettings resolveLockSettings() {
        FileConfiguration cfg = getConfig();
        String strategy = cfg.getString("locking-strategy", "LOCAL").toUpperCase(Locale.ROOT);

        long ttlMs = cfg.getLong("locking.ttl-ms", 5000L);
        long retryMs = cfg.getLong("locking.retry-ms", 50L);
        int maxAttempts = cfg.getInt("locking.max-attempts", 100);

        // Backward compatibility with previous config keys.
        ttlMs = cfg.getLong("redis.ttl-ms", ttlMs);
        retryMs = cfg.getLong("redis.retry-ms", retryMs);
        maxAttempts = cfg.getInt("redis.max-attempts", maxAttempts);

        if ("REDIS".equals(strategy)) {
            FileConfiguration redis = loadOptionalConfig("redis.yml");
            if (redis != null) {
                ttlMs = redis.getLong("ttl-ms", ttlMs);
                retryMs = redis.getLong("retry-ms", retryMs);
                maxAttempts = redis.getInt("max-attempts", maxAttempts);
            }
        } else if ("BUNGEECORD".equals(strategy)) {
            FileConfiguration bungee = loadOptionalConfig("bungeecord.yml");
            if (bungee != null) {
                ttlMs = bungee.getLong("ttl-ms", ttlMs);
                retryMs = bungee.getLong("retry-ms", retryMs);
                maxAttempts = bungee.getInt("max-attempts", maxAttempts);
            }
        }

        return new LockSettings(ttlMs, retryMs, maxAttempts);
    }

    private FileConfiguration loadOptionalConfig(String fileName) {
        File file = new File(getDataFolder(), fileName);
        return file.exists() ? YamlConfiguration.loadConfiguration(file) : null;
    }

    private static final class LockSettings {
        private final long ttlMs;
        private final long retryMs;
        private final int maxAttempts;

        private LockSettings(long ttlMs, long retryMs, int maxAttempts) {
            this.ttlMs = ttlMs;
            this.retryMs = retryMs;
            this.maxAttempts = maxAttempts;
        }
    }

    /**
     * Returns the active plugin instance, or null if not set.
     */
    public static GitEconomyPlugin getInstance() {
        return INSTANCE;
    }

    /**
     * Accessor for the CurrencyFormatter service.
     */
    public com.gitepildev.giteconomy.service.format.CurrencyFormatter getCurrencyFormatter() {
        if (currencyFormatter == null) currencyFormatter = new com.gitepildev.giteconomy.service.format.CurrencyFormatter(this);
        return currencyFormatter;
    }

    /**
     * Accessor for the TransactionMetricsService.
     */
    public com.gitepildev.giteconomy.service.metrics.TransactionMetricsService getTransactionMetricsService() {
        if (transactionMetricsService == null) transactionMetricsService = new com.gitepildev.giteconomy.service.metrics.TransactionMetricsService(this);
        return transactionMetricsService;
    }

    /**
     * Accessor for the StorageConfigLoader.
     */
    public com.gitepildev.giteconomy.service.storage.StorageConfigLoader getStorageConfigLoader() {
        if (storageConfigLoader == null) storageConfigLoader = new com.gitepildev.giteconomy.service.storage.StorageConfigLoader(this);
        return storageConfigLoader;
    }
}

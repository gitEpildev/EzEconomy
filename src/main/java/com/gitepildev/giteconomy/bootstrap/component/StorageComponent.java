package com.gitepildev.giteconomy.bootstrap.component;

import com.gitepildev.giteconomy.bootstrap.BootstrapComponent;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.storage.YMLStorageProvider;
import com.gitepildev.giteconomy.storage.MySQLStorageProvider;
import com.gitepildev.giteconomy.storage.SQLiteStorageProvider;
import com.gitepildev.giteconomy.storage.MongoDBStorageProvider;
import org.bukkit.configuration.file.YamlConfiguration;
import com.gitepildev.giteconomy.service.storage.StorageConfigLoader;

public class StorageComponent implements BootstrapComponent {
    private final GitEconomyPlugin plugin;

    public StorageComponent(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        // Support both top-level `storage: mysql` and nested `storage.type: mysql`
        Object storageCfg = plugin.getConfig().get("storage");
        String type;
        if (storageCfg instanceof String) {
            type = ((String) storageCfg).toLowerCase();
        } else {
            type = plugin.getConfig().getString("storage.type", "yml").toLowerCase();
        }
        StorageProvider provider = null;
        try {
            switch (type) {
                case "mysql":
                    YamlConfiguration mysqlCfg = new StorageConfigLoader(plugin).load("config-mysql.yml");
                    provider = new MySQLStorageProvider(plugin, mysqlCfg);
                    break;
                case "sqlite":
                case "sqlite3":
                    YamlConfiguration sqliteCfg = new StorageConfigLoader(plugin).load("config-sqlite.yml");
                    provider = new SQLiteStorageProvider(plugin, sqliteCfg);
                    break;
                case "mongodb":
                case "mongo":
                    YamlConfiguration mongoCfg = new StorageConfigLoader(plugin).load("config-mongodb.yml");
                    provider = new MongoDBStorageProvider(plugin, mongoCfg);
                    break;
                case "yml":
                default:
                    YamlConfiguration ymlCfg = new StorageConfigLoader(plugin).load("config-yml.yml");
                    provider = new YMLStorageProvider(plugin, ymlCfg);
                    break;
            }

            if (provider == null) {
                throw new RuntimeException("No storage provider selected");
            }

            // initialize and load provider
            provider.init();
            provider.load();
            plugin.setStorage(provider);
            plugin.getLogger().info("Initialized storage provider: " + provider.getClass().getSimpleName());
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to initialize storage provider: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop() {
        StorageProvider s = plugin.getStorage();
        if (s != null) {
            try {
                s.save();
            } catch (Exception ignored) {}
            try {
                s.shutdown();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void reload() {
        // Reinitialize storage if requested
        stop();
        start();
    }
}

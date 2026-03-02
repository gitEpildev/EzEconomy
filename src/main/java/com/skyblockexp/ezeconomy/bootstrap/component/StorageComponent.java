package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.Registry;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.storage.YMLStorageProvider;
import com.skyblockexp.ezeconomy.storage.MySQLStorageProvider;
import com.skyblockexp.ezeconomy.storage.SQLiteStorageProvider;
import com.skyblockexp.ezeconomy.storage.MongoDBStorageProvider;
import org.bukkit.configuration.file.YamlConfiguration;

public class StorageComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public StorageComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        String type = plugin.getConfig().getString("storage.type", "yml").toLowerCase();
        StorageProvider provider = null;
        try {
            switch (type) {
                case "mysql":
                    YamlConfiguration mysqlCfg = plugin.loadStorageConfig("config-mysql.yml");
                    provider = new MySQLStorageProvider(plugin, mysqlCfg);
                    break;
                case "sqlite":
                case "sqlite3":
                    YamlConfiguration sqliteCfg = plugin.loadStorageConfig("config-sqlite.yml");
                    provider = new SQLiteStorageProvider(plugin, sqliteCfg);
                    break;
                case "mongodb":
                case "mongo":
                    YamlConfiguration mongoCfg = plugin.loadStorageConfig("config-mongodb.yml");
                    provider = new MongoDBStorageProvider(plugin, mongoCfg);
                    break;
                case "yml":
                default:
                    YamlConfiguration ymlCfg = plugin.loadStorageConfig("config-yml.yml");
                    provider = new YMLStorageProvider(plugin, ymlCfg);
                    break;
            }

            if (provider == null) {
                throw new RuntimeException("No storage provider selected");
            }

            // initialize, load and register provider
            provider.init();
            provider.load();
            Registry.register(StorageProvider.class, provider);
            plugin.getLogger().info("Initialized storage provider: " + provider.getClass().getSimpleName());
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to initialize storage provider: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop() {
        StorageProvider s = Registry.get(StorageProvider.class);
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

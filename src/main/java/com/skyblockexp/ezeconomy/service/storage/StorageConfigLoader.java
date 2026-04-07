package com.skyblockexp.ezeconomy.service.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class StorageConfigLoader {
    private final Plugin plugin;

    public StorageConfigLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public YamlConfiguration load(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        return YamlConfiguration.loadConfiguration(file);
    }
}

package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.Registry;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class GuiComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public GuiComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        File file = new File(plugin.getDataFolder(), "user-gui.yml");
        if (!file.exists()) {
            if (plugin.getResource("user-gui.yml") != null) {
                plugin.saveResource("user-gui.yml", false);
            }
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        // register user GUI config for consumers/tests
        Registry.register(FileConfiguration.class, cfg);
    }

    @Override
    public void stop() {
        // no-op
    }

    @Override
    public void reload() {
        start();
    }
}

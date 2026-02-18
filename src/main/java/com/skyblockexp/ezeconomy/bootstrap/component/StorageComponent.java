package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class StorageComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public StorageComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        boolean ok = plugin.initializeStorage();
        if (!ok) {
            throw new RuntimeException("Storage initialization failed");
        }
    }

    @Override
    public void stop() {
        // storage providers may implement their own shutdowns; nothing generic here
    }

    @Override
    public void reload() {
        // no-op for now
    }
}

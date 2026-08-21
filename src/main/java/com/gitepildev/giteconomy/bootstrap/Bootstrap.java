package com.gitepildev.giteconomy.bootstrap;

import com.gitepildev.giteconomy.bootstrap.component.CommandsComponent;
import com.gitepildev.giteconomy.bootstrap.component.ConfigComponent;
import com.gitepildev.giteconomy.bootstrap.component.ListenersComponent;
import com.gitepildev.giteconomy.bootstrap.component.ManagersComponent;
import com.gitepildev.giteconomy.bootstrap.component.MetricsComponent;
import com.gitepildev.giteconomy.bootstrap.component.PlaceholderComponent;
import com.gitepildev.giteconomy.bootstrap.component.EconomyComponent;
import com.gitepildev.giteconomy.bootstrap.component.ShutdownComponent;
import com.gitepildev.giteconomy.bootstrap.component.StorageComponent;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bootstrap {
    private final GitEconomyPlugin plugin;
    private final List<BootstrapComponent> components = new ArrayList<>();
    private boolean started = false;

    public Bootstrap(GitEconomyPlugin plugin) {
        this.plugin = plugin;
        // build components in startup order
        components.add(new ConfigComponent(plugin));
        components.add(new com.gitepildev.giteconomy.bootstrap.component.CacheComponent(plugin));
        components.add(new com.gitepildev.giteconomy.bootstrap.component.LockingComponent(plugin));
        components.add(new StorageComponent(plugin));
        components.add(new com.gitepildev.giteconomy.bootstrap.component.MessagingComponent(plugin));
        components.add(new ManagersComponent(plugin));
        // Metrics component should be initialized after managers
        components.add(new MetricsComponent(plugin));
        // Ensure Vault economy provider is registered before commands run
        components.add(new EconomyComponent(plugin));
        components.add(new CommandsComponent(plugin));
        components.add(new ListenersComponent(plugin));
        components.add(new PlaceholderComponent(plugin));
        components.add(new ShutdownComponent(plugin));
    }

    public void start() {
        List<BootstrapComponent> startedComponents = new ArrayList<>();
        try {
            for (BootstrapComponent c : components) {
                // Do not start the shutdown component here
                if (c instanceof ShutdownComponent) continue;
                c.start();
                startedComponents.add(c);
            }
            started = true;
        } catch (RuntimeException ex) {
            // stop started components in reverse order
            Collections.reverse(startedComponents);
            for (BootstrapComponent c : startedComponents) {
                try { c.stop(); } catch (Exception ignored) {}
            }
            throw ex;
        }
    }

    public void stop() {
        // Run stop on all components in reverse order so shutdown runs last
        List<BootstrapComponent> rev = new ArrayList<>(components);
        Collections.reverse(rev);
        for (BootstrapComponent c : rev) {
            try {
                c.stop();
            } catch (Exception ex) {
                plugin.getLogger().warning("Error stopping component: " + ex.getMessage());
            }
        }
        started = false;
    }

    public void reload() {
        for (BootstrapComponent c : components) {
            try { 
                c.reload(); 
            } catch (Exception ex) {
                plugin.getLogger().warning("Error reloading component: " + ex.getMessage()); 
            }
        }
    }
}

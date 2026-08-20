package com.gitepildev.giteconomy.bootstrap.component;

import com.gitepildev.giteconomy.bootstrap.BootstrapComponent;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.command.BalanceCommand;
import com.gitepildev.giteconomy.command.BaltopCommand;
import com.gitepildev.giteconomy.command.CurrencyCommand;
import com.gitepildev.giteconomy.command.EcoCommand;
import com.gitepildev.giteconomy.command.GitEconomyCommand;
import com.gitepildev.giteconomy.command.PayCommand;
import com.gitepildev.giteconomy.tabcomplete.BalanceTabCompleter;
import com.gitepildev.giteconomy.tabcomplete.BaltopTabCompleter;
import com.gitepildev.giteconomy.tabcomplete.CurrencyTabCompleter;
import com.gitepildev.giteconomy.tabcomplete.EcoTabCompleter;
import com.gitepildev.giteconomy.tabcomplete.GitEconomyCommandTabCompleter;
import com.gitepildev.giteconomy.tabcomplete.PayTabCompleter;

public class CommandsComponent implements BootstrapComponent {
    private final GitEconomyPlugin plugin;

    public CommandsComponent(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        plugin.getCommand("balance").setExecutor(new BalanceCommand(plugin));
        plugin.getCommand("balance").setTabCompleter(new BalanceTabCompleter(plugin));
        plugin.getCommand("eco").setExecutor(new EcoCommand(plugin));
        plugin.getCommand("eco").setTabCompleter(new EcoTabCompleter(plugin));
        plugin.getCommand("baltop").setExecutor(new BaltopCommand(plugin));
        plugin.getCommand("baltop").setTabCompleter(new BaltopTabCompleter(plugin));
        // Register pay and alias payall to the same executor
        if (plugin.getCommand("pay") != null) {
            var payCmd = new PayCommand(plugin);
            plugin.getCommand("pay").setExecutor(payCmd);
            plugin.getCommand("pay").setTabCompleter(new PayTabCompleter(plugin));
            if (plugin.getCommand("payall") != null) {
                plugin.getCommand("payall").setExecutor(payCmd);
                plugin.getCommand("payall").setTabCompleter(new PayTabCompleter(plugin));
            }
        }
        plugin.getCommand("currency").setExecutor(new CurrencyCommand(plugin));
        plugin.getCommand("currency").setTabCompleter(new CurrencyTabCompleter(plugin));
        plugin.getCommand("giteconomy").setExecutor(new GitEconomyCommand(plugin));
        plugin.getCommand("giteconomy").setTabCompleter(new GitEconomyCommandTabCompleter(plugin));
    }

    @Override
    public void stop() {
        // No generic command unregister API; Bukkit handles plugin disable.
    }

    @Override
    public void reload() {
        // Re-register commands
        start();
    }
}

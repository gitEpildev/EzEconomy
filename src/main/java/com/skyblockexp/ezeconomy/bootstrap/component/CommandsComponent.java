package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.command.BalanceCommand;
import com.skyblockexp.ezeconomy.command.BaltopCommand;
import com.skyblockexp.ezeconomy.command.CurrencyCommand;
import com.skyblockexp.ezeconomy.command.EcoCommand;
import com.skyblockexp.ezeconomy.command.EzEconomyCommand;
import com.skyblockexp.ezeconomy.command.PayCommand;
import com.skyblockexp.ezeconomy.tabcomplete.BalanceTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.BaltopTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.CurrencyTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.EcoTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.EzEconomyCommandTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.PayTabCompleter;

public class CommandsComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public CommandsComponent(EzEconomyPlugin plugin) {
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
        plugin.getCommand("ezeconomy").setExecutor(new EzEconomyCommand(plugin));
        plugin.getCommand("ezeconomy").setTabCompleter(new EzEconomyCommandTabCompleter(plugin));
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

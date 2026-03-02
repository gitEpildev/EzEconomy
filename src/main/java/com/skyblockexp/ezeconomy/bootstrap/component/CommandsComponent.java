package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.Registry;
import com.skyblockexp.ezeconomy.command.BalanceCommand;
import com.skyblockexp.ezeconomy.command.BaltopCommand;
import com.skyblockexp.ezeconomy.command.BankCommand;
import com.skyblockexp.ezeconomy.command.CurrencyCommand;
import com.skyblockexp.ezeconomy.command.EcoCommand;
import com.skyblockexp.ezeconomy.command.EzEconomyCommand;
import com.skyblockexp.ezeconomy.command.PayCommand;
import com.skyblockexp.ezeconomy.tabcomplete.BalanceTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.BaltopTabCompleter;
import com.skyblockexp.ezeconomy.tabcomplete.BankTabCompleter;
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
        // Create and register command executors
        BalanceCommand balanceCmd = new BalanceCommand(plugin);
        plugin.getCommand("balance").setExecutor(balanceCmd);
        plugin.getCommand("balance").setTabCompleter(new BalanceTabCompleter(plugin));

        EcoCommand ecoCmd = new EcoCommand(plugin);
        plugin.getCommand("eco").setExecutor(ecoCmd);
        plugin.getCommand("eco").setTabCompleter(new EcoTabCompleter(plugin));

        BaltopCommand baltopCmd = new BaltopCommand(plugin);
        plugin.getCommand("baltop").setExecutor(baltopCmd);
        plugin.getCommand("baltop").setTabCompleter(new BaltopTabCompleter(plugin));

        BankCommand bankCmd = new BankCommand(plugin);
        plugin.getCommand("bank").setExecutor(bankCmd);
        plugin.getCommand("bank").setTabCompleter(new BankTabCompleter(plugin));

        PayCommand payCmd = new PayCommand(plugin);
        plugin.getCommand("pay").setExecutor(payCmd);
        plugin.getCommand("pay").setTabCompleter(new PayTabCompleter(plugin));

        CurrencyCommand currencyCmd = new CurrencyCommand(plugin);
        plugin.getCommand("currency").setExecutor(currencyCmd);
        plugin.getCommand("currency").setTabCompleter(new CurrencyTabCompleter(plugin));

        // Use Registry for manager lookup (ManagersComponent should have registered it earlier)
        com.skyblockexp.ezeconomy.manager.DailyRewardManager drm = Registry.get(com.skyblockexp.ezeconomy.manager.DailyRewardManager.class);
        EzEconomyCommand ezCmd = new EzEconomyCommand(plugin, drm);
        plugin.getCommand("ezeconomy").setExecutor(ezCmd);
        plugin.getCommand("ezeconomy").setTabCompleter(new EzEconomyCommandTabCompleter(plugin));

        // register command instances for potential runtime access/tests
        Registry.register(BalanceCommand.class, balanceCmd);
        Registry.register(EcoCommand.class, ecoCmd);
        Registry.register(BaltopCommand.class, baltopCmd);
        Registry.register(BankCommand.class, bankCmd);
        Registry.register(PayCommand.class, payCmd);
        Registry.register(CurrencyCommand.class, currencyCmd);
        Registry.register(EzEconomyCommand.class, ezCmd);
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

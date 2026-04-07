package com.skyblockexp.ezeconomy.tabcomplete;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BalanceTabCompleter implements TabCompleter {
    private final EzEconomyPlugin plugin;

    public BalanceTabCompleter(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ezeconomy.balance")) return Collections.emptyList();
        // /balance [player|currency] [currency]
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> res = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.getName() != null && p.getName().toLowerCase().startsWith(partial)) res.add(p.getName());
            });
            var messenger = plugin.getCrossServerMessenger();
            if (messenger != null) {
                messenger.getNetworkPlayers().stream()
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .forEach(res::add);
            }
            // suggest currencies
            var cfg = plugin.getConfig();
            if (cfg.isConfigurationSection("multi-currency.currencies")) {
                res.addAll(cfg.getConfigurationSection("multi-currency.currencies").getKeys(false).stream()
                        .filter(k -> k.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList()));
            }
            return res;
        }
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            var cfg = plugin.getConfig();
            if (cfg.isConfigurationSection("multi-currency.currencies")) {
                return cfg.getConfigurationSection("multi-currency.currencies").getKeys(false).stream()
                        .filter(k -> k.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}

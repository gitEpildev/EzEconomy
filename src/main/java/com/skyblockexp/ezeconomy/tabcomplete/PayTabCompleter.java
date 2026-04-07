package com.skyblockexp.ezeconomy.tabcomplete;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class PayTabCompleter implements TabCompleter {
    private final EzEconomyPlugin plugin;

    public PayTabCompleter(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ezeconomy.pay")) return Collections.emptyList();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            Set<String> names = new LinkedHashSet<>();
            Bukkit.getOnlinePlayers().forEach(p -> { if (p.getName() != null) names.add(p.getName()); });
            var messenger = plugin.getCrossServerMessenger();
            if (messenger != null) names.addAll(messenger.getNetworkPlayers());
            return names.stream()
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.asList("100", "1000", "10000").stream()
                .filter(s -> s.startsWith(args[1]))
                .collect(Collectors.toList());
        }
        if (args.length == 3) {
            var cfg = plugin.getConfig();
            if (cfg.isConfigurationSection("multi-currency.currencies")) {
                return cfg.getConfigurationSection("multi-currency.currencies").getKeys(false).stream()
                        .filter(k -> k.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
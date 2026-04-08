package com.skyblockexp.ezeconomy.tabcomplete;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.*;
import java.util.stream.Collectors;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class PayTabCompleter implements TabCompleter {
    private final EzEconomyPlugin plugin;

    public PayTabCompleter(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ezeconomy.pay")) return Collections.emptyList();
        // For /payall the first argument is the amount, not a player name.
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if (command != null && (command.getName().equalsIgnoreCase("payall") || alias.equalsIgnoreCase("payall"))) {
                // Suggest compact/shortcut formats
                return Arrays.asList("1k", "2k", "5k", "10k", "100", "1000", "1m")
                        .stream()
                        .filter(s -> s.startsWith(partial))
                        .collect(Collectors.toList());
            }
            return Bukkit.getOnlinePlayers().stream()
                .map(OfflinePlayer::getName)
                .filter(name -> name != null && name.toLowerCase().startsWith(partial))
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
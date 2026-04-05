package com.skyblockexp.ezeconomy.tabcomplete;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class EcoTabCompleter implements TabCompleter {
    private final EzEconomyPlugin plugin;

    public EcoTabCompleter(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ezeconomy.eco")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("give", "take", "set").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return com.skyblockexp.ezeconomy.util.PlayerLookup.namesStartingWith(args[1]);
        }
        // suggest currency at position 4: /eco give <player> <amount> [currency]
        if (args.length == 4) {
            var cfg = plugin.getConfig();
            if (cfg.isConfigurationSection("multi-currency.currencies")) {
                return cfg.getConfigurationSection("multi-currency.currencies").getKeys(false).stream()
                        .filter(k -> k.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}
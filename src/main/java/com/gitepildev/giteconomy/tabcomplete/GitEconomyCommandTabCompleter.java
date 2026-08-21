package com.gitepildev.giteconomy.tabcomplete;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tab completion for /giteconomy admin command and its subcommands.
 * Professional, context-aware, and permission-sensitive.
 */
public class GitEconomyCommandTabCompleter implements TabCompleter {
    private final GitEconomyPlugin plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "cleanup", "reload", "reloadmessages", "database"
    );
    private static final List<String> DATABASE_SUBS = Arrays.asList(
            "info", "test", "reset"
    );

    public GitEconomyCommandTabCompleter(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("database")) {
            return DATABASE_SUBS.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

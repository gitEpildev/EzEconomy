package com.gitepildev.giteconomy.command;

import org.bukkit.command.CommandSender;

/**
 * Interface for GitEconomy subcommands.
 */
public interface Subcommand {
    /**
     * Executes the subcommand.
     * @param sender The command sender
     * @param args The arguments (excluding the subcommand name)
     * @return true if the command was handled, false otherwise
     */
    boolean execute(CommandSender sender, String[] args);
}
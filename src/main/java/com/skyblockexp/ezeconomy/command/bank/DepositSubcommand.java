package com.skyblockexp.ezeconomy.command.bank;

import com.skyblockexp.ezeconomy.command.Subcommand;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.MessageProvider;
import com.skyblockexp.ezeconomy.util.NumberUtil;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Subcommand for /bank deposit <name> <amount> [currency]
 */
public class DepositSubcommand implements Subcommand {
    private final EzEconomyPlugin plugin;

    public DepositSubcommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ezeconomy.bank.deposit") && !sender.hasPermission("ezeconomy.bank.admin")) {
            com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }
        if (args.length < 2) {
            com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "usage_bank_deposit");
            return true;
        }
        String currency = args.length >= 3 ? args[2] : "dollar";
        Double amount = NumberUtil.parseDouble(args[1]);
        if (amount == null || amount <= 0) {
            com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "invalid_amount");
            return true;
        }
        EconomyResponse depositResponse = plugin.getEconomy().bankDeposit(args[0], currency, amount);
        if (handleEconomyFailure(sender, depositResponse)) {
            return true;
        }
        String formattedAmount = plugin.getCurrencyFormatter().formatPriceForMessage(amount, currency);
        java.util.HashMap<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("name", String.valueOf(args[0]));
        placeholders.put("amount", String.valueOf(formattedAmount));
        placeholders.put("currency", String.valueOf(currency));
        com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "deposited", placeholders);
        return true;
    }
    private boolean handleEconomyFailure(CommandSender sender, EconomyResponse response) {
        if (response == null || response.type == EconomyResponse.ResponseType.FAILURE
            || response.type == EconomyResponse.ResponseType.NOT_IMPLEMENTED) {
            String message = response == null ? "Bank operation failed." : response.errorMessage;
            if (message == null || message.isBlank()) {
                message = "Bank operation failed.";
            }
            sender.sendMessage(com.skyblockexp.ezeconomy.util.MessageUtils.color(plugin, message));
            return true;
        }
        return false;
    }
}
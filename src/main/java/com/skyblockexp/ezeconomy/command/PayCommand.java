
package com.skyblockexp.ezeconomy.command;

import com.skyblockexp.ezeconomy.core.MessageProvider;
import com.skyblockexp.ezeconomy.util.NumberUtil;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.storage.TransferResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

public class PayCommand implements CommandExecutor {
    private final EzEconomyPlugin plugin;

    public PayCommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageProvider messages = plugin.getMessageProvider();
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.color(messages.get("only_players")));
            return true;
        }
        if (!sender.hasPermission("ezeconomy.pay")) {
            sender.sendMessage(messages.color(messages.get("no_permission")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.color(messages.get("usage_pay")));
            return true;
        }
        Player from = (Player) sender;
        // Prefer online exact match to avoid blocking lookups.
        Player online = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer to = (online != null) ? online : null;
        double amount = NumberUtil.parseAmount(args[1]);

        // Validate amount first
        if (Double.isNaN(amount)) {
            sender.sendMessage(messages.color(messages.get("invalid_amount")));
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage(messages.color(messages.get("must_be_positive")));
            return true;
        }

        // If recipient is online we can perform the transfer synchronously (fast path)
        double netAmount = amount;
        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) {
            return true;
        }

        if (to != null) {
            // Validate not paying self
            if (from.getUniqueId().equals(to.getUniqueId())) {
                sender.sendMessage(messages.color(messages.get("cannot_pay_self")));
                return true;
            }

            TransferResult transfer = storage.transfer(from.getUniqueId(), to.getUniqueId(), plugin.getDefaultCurrency(), amount, netAmount);
            if (!transfer.isSuccess()) {
                sender.sendMessage(messages.color(messages.get("not_enough_money")));
                return true;
            }

            // Success messages
            sender.sendMessage(messages.color(messages.get("paid", java.util.Map.of(
                "player", to.getName(),
                "amount", plugin.getEconomy().format(netAmount)
            ))));
            if (to.isOnline() && to.getPlayer() != null) {
                to.getPlayer().sendMessage(messages.color(messages.get("received", java.util.Map.of(
                    "player", from.getName(),
                    "amount", plugin.getEconomy().format(netAmount)
                ))));
            }
            return true;
        }

        // Recipient not online — perform a safe async lookup and transfer to avoid blocking the main thread
        UUID fromUuid = from.getUniqueId();
        String currency = plugin.getDefaultCurrency();
        String nameArg = args[0];
        String fromName = from.getName();

        CompletableFuture.supplyAsync(() -> {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(nameArg);
            if (offline == null) {
                return null;
            }
            // Prevent paying self when resolved offline
            if (offline.getUniqueId().equals(fromUuid)) {
                return new Object[] { "SELF" };
            }
            TransferResult res = storage.transfer(fromUuid, offline.getUniqueId(), currency, amount, netAmount);
            return new Object[] { offline, res };
        }).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result == null) {
                    sender.sendMessage(messages.color(messages.get("player_not_found")));
                    return;
                }
                if (result.length == 1 && "SELF".equals(result[0])) {
                    sender.sendMessage(messages.color(messages.get("cannot_pay_self")));
                    return;
                }
                OfflinePlayer offline = (OfflinePlayer) result[0];
                TransferResult tr = (TransferResult) result[1];
                if (!tr.isSuccess()) {
                    sender.sendMessage(messages.color(messages.get("not_enough_money")));
                    return;
                }
                sender.sendMessage(messages.color(messages.get("paid", java.util.Map.of(
                    "player", offline.getName(),
                    "amount", plugin.getEconomy().format(netAmount)
                ))));
                if (offline.isOnline() && offline.getPlayer() != null) {
                    offline.getPlayer().sendMessage(messages.color(messages.get("received", java.util.Map.of(
                        "player", fromName,
                        "amount", plugin.getEconomy().format(netAmount)
                    ))));
                }
            });
        });

        return true;
    }
}

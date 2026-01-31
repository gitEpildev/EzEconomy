
package com.skyblockexp.ezeconomy.command;

import com.skyblockexp.ezeconomy.core.MessageProvider;
import com.skyblockexp.ezeconomy.util.NumberUtil;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.storage.TransferResult;
import com.skyblockexp.ezeconomy.api.events.PlayerPayPlayerEvent;
import java.math.BigDecimal;
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

        // If recipient resolved as online, handle synchronously (fast path)
        if (to != null) {
            // Prevent paying self
            if (from.getUniqueId().equals(to.getUniqueId())) {
                sender.sendMessage(messages.color(messages.get("cannot_pay_self")));
                return true;
            }

            // Fire cancellable event on main thread
            PlayerPayPlayerEvent payEvent = new PlayerPayPlayerEvent(from.getUniqueId(), to.getUniqueId(), BigDecimal.valueOf(amount));
            Bukkit.getPluginManager().callEvent(payEvent);
            if (payEvent.isCancelled()) {
                String reason = payEvent.getCancelReason();
                if (reason != null && !reason.isEmpty()) {
                    sender.sendMessage(reason);
                } else {
                    sender.sendMessage(messages.color("&cPayment cancelled."));
                }
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
        // Resolve offline player: prefer matching an existing OfflinePlayer by name, fallback to Bukkit.getOfflinePlayer
        OfflinePlayer offline = null;
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getName() != null && p.getName().equalsIgnoreCase(nameArg)) {
                offline = p;
                break;
            }
        }
        if (offline == null) {
            offline = Bukkit.getOfflinePlayer(nameArg);
        }
        if (offline == null) {
            sender.sendMessage(messages.color(messages.get("player_not_found")));
            return true;
        }

        if (offline.getUniqueId().equals(fromUuid)) {
            sender.sendMessage(messages.color(messages.get("cannot_pay_self")));
            return true;
        }
        // If offline player hasn't played before, ensure storage contains a record for them
        boolean exists = offline.hasPlayedBefore() || storage.getAllBalances(currency).containsKey(offline.getUniqueId());
        if (!exists) {
            sender.sendMessage(messages.color(messages.get("player_not_found")));
            return true;
        }

        PlayerPayPlayerEvent payEvent = new PlayerPayPlayerEvent(fromUuid, offline.getUniqueId(), BigDecimal.valueOf(amount));
        Bukkit.getPluginManager().callEvent(payEvent);
        if (payEvent.isCancelled()) {
            String reason = payEvent.getCancelReason();
            if (reason != null && !reason.isEmpty()) {
                sender.sendMessage(reason);
            } else {
                sender.sendMessage(messages.color("&cPayment cancelled."));
            }
            return true;
        }

        TransferResult tr = storage.transfer(fromUuid, offline.getUniqueId(), currency, amount, netAmount);
        if (!tr.isSuccess()) {
            sender.sendMessage(messages.color(messages.get("not_enough_money")));
            return true;
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

        return true;
    }
}


package com.skyblockexp.ezeconomy.command;

import com.skyblockexp.ezeconomy.util.MessageUtils;
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
        if (!(sender instanceof Player)) {
            MessageUtils.send(sender, plugin, "only_players");
            return true;
        }
        if (!sender.hasPermission("ezeconomy.pay")) {
            MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }
        if (args.length < 2) {
            MessageUtils.send(sender, plugin, "usage_pay");
            return true;
        }
        Player from = (Player) sender;
        // Prefer online exact match to avoid blocking lookups.
        Player online = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer to = (online != null) ? online : null;
        double amount = NumberUtil.parseAmount(args[1]);

        // Validate amount first
        if (Double.isNaN(amount)) {
            MessageUtils.send(sender, plugin, "invalid_amount");
            return true;
        }
        if (amount <= 0) {
            MessageUtils.send(sender, plugin, "must_be_positive");
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
                MessageUtils.send(sender, plugin, "cannot_pay_self");
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
                    MessageUtils.send(sender, plugin, "payment_cancelled");
                }
                return true;
            }

            TransferResult transfer = storage.transfer(from.getUniqueId(), to.getUniqueId(), plugin.getDefaultCurrency(), amount, netAmount);
            if (!transfer.isSuccess()) {
                MessageUtils.send(sender, plugin, "not_enough_money");
                return true;
            }

            // Success messages
            MessageUtils.send(sender, plugin, "paid", java.util.Map.of(
                "player", to.getName(),
                "amount", plugin.getEconomy().format(netAmount)
            ));
            if (to.isOnline() && to.getPlayer() != null) {
                MessageUtils.send(to.getPlayer(), plugin, "received", java.util.Map.of(
                    "player", from.getName(),
                    "amount", plugin.getEconomy().format(netAmount)
                ));
            }

            return true;
        }

        // Recipient not online — perform a safe async lookup and transfer to avoid blocking the main thread
        UUID fromUuid = from.getUniqueId();
        String currency = plugin.getDefaultCurrency();
        String nameArg = args[0];
        String fromName = from.getName();
        // Resolve offline player directly to avoid iterating all offline players
        OfflinePlayer offline = Bukkit.getOfflinePlayer(nameArg);
        if (offline == null) {
            MessageUtils.send(sender, plugin, "player_not_found");
            return true;
        }


        if (offline.getUniqueId().equals(fromUuid)) {
            MessageUtils.send(sender, plugin, "cannot_pay_self");
            return true;
        }
        // If offline player hasn't played before, ensure storage contains a record for them
        java.util.Map<UUID, Double> all = storage.getAllBalances(currency);
        boolean exists = offline.hasPlayedBefore() || all.containsKey(offline.getUniqueId());
        if (!exists) {
            MessageUtils.send(sender, plugin, "player_not_found");
            return true;
        }

        PlayerPayPlayerEvent payEvent = new PlayerPayPlayerEvent(fromUuid, offline.getUniqueId(), BigDecimal.valueOf(amount));
        Bukkit.getPluginManager().callEvent(payEvent);
        if (payEvent.isCancelled()) {
            String reason = payEvent.getCancelReason();
            if (reason != null && !reason.isEmpty()) {
                sender.sendMessage(reason);
            } else {
                MessageUtils.send(sender, plugin, "payment_cancelled");
            }
            return true;
        }

        TransferResult tr = storage.transfer(fromUuid, offline.getUniqueId(), currency, amount, netAmount);
        if (!tr.isSuccess()) {
            MessageUtils.send(sender, plugin, "not_enough_money");
            return true;
        }

        MessageUtils.send(sender, plugin, "paid", java.util.Map.of(
            "player", offline.getName(),
            "amount", plugin.getEconomy().format(netAmount)
        ));
        if (offline.isOnline() && offline.getPlayer() != null) {
            MessageUtils.send(offline.getPlayer(), plugin, "received", java.util.Map.of(
                "player", fromName,
                "amount", plugin.getEconomy().format(netAmount)
            ));
        }

        return true;
    }
}

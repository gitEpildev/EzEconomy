
package com.skyblockexp.ezeconomy.command;

import com.skyblockexp.ezeconomy.util.MessageUtils;
import com.skyblockexp.ezeconomy.util.NumberUtil;
import com.skyblockexp.ezeconomy.core.Money;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import java.math.BigDecimal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
 

public class PayCommand implements CommandExecutor {
    private final EzEconomyPlugin plugin;
    // Pending confirmation transfers are managed by PayFlowManager

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
        // Support: /pay confirm
        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            if (!(sender instanceof Player)) {
                MessageUtils.send(sender, plugin, "only_players");
                return true;
            }
            Player p = (Player) sender;
            var pt = plugin.getPayFlowManager().pollPendingTransfer(p.getUniqueId());
            if (pt == null || System.currentTimeMillis() > pt.getExpiresAtMillis()) {
                MessageUtils.send(sender, plugin, "no_pending_payment");
                return true;
            }
            // Execute pending transfer via PaymentExecutor
            java.math.BigDecimal amountDecimal = pt.getAmount().getAmount();
            String toName = pt.getToName();
            com.skyblockexp.ezeconomy.service.PaymentExecutor.execute(plugin, p, toName, amountDecimal, pt.getCurrency());
            return true;
        }

        if (args.length < 2) {
            MessageUtils.send(sender, plugin, "usage_pay");
            return true;
        }
        // Accept either: /pay <player> <amount> OR /pay <player> <amount> <currency>
        if (args.length > 3) {
            MessageUtils.send(sender, plugin, "usage_pay");
            return true;
        }
        Player from = (Player) sender;
        // Prefer online exact match to avoid blocking lookups.
        Player online = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer to = (online != null) ? online : null;
        // Determine currency (optional 3rd arg) — use before parsing so parseMoney can know currency context
        final String currency;
        if (args.length == 3) {
            String tmpCurrency = args[2].toLowerCase();
            java.util.Map<String, Object> currencies = plugin.getConfig().getConfigurationSection("multi-currency.currencies") != null
                ? plugin.getConfig().getConfigurationSection("multi-currency.currencies").getValues(false)
                : java.util.Collections.emptyMap();
            if (!currencies.containsKey(tmpCurrency)) {
                MessageUtils.send(sender, plugin, "unknown_currency", java.util.Map.of("currency", tmpCurrency));
                return true;
            }
            currency = tmpCurrency;
        } else {
            currency = plugin.getDefaultCurrency();
        }

        Money money = NumberUtil.parseMoney(args[1], currency);

        // Validate amount and parsing
        if (money == null) {
            MessageUtils.send(sender, plugin, "invalid_amount", java.util.Map.of("input", args[1]));
            return true;
        }
        java.math.BigDecimal amountDecimal = money.getAmount();
        if (amountDecimal.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            MessageUtils.send(sender, plugin, "must_be_positive");
            return true;
        }

        // Legacy validations removed; using Money/BigDecimal checks above


        // Confirmation threshold check: if configured and amount >= threshold, create pending transfer
        double threshold = plugin.getConfig().getDouble("pay.confirmation.threshold", -1.0);
        int timeoutSeconds = plugin.getConfig().getInt("pay.confirmation.timeout_seconds", 30);
        boolean requireConfirm = threshold >= 0 && amountDecimal.compareTo(java.math.BigDecimal.valueOf(threshold)) >= 0;
        if (requireConfirm && !args[1].equalsIgnoreCase("confirm")) {
            // Store pending transfer via PayFlowManager and instruct user to confirm
            long expiresAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);
            plugin.getPayFlowManager().createPendingTransfer(from.getUniqueId(), to == null ? null : to.getUniqueId(), args[0], money, currency, expiresAt);
            MessageUtils.send(sender, plugin, "payment_confirm_required", java.util.Map.of("amount", plugin.format(amountDecimal.doubleValue(), currency), "timeout", String.valueOf(timeoutSeconds)));
            // Schedule cleanup
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getPayFlowManager().removeIfExpired(from.getUniqueId()), timeoutSeconds * 20L);
            return true;
        }

        // Delegate the actual transfer logic to PaymentExecutor (handles online/offline flows)
        // If the recipient is currently online, run synchronously so callers see immediate result in tests.
        // If recipient is offline, run asynchronously to avoid blocking I/O.
        // Determine whether the recipient is known to the server (main-thread check)
        boolean knownOffline = false;
        if (online != null) {
            knownOffline = true;
        } else {
            OfflinePlayer sample = Bukkit.getOfflinePlayer(args[0]);
            if (sample != null) {
                // Consider known if has played before or if storage already contains a balance entry for this UUID
                if (sample.hasPlayedBefore()) {
                    knownOffline = true;
                } else {
                    try {
                        var storage = plugin.getStorageOrWarn();
                        if (storage != null) {
                            java.util.Map<java.util.UUID, Double> all = storage.getAllBalances(currency);
                            if (all.containsKey(sample.getUniqueId())) {
                                knownOffline = true;
                            }
                        }
                    } catch (Exception ignored) {
                        // If storage lookup fails, fall back to scanning Bukkit.getOfflinePlayers()
                    }
                }
            }
            if (!knownOffline) {
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    if (op.getName() != null && op.getName().equalsIgnoreCase(args[0])) {
                        knownOffline = true;
                        break;
                    }
                }
            }
        }

        if (online != null) {
            // Execute immediately on current thread for online recipient (tests expect immediate result)
            com.skyblockexp.ezeconomy.service.PaymentExecutor.execute(plugin, from, args[0], amountDecimal, currency, knownOffline);
        } else {
            boolean ko = knownOffline;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                com.skyblockexp.ezeconomy.service.PaymentExecutor.execute(plugin, from, args[0], amountDecimal, currency, ko);
            });
        }
        return true;
    }

    // executeTransfer removed: PaymentExecutor centralizes transfer logic
}

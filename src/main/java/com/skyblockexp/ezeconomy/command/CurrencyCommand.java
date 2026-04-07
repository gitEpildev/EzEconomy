package com.skyblockexp.ezeconomy.command;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;
import com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import com.skyblockexp.ezeconomy.util.NumberUtil;
import com.skyblockexp.ezeconomy.core.Money;
import com.skyblockexp.ezeconomy.util.CurrencyUtil;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;

public class CurrencyCommand implements CommandExecutor {
	private final EzEconomyPlugin plugin;

	public CurrencyCommand(EzEconomyPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// MessageProvider replaced by MessageUtils
		FileConfiguration config = plugin.getConfig();
		boolean multiEnabled = config.getBoolean("multi-currency.enabled", false);
		if (!multiEnabled) {
			MessageUtils.send(sender, plugin, "multi_currency_disabled");
			return true;
		}

		if (!(sender instanceof Player)) {
			MessageUtils.send(sender, plugin, "only_players");
			return true;
		}
		Player player = (Player) sender;
		CurrencyPreferenceManager preferenceManager = plugin.getCurrencyPreferenceManager();

		Map<String, Object> currencies = config.getConfigurationSection("multi-currency.currencies").getValues(false);
		String preferred = preferenceManager.getPreferredCurrency(player.getUniqueId());

		if (args.length == 0) {
			MessageUtils.send(sender, plugin, "preferred_currency", Map.of("currency", preferred));
			MessageUtils.send(sender, plugin, "available_currencies");
			for (String currency : currencies.keySet()) {
				sender.sendMessage(" - " + currency);
			}
			MessageUtils.send(sender, plugin, "use_currency");
			return true;
		}
		// Support: /currency convert <from> <to> <amount>
		if (args[0].equalsIgnoreCase("convert")) {
			if (args.length != 4) {
				MessageUtils.send(sender, plugin, "usage_currency");
				return true;
			}
			String from = args[1].toLowerCase();
			String to = args[2].toLowerCase();
			if (!currencies.containsKey(from)) {
				MessageUtils.send(sender, plugin, "unknown_currency", Map.of("currency", from));
				return true;
			}
			if (!currencies.containsKey(to)) {
				MessageUtils.send(sender, plugin, "unknown_currency", Map.of("currency", to));
				return true;
			}
			Money m = NumberUtil.parseMoney(args[3], from);
			if (m == null || m.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
				MessageUtils.send(sender, plugin, "invalid_amount", java.util.Map.of("input", args[3]));
				return true;
			}
			BigDecimal amt = m.getAmount();
			var res = CurrencyUtil.convertBigDecimal(plugin, amt, from, to);
			if (res == null) {
				MessageUtils.send(sender, plugin, "unknown_conversion");
				return true;
			}

			if (res.converted == null || res.converted.compareTo(BigDecimal.ZERO) == 0) {
				MessageUtils.send(sender, plugin, "conversion_too_small");
				return true;
			}

			var storage = plugin.getStorageOrWarn();
			if (storage == null) {
				MessageUtils.send(sender, plugin, "storage_unavailable");
				return true;
			}

			BigDecimal usedSource = res.usedSource == null ? amt : res.usedSource;

			double balance = storage.getBalance(player.getUniqueId(), from);
			// Require the player to have at least the requested amount, and also ensure
			// they have enough to cover the actual source used for an integer conversion.
			if (BigDecimal.valueOf(balance).compareTo(amt) < 0) {
				MessageUtils.send(sender, plugin, "not_enough_money");
				return true;
			}
			if (BigDecimal.valueOf(balance).compareTo(usedSource) < 0) {
				MessageUtils.send(sender, plugin, "not_enough_money");
				return true;
			}

			// Attempt an atomic withdraw; tryWithdraw will fail if concurrent change removed funds
			boolean withdrawn = storage.tryWithdraw(player.getUniqueId(), from, usedSource.doubleValue());
			if (!withdrawn) {
				MessageUtils.send(sender, plugin, "not_enough_money");
				return true;
			}

			// Deposit converted amount to target currency
			storage.deposit(player.getUniqueId(), to, res.converted.doubleValue());

			// Log both sides of the conversion as transactions
			long now = System.currentTimeMillis();
			plugin.getTransactionMetricsService().logTransaction(new com.skyblockexp.ezeconomy.api.storage.models.Transaction(player.getUniqueId(), from, -usedSource.doubleValue(), now));
			plugin.getTransactionMetricsService().logTransaction(new com.skyblockexp.ezeconomy.api.storage.models.Transaction(player.getUniqueId(), to, res.converted.doubleValue(), now));
			// Record conversion metric (converted amount in target currency)
			try { plugin.getTransactionMetricsService().recordConversion(res.converted.doubleValue()); } catch (Throwable ignored) {}

			String fromDisplay = plugin.getCurrencyFormatter().formatPriceForMessage(usedSource.doubleValue(), from);
			String toDisplay = plugin.getCurrencyFormatter().formatPriceForMessage(res.converted.doubleValue(), to);
			sender.sendMessage(plugin.getMessageProvider().color("&eConversion: " + fromDisplay + " → " + toDisplay));
			sender.sendMessage(plugin.getMessageProvider().color("&aConversion successful."));
			return true;
		}

		String newCurrency = args[0].toLowerCase();
		if (!currencies.containsKey(newCurrency)) {
			MessageUtils.send(sender, plugin, "unknown_currency", Map.of("currency", newCurrency));
			return true;
		}

		// Set preferred currency (demo: metadata)
		preferenceManager.setPreferredCurrency(player.getUniqueId(), newCurrency);
		MessageUtils.send(sender, plugin, "set_currency", Map.of("currency", newCurrency));
		return true;
	}
}

package com.skyblockexp.ezeconomy.command;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;
import com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Map;

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

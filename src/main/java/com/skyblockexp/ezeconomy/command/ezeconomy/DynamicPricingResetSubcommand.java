package com.skyblockexp.ezeconomy.command.ezeconomy;

import com.skyblockexp.ezeconomy.command.Subcommand;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;
import org.bukkit.command.CommandSender;

import java.io.File;

/**
 * Subcommand for /ezeconomy dynamic reset - resets runtime dynamic pricing data.
 */
public class DynamicPricingResetSubcommand implements Subcommand {
    private final EzEconomyPlugin plugin;

    public DynamicPricingResetSubcommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ezeconomy.dynamic.reset")) {
            MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }

        boolean enabled = plugin.getConfig().getBoolean("dynamic-pricing.enabled", false);
        File dynFile = new File(plugin.getDataFolder(), "dynamic-pricing.yml");

        if (!enabled && !dynFile.exists()) {
            MessageUtils.send(sender, plugin, "dynamic_pricing_not_enabled");
            return true;
        }

        if (args.length < 1 || !args[0].equalsIgnoreCase("confirm")) {
            sender.sendMessage(MessageUtils.color(plugin, "&cThis will reset any runtime dynamic pricing adjustments."));
            sender.sendMessage(MessageUtils.color(plugin, "&cUse &f/ezeconomy dynamic reset confirm &cto proceed."));
            return true;
        }

        // Attempt to remove a dynamic-pricing file if present (legacy/runtime store)
        if (dynFile.exists()) {
            if (!dynFile.delete()) {
                sender.sendMessage(MessageUtils.color(plugin, "&cFailed to delete dynamic-pricing.yml. Check file permissions."));
                return true;
            }
        }

        // No in-memory runtime system exists currently; inform success.
        MessageUtils.send(sender, plugin, "dynamic_pricing_reset_success");
        return true;
    }
}

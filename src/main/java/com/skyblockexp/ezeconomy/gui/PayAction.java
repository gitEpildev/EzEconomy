package com.skyblockexp.ezeconomy.gui;

import org.bukkit.entity.Player;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class PayAction extends GuiAction {
    public PayAction() {
        super("pay", "\u00A7ePay");
    }

    @Override
    public void open(EzEconomyPlugin plugin, Player player) {
        PayPlayerSelectionGui.open(plugin, player);
    }
}

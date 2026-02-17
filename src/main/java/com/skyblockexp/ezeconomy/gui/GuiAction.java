package com.skyblockexp.ezeconomy.gui;

import org.bukkit.entity.Player;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

/**
 * Abstract GUI action hook for user-facing actions.
 */
public abstract class GuiAction {
    private final String key;
    private final String displayName;

    protected GuiAction(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public abstract void open(EzEconomyPlugin plugin, Player player);
}

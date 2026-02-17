package com.skyblockexp.ezeconomy.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * InventoryHolder used to reliably identify plugin GUIs regardless of title.
 */
public class GuiInventoryHolder implements InventoryHolder {
    private final String id;

    public GuiInventoryHolder(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

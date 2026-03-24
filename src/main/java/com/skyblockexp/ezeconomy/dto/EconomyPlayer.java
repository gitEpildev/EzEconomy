package com.skyblockexp.ezeconomy.dto;

import java.util.UUID;

/**
 * Lightweight DTO representing a player with UUID, name and display name.
 */
public class EconomyPlayer {
    private final UUID uuid;
    private final String name;
    private final String displayName;

    public EconomyPlayer(UUID uuid, String name, String displayName) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
}

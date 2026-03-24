package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.dto.EconomyPlayer;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import com.skyblockexp.ezeconomy.util.PlayerUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetPlayerTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setup() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EzEconomyPlugin.class);
    }

    @AfterEach
    public void teardown() {
        TestSupport.cleanupTestDataFolder(plugin, "test-data");
        MockBukkit.unmock();
    }

    @Test
    public void testYmlStoragePersistsNameAndDisplayName() throws Exception {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("yml.data-folder", "test-data");
        cfg.set("yml.per-player-file-naming", "uuid");
        YMLStorageProvider yml = new YMLStorageProvider(plugin, cfg);

        TestSupport.createTestDataFolder(plugin, "test-data");
        TestSupport.injectField(plugin, "storage", yml);

        // create offline recipient
        Object offlineObj;
        try {
            java.lang.reflect.Method addOffline = server.getClass().getMethod("addOfflinePlayer", String.class);
            offlineObj = addOffline.invoke(server, "ymlOffline");
        } catch (NoSuchMethodException ignored) {
            offlineObj = org.bukkit.Bukkit.getOfflinePlayer("ymlOffline");
        }
        org.bukkit.OfflinePlayer offline = (org.bukkit.OfflinePlayer) offlineObj;

        // Trigger a write so the YML file is created and saved
        yml.setBalance(offline.getUniqueId(), "dollar", 1.0);

        EconomyPlayer p = yml.getPlayer(offline.getUniqueId());
        assertEquals("ymlOffline", p.getName());
        assertEquals("ymlOffline", p.getDisplayName());
    }

    @Test
    public void testPlayerUtilPrefersOnlineDisplayName() throws Exception {
        // create online player
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "onlineOne");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        sender.setDisplayName("CoolName");

        // PlayerUtil should prefer the online player's display name
        com.skyblockexp.ezeconomy.dto.EconomyPlayer p = PlayerUtil.getPlayer(sender.getUniqueId());
        assertEquals("onlineOne", p.getName());
        assertEquals("CoolName", p.getDisplayName());
    }
}

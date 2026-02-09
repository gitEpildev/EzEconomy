package com.skyblockexp.ezeconomy;

import com.skyblockexp.ezeconomy.core.MessageProvider;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageProviderTest {

    private FileConfiguration loadLang(String resourcePath) throws Exception {
        try (var is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) throw new IllegalStateException("Resource not found: " + resourcePath);
            var reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    @Test
    public void paidOtherCurrencyContainsDefaultAmount() throws Exception {
        FileConfiguration selected = loadLang("/languages/en.yml");
        FileConfiguration fallback = selected;
        MessageProvider provider = new MessageProvider(selected, fallback, "en");

        String out = provider.get("paid_other_currency", Map.of(
                "player", "Bob",
                "amount", "$100",
                "amount_default", "$95"
        ));

        // Should include the payer/player text and the default equivalent amount
        assertTrue(out.contains("You paid Bob"), "Rendered message should contain player and action");
        assertTrue(out.contains("≈ $95") || out.contains("(≈ $95)"), "Rendered message should include amount_default equivalent");
    }

    @Test
    public void receivedOtherCurrencyContainsDefaultAmount() throws Exception {
        FileConfiguration selected = loadLang("/languages/en.yml");
        FileConfiguration fallback = selected;
        MessageProvider provider = new MessageProvider(selected, fallback, "en");

        String out = provider.get("received_other_currency", Map.of(
                "player", "Alice",
                "amount", "$42",
                "amount_default", "$40"
        ));

        assertTrue(out.contains("received"), "Rendered message should mention received");
        assertTrue(out.contains("≈ $40") || out.contains("(≈ $40)"), "Rendered message should include amount_default equivalent");
    }
}

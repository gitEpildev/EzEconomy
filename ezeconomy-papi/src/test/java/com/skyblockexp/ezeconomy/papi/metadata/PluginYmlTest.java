package com.skyblockexp.ezeconomy.papi.metadata;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class PluginYmlTest {

    @Test
    public void pluginYmlContainsExpectedFields() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("plugin.yml");
        assertNotNull(is, "plugin.yml must be present on the classpath");
        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(content.contains("version: 1.0.0"), "plugin.yml should contain version 1.0.0");
        assertTrue(content.contains("main: com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin"), "plugin.yml should declare correct main class");
        assertTrue(content.contains("PlaceholderAPI"), "plugin.yml should depend on PlaceholderAPI");
    }
}

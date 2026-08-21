package com.gitepildev.giteconomy.papi.testhelpers;

import com.gitepildev.giteconomy.cache.CacheManager;
import com.gitepildev.giteconomy.cache.CachingStrategy;
import com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockbukkit.mockbukkit.MockBukkit;

public class TestLifecycleExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Intentionally left blank: tests control when to mock MockBukkit.
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        try { MockBukkit.unmock(); } catch (Throwable ignored) {}
        GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
        try { CacheManager.setStrategy(CachingStrategy.LOCAL); } catch (Throwable ignored) {}
    }
}

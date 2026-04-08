package com.skyblockexp.ezeconomy.papi.testhelpers;

import com.skyblockexp.ezeconomy.cache.CacheManager;
import com.skyblockexp.ezeconomy.cache.CachingStrategy;
import com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion;
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
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
        try { CacheManager.setStrategy(CachingStrategy.LOCAL); } catch (Throwable ignored) {}
    }
}

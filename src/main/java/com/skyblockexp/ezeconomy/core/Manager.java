package com.skyblockexp.ezeconomy.core;

/**
 * Base lifecycle interface for managers registered in the Registry.
 */
public abstract class Manager {
    /** Called during startup/initialization. Default no-op. */
    public void init() throws Exception {}

    /** Called during shutdown/cleanup. Default no-op. */
    public void shutdown() throws Exception {}
}

Test conventions for ezeconomy-papi

- Use `TestBase` as the base class for unit tests. It is annotated with the `TestLifecycleExtension` which handles MockBukkit lifecycle and common cleanup.
- Place lightweight test helpers and stubs in `src/test/java/com/skyblockexp/ezeconomy/papi/testhelpers`.
- Tests should avoid calling `MockBukkit.mock()` or `MockBukkit.unmock()` directly unless they need custom setup; the extension handles it.
- Use `EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS` to inject test doubles (`TestEzEconomy`) for deterministic behaviour.
- Keep tests focused and avoid relying on long async waits; prefer injection of test doubles when possible.

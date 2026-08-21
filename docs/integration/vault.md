# Vault Integration

Overview
- GitEconomy registers as an economy provider via Vault so other plugins (shops, NPCs, protection plugins) can use the economy without a direct dependency.

Installation & setup
1. Install Vault on your Bukkit/Spigot/Paper server.
2. Ensure GitEconomy (`giteconomy-bukkit`) is installed and enabled.
3. Vault will automatically detect GitEconomy as an `Economy` service if the plugin registers with the `ServicesManager`.

Behavior
- When enabled, GitEconomy implements the standard Vault `Economy` interface and supports balance lookups, deposits, withdrawals, and formatting.
- Bank support is not provided (`hasBankSupport()` returns `false`). Vault bank APIs are unsupported.
- No extra configuration is needed by default; multi-currency mapping may require platform-specific settings — see `config.yml`.

Troubleshooting
- Vault not listing GitEconomy: confirm GitEconomy loaded after Vault and check server logs for the `ServicesManager` registration entry.
- Plugin compatibility issues: verify Vault and server build versions; enable debug logging in `config.yml` to view registration details.

See also
- [docs/integration/placeholderapi.md](placeholderapi.md)

# Multi-Currency Support

Overview
- GitEconomy supports multiple currencies concurrently so servers can offer region-specific currencies, tokens, or secondary economies (e.g., `coins`, `gems`, `dollars`). Each currency is defined in the configuration and has its own formatting, symbol, and conversion rules.

Configuration
- Define currencies in `config.yml` under the `multi-currency` / `currencies` section. Each entry includes:
	- currency key - internal identifier (lowercase, no spaces)
	- `display` - human-friendly name
	- `symbol` - short symbol shown in formatted outputs
	- `decimals` - number of decimal places (use integers; server-side arithmetic uses `BigDecimal` or scaled long)
	- `conversion` - optional conversion rates between currencies

Usage
- Commands and API methods accept an optional currency parameter. If omitted, the server's default currency is used.
- Examples:
	- `/balance` - shows balance in preferred/default currency.
	- `/balance coins` - shows balance in `coins` currency.

Conversions
- If conversion rates are configured, GitEconomy can convert balances between currencies for display or transactions. Conversion uses configured rates and respects decimal settings to avoid rounding errors.

API
- The public API exposes currency-aware methods such as `getBalance(uuid, currency)`, `deposit(uuid, currency, amount)`, and related transfer helpers on `GitEconomyAPI`.

Best practices
- Prefer `BigDecimal`-backed currencies for high-precision economies (e.g., with interest or fractional rates).
- Keep conversion rates in sync across distributed deployments; consider storing conversion rules in a central datastore if using multiple server instances.

See also
- [docs/configuration.md](../configuration.md)
- [docs/integration/vault.md](../integration/vault.md)

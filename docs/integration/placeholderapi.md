# PlaceholderAPI Integration

Overview
- GitEconomy provides a PlaceholderAPI (PAPI) expansion so server owners can display balances, symbols, and top lists in chat, scoreboards, and other plugins.

Why enable
- Easy integration into existing PAPI-supported plugins and server-side displays.
- Low overhead: placeholders fetch data from GitEconomy with optional caching to reduce storage calls.

Installation
1. Install PlaceholderAPI on your server.
2. Place the `giteconomy-papi` plugin jar in your `plugins/` folder (this module is included in the repository as `giteconomy-papi`).
3. Restart the server; PAPI should detect the expansion automatically.

Available placeholders
- `%giteconomy_balance%` - player's balance using preferred currency and default formatting.
- `%giteconomy_balance_formatted%` - formatted balance with currency symbol.
- `%giteconomy_balance_<currency>%` - balance in specific currency (e.g., `%giteconomy_balance_dollar%`).
- `%giteconomy_symbol_<currency>%` - currency symbol (e.g., `%giteconomy_symbol_dollar%`).
- `%giteconomy_top_<n>_<currency>%` - top `n` players for given currency (e.g., `%giteconomy_top_10_dollar%`).
- `%giteconomy_currency%` - player's preferred currency key.

Caching and performance
- The PAPI expansion uses the global `CacheProvider` selected by `caching-strategy` (default `LOCAL`).
- `top` placeholders are cached for a short TTL (default 30s) and refreshed asynchronously to avoid blocking placeholder resolution.

Permissions and safety
- No special permissions are required to use placeholders.
- Be mindful of placeholder usage in high-frequency contexts (e.g., scoreboard with many players); use caching or server-side limits.

Troubleshooting
- Placeholder not resolving: ensure `giteconomy` / GitEconomy is enabled and registered with PAPI. Check server logs for expansion registration messages.
- Incorrect formatting: review your currency config in `config.yml` and `multi-currency` settings.

See also
- [docs/placeholders.md](../placeholders.md)
- [docs/feature/caching-strategy.md](../feature/caching-strategy.md)
- [docs/feature/proxy-network.md](../feature/proxy-network.md) (if using proxy-backed caching)

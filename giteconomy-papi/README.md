# GitEconomy PlaceholderAPI Expansion

This module provides a PlaceholderAPI expansion that exposes GitEconomy multi-currency placeholders.

Placeholders provided (examples):

- `%giteconomy_balance%` — player's preferred/default currency formatted
- `%giteconomy_balance_<currency>%` — player's balance for a specific currency
- `%giteconomy_symbol_<currency>%` — raw currency symbol
- `%giteconomy_top_<n>_<currency>%` — top N players for a currency (comma-separated)
- `%giteconomy_bank_<name>_<currency>%` — named bank balance for a specific currency

Build: run `mvn -DskipTests clean package` from the `giteconomy-papi` folder. Drop the produced JAR into your server `plugins/` folder alongside `GitEconomy.jar` and `PlaceholderAPI.jar`.

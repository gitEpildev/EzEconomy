# Placeholders

GitEconomy integrates with PlaceholderAPI for use in chat, scoreboards, and other plugins.

## Balance Placeholders

| Placeholder | Description |
| --- | --- |
| `%giteconomy_balance%` | Player balance in their preferred currency. |
| `%giteconomy_balance_<currency>%` | Player balance in the specified currency. |
| `%giteconomy_balance_formatted%` | Player balance formatted according to `money-format` settings and `price_message_format`. |
| `%giteconomy_balance_short%` | Compact/short form of the balance (e.g., `1.2K`, `3.4M`) when `useCompact` is enabled in config. |
| `%giteconomy_currency%` | Player's preferred currency key. |
| `%giteconomy_symbol_<currency>%` | Currency symbol for the given currency. |

## Price formatting

GitEconomy provides a language-level template to control how currency amounts are rendered in messages.

Add the `price_message_format` key to your language file under `GitEconomy/languages/` (or in your data folder language file). The template supports two placeholders:

- `{amount}`: the localized numeric amount (respecting decimals and locale settings)
- `{symbol}`: the raw currency symbol from the multi-currency config

Default (bundled):

```
price_message_format: "{amount} {symbol}"
```

Examples:

- `price_message_format: "{symbol} {amount}"` -> `$ 190`
- `price_message_format: "{amount}{symbol}"` -> `190$`
- `price_message_format: "{amount} {symbol} ({amount_default})"` -> `190 $ (≈ 170 $)`

Notes:

- The plugin supplies `{amount}` (number with sign and locale) and `{symbol}` (no surrounding spaces). Keep spacing in the template to control visual spacing.
- If the key is missing, the plugin falls back to `"{amount} {symbol}"` to preserve current behaviour.
- This template only affects how amounts are displayed in messages and placeholders; numeric parsing and storage are unchanged.

## Leaderboard Placeholders

| Placeholder | Description |
| --- | --- |
| `%giteconomy_top_1%` | Top player balance (replace `1` with rank). |
| `%giteconomy_top_2%` | Second place player balance. |
| `%giteconomy_top_<n>_<currency>%` | Top `n` balance for a specific currency. |

### Usage Examples

- `Balance: %giteconomy_balance%`
- `Euro Balance: %giteconomy_balance_euro%`
- `Top Player: %giteconomy_top_1%`

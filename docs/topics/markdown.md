# GitEconomy

**GitEconomy** - Modern, fast, and flexible Vault economy provider for Minecraft servers by **GitEpildev** & **Epildevconnect Ltd**. Supports YML, MySQL, SQLite, MongoDB, and custom storage. Optional multi-currency, async caching, and core economy commands - no banking subsystem.

**Available languages**: English, Español, Nederlands, 中国人, Français

**Full documentation**: [Available on Github.com](https://github.com/gitEpildev/GitEconomy/blob/main/README.md)

---

## ★ Our key economy features

GitEconomy is built for performance, flexibility, and ease of use. Highlights include:

- **Vault API compatible**: Works with any Vault-based plugin
- **YML, MySQL, SQLite, MongoDB, or custom storage**: Flexible, production-ready storage options
- **Thread-safe**: Robust error handling and concurrency
- **Multi-currency support**: Optional, per-player, fully configurable
- **Async caching**: Optimized for large servers
- **Core commands**: `/balance` (`/bal`), `/eco`, `/baltop` (`/top`), `/pay`, `/currency`, `/giteconomy`
- **Granular permissions**: Per-command access control (`giteconomy.*`)

---

## ⚡ Commands

- **/balance** (`/bal`): View your balance
- **/balance <player>**: View another player's balance (`giteconomy.balance.others`)
- **/eco <give|take|set> <player> <amount>**: Admin control (`giteconomy.eco`)
- **/baltop** (`/top`) **[amount]**: Show top balances (`giteconomy.baltop`)
- **/pay <player> <amount>**: Pay another player (`giteconomy.pay`)
- **/currency [currency]**: Set or view your preferred currency (`giteconomy.currency`)
- **/giteconomy cleanup**: Remove orphaned player data (`giteconomy.admin`)
- **/giteconomy reload**: Reload plugin configuration (`giteconomy.admin`)
- **/giteconomy reload messages**: Reload only the message file (`giteconomy.admin`)
- **/giteconomy database info**: Show database connection info (`giteconomy.admin`)
- **/giteconomy database test**: Test the database connection (`giteconomy.admin`)
- **/giteconomy database reset**: Reset all database tables (DANGEROUS) (`giteconomy.admin`)

---

## 🛡️ Permissions

- `giteconomy.balance.others`: View other players' balances
- `giteconomy.eco`: Use /eco admin command
- `giteconomy.pay`: Use /pay command
- `giteconomy.payall`: Use `/pay *`
- `giteconomy.currency`: Use /currency command
- `giteconomy.baltop`: Use /baltop
- `giteconomy.admin`: Use /giteconomy admin commands (cleanup, reload, database)

---

## ⚙️ Configuration Example

### `config.yml` (Only global settings):
```yaml
storage: yml
multi-currency:
  enabled: false
  default: "dollar"
  currencies:
    dollar:
      display: "Dollar"
      symbol: "$"
      decimals: 2
    euro:
      display: "Euro"
      symbol: "€"
      decimals: 2
  conversion:
    dollar:
      euro: 0.95
      gem: 0.01
    euro:
      dollar: 1.05
      gem: 0.012
    gem:
      dollar: 100
      euro: 80
```

### `config-yml.yml` (YML storage settings):
```yaml
yml:
  file: balances.yml
  per-player-file-naming: uuid
  data-folder: data
```

### `config-mysql.yml` (MySQL storage settings):
```yaml
mysql:
  host: localhost
  port: 3306
  database: giteconomy
  username: root
  password: password
  table: balances
```

### `config-sqlite.yml` (SQLite storage settings):
```yaml
sqlite:
  file: giteconomy.db
  table: balances
```

### `config-mongodb.yml` (MongoDB storage settings):
```yaml
mongodb:
  uri: mongodb://localhost:27017
  database: giteconomy
  collection: balances
```

---

## ⬇️ Installation

1. Place `giteconomy-bukkit-*.jar` in your plugins folder
2. Configure `config.yml` and the appropriate `config-*.yml` file for your storage type
3. Restart your server

---

## 🔗 Integration

- GitEconomy automatically registers as a Vault provider
- No extra setup required for Vault-compatible plugins
- Vault bank APIs are not supported
- **PlaceholderAPI support** (via `giteconomy-papi`):
  - Use placeholders in chat, scoreboard, and other plugins:
    - `%giteconomy_balance%` - Your balance
    - `%giteconomy_balance_<currency>%` - Your balance in a specific currency (e.g., `%giteconomy_balance_euro%`)
    - `%giteconomy_top_1%` - Top 1 player balance (replace 1 with rank)
    - `%giteconomy_currency%` - Your preferred currency
  - Works with all PlaceholderAPI-compatible plugins

---

## 🛠️ Developer: Custom Storage Providers

GitEconomy supports custom storage backends (YML, MySQL, SQLite, MongoDB, or your own)! You can implement your own provider for any database or storage system.

**How to add a custom provider:**

1. Implement the `StorageProvider` interface in your plugin or module.
2. Register your provider before GitEconomy loads:
   ```java
   GitEconomy.registerStorageProvider(new YourProvider(...));
   ```
3. Only one provider can be registered. If set, GitEconomy will use it instead of YML/MySQL.
4. See the [full StorageProvider reference](../api/storage-provider.md) for required methods and implementation details.

This allows you to use SQLite, MongoDB, Redis, or any other system for player balances.

---

## ❓ Support

- For help, join our [community Discord](https://discord.gg/yWP95XfmBS)

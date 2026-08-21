# Developer API (v2)

> GitEconomy is a Vault-compatible, standalone economy API with optional multi-currency support. It is designed for plugin developers who need robust player-balance economy features. Banking is not part of this API.

---

## Vault Integration

GitEconomy automatically registers as a Vault economy provider at startup. Any plugin using `net.milkbowl.vault.economy.Economy` will interact with GitEconomy without extra configuration.

**Steps:**
1. Install Vault and GitEconomy.
2. Start your server. GitEconomy will register itself as the economy provider.
3. Plugins using Vault will now use GitEconomy for all economy operations.

Vault bank methods are not supported (`hasBankSupport()` returns `false`).

---

## GitEconomyAPI Usage

The main entry point for custom integrations is the `GitEconomyAPI` class. This API is versioned and independent of Bukkit/Spigot.

Package: `com.gitepildev.giteconomy.api`

### Example: Basic Usage

```java
import com.gitepildev.giteconomy.api.GitEconomyAPI;
import com.gitepildev.giteconomy.api.storage.StorageProvider;
import java.util.UUID;

StorageProvider storage = ...; // Your storage provider or the default
GitEconomyAPI api = new GitEconomyAPI(storage);

// Get a player's balance in a specific currency
PlayerBalanceDTO balance = api.getBalance(playerUuid, "dollar");

// Deposit funds
api.deposit(playerUuid, "euro", 100.0);

// Withdraw funds
api.withdraw(playerUuid, "dollar", 50.0);

// Transfer between players
api.transfer(fromUuid, toUuid, "dollar", 25.0);
```

### Multi-Currency Support

- Use currency codes (e.g., "dollar", "euro", "gem") in all balance and transaction methods.
- Get the default currency: `api.getDefaultCurrency()`
- List all available currencies: `api.getAvailableCurrencies()`
- Check if a currency is enabled: `api.isCurrencyEnabled("euro")`

### Player Balances & Transactions

- Get a player's balance: `api.getBalance(uuid, currency)`
- Deposit/withdraw: `api.deposit(uuid, currency, amount)`, `api.withdraw(uuid, currency, amount)`
- Get all balances for a currency: `api.getAllBalances(currency)`
- Get transaction history: `api.getTransactions(uuid, currency)`
- Transfer funds: `api.transfer(fromUuid, toUuid, currency, amount)`
- Custom debit/credit transfer: `api.transfer(fromUuid, toUuid, currency, debitAmount, creditAmount)`

---

## Custom Storage Providers

You can supply your own storage backend by implementing the `StorageProvider` interface. This allows you to use custom databases or data sources for player balances and currencies.

See [api/storage-provider.md](storage-provider.md) for a full implementation guide, best practices, and example code.

---

## PlaceholderAPI

If PlaceholderAPI is installed, GitEconomy registers placeholders for player balances and top lists. See the Placeholders documentation for available keys.

---

## API Versioning

You can check the API version at runtime:

```java
String version = GitEconomyAPI.VERSION; // e.g., "2.0.0"
```

---

## `/giteconomy` Command

The `/giteconomy` admin command provides server management utilities. It supports tab completion for all subcommands and database actions.

**Subcommands:**
- `cleanup`: Remove orphaned player data from all storage types
- `reload`: Reload the plugin configuration
- `reload messages`: Reload only the message file
- `database info`: Show current database connection info
- `database test`: Test the database connection
- `database reset`: Reset all database tables (DANGEROUS)

**Tab Completion:**
- Context-aware suggestions for all subcommands and database actions
- Permission-sensitive (only shows what the user can access)

See [commands.md](../commands.md) for usage details.

---

## See Also

- [commands.md](../commands.md): Command usage and permissions
- [configuration.md](../configuration.md): Configuration options
- [storage.md](../storage/storage.md): Storage backends and setup
- [placeholders.md](../placeholders.md): PlaceholderAPI integration

- **Events:**
	- [PreTransactionEvent](event/PreTransactionEvent.md)
	- [PostTransactionEvent](event/PostTransactionEvent.md)
	- [PlayerPayPlayerEvent](event/PlayerPayPlayerEvent.md)
	- [TransactionType (enum)](event/TransactionType.md)

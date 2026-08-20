# Custom Storage Providers for GitEconomy

GitEconomy supports pluggable storage backends for player balances, currencies, and optional transaction history. You can implement your own provider to use a custom database, cloud service, or any data source.

## Overview

A storage provider is any class that implements the `StorageProvider` interface from the GitEconomy API.

- **Location:** `com.gitepildev.giteconomy.api.storage.StorageProvider`
- **Purpose:** Abstracts all persistent data operations for the plugin
- **Use Cases:**
  - Integrate with a custom SQL/NoSQL database
  - Use a remote API or cloud service
  - Add advanced caching or sharding

## Implementation Steps

1. **Implement the Interface**

   ```java
   public class MyProvider implements StorageProvider {
       // Implement all required methods
   }
   ```

2. **Register Your Provider**

   Register your provider in your plugin's `onLoad` method, before GitEconomy finishes loading:

   ```java
   import com.gitepildev.giteconomy.api.storage.StorageProvider;
   import com.gitepildev.giteconomy.GitEconomy;

   public void onLoad() {
       StorageProvider customProvider = new MyProvider(...);
       GitEconomy.registerStorageProvider(customProvider);
   }
   ```

3. **Required Methods**

   Your provider must implement all methods for:
   - Player balances (get, set, deposit, withdraw)
   - Currency-aware balance maps
   - (Optional) Transaction history
   - Lifecycle (`init`, `load`, `save`, `shutdown`)
   - Player metadata (`getPlayer`, and optionally `resolvePlayerByName` / `persistPlayerInfo`)

   See the Javadoc for `StorageProvider` for method signatures and expected behaviors.

4. **Legacy Compatibility**

   For single-currency servers, legacy overloads are provided. You must implement the multi-currency methods; legacy defaults delegate to currency `"dollar"`.

## Guidelines

- **Atomicity:** Ensure all balance changes are atomic and thread-safe.
- **Performance:** Use async IO and caching where possible. Avoid blocking the main server thread.
- **Validation:** Validate all DTOs (data transfer objects) before writing to storage.
- **Error Handling:** Throw meaningful exceptions for impossible or failed operations.
- **Migration:** If replacing the default provider, migrate data before switching in production.

## Example: Minimal Provider

```java
public class ExampleProvider implements StorageProvider {
    // Implement all required methods...
}
```

## Notes

- Only one custom provider can be registered at a time.
- Register your provider before GitEconomy finishes loading.
- See the GitEconomy source for built-in provider examples (YML, SQLite, MySQL, MongoDB).

---

# `StorageProvider` Interface Reference

This section documents the methods of the `StorageProvider` interface. Implement all required methods for a fully functional provider.

## Initialization & Lifecycle

- `void init()`  
  Initialize the storage provider. Throw `StorageInitException` on failure.
- `void load()`  
  Load data or establish connections. Throw `StorageLoadException` on failure.
- `void save()`  
  Persist any in-memory data. Throw `StorageSaveException` on failure.
- `void shutdown()`  
  Clean up and close resources.

## Player Balances

- `double getBalance(UUID uuid, String currency)`  
  Get a player's balance for a currency.
- `void setBalance(UUID uuid, String currency, double amount)`  
  Set a player's balance for a currency.
- `boolean tryWithdraw(UUID uuid, String currency, double amount)`  
  Attempt to withdraw from a player's balance. Return false if insufficient funds.
- `void deposit(UUID uuid, String currency, double amount)`  
  Deposit to a player's balance.
- `Map<UUID, Double> getAllBalances(String currency)`  
  Get all player balances for a currency.
- `boolean playerExists(UUID uuid)`  
  Return true if a record exists for the player (default falls back to non-zero balance).
- `Set<String> cleanupOrphanedPlayers()`  
  Remove balances for unknown players. Default: no-op.

## Player Metadata

- `EconomyPlayer getPlayer(UUID uuid)`  
  Return last-known name/displayName when available, or null.
- `UUID resolvePlayerByName(String name)`  
  Resolve a UUID by name (default: null).
- `void persistPlayerInfo(UUID uuid, String name, String displayName)`  
  Persist player metadata (default: no-op).

## Transactions

- `void logTransaction(Transaction transaction)`  
  Log a transaction for a player and currency.
- `List<Transaction> getTransactions(UUID uuid, String currency)`  
  Get transaction history for a player and currency.

## Transfers

- `TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount)`  
  Transfer funds between players (default: debit/credit same amount).
- `TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double debitAmount, double creditAmount)`  
  Transfer custom debit/credit amounts between players.

## Connection & Status

- `boolean isConnected()`  
  Return true if the provider is connected to its backend (default: false).

## Legacy Overloads (Single-Currency)

All legacy methods use the default currency `"dollar"`. Prefer the currency-aware overloads:
- `double getBalance(UUID uuid)`
- `void setBalance(UUID uuid, double amount)`
- `boolean tryWithdraw(UUID uuid, double amount)`
- `void deposit(UUID uuid, double amount)`
- `Map<UUID, Double> getAllBalances()`

## Thread Safety & Atomicity

- All balance and transfer operations must be atomic and thread-safe.
- Use locking or transactions as appropriate for your backend.

## Exceptions

- Throw the appropriate exception (`StorageInitException`, `StorageLoadException`, `StorageSaveException`) for lifecycle failures.
- Throw meaningful runtime exceptions for impossible or failed operations.

---

**See Also:**
- [API Reference: StorageProvider](../../src/main/java/com/gitepildev/giteconomy/api/storage/StorageProvider.java)
- [developer-api.md](../developer-api.md)
- [storage.md](../storage/storage.md)

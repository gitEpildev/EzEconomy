# EzEconomy Changelog

This file was moved from the repository root `CHANGES.md` into `docs/` to keep top-level files stable.

## 2.5.1 Fork Update - Multi-Server, Performance, Formatting

**Author:** [@GitEpildev](https://github.com/gitEpildev)  
**Original Plugin:** [@ez-plugins/EzEconomy](https://github.com/ez-plugins/EzEconomy)  
**Date:** April 2026  
**Plugin Version:** 2.5.1 (modified)

---

## Summary

These changes add full multi-server network support to EzEconomy over Velocity, rewrite the MySQL storage layer for performance with HikariCP connection pooling, and redesign all user-facing messages. Players can now seamlessly pay each other across servers, receive instant payment notifications regardless of which backend they're on, and see network-wide players in tab completion.

---

## Build Artifacts

Historical notes in this changelog may reference pre-built jars under `jars/`. Current builds produce artifacts from Maven `target/` directories.

---

## Changes

### 1. Full Multi-Server Velocity Network Support

EzEconomy now works seamlessly across a Velocity proxy network with multiple backend servers (e.g. Lobby, US, EU). This is the biggest change and touches both the Bukkit plugin and the new Velocity plugin.

**What works cross-server:**

- **Payments (`/pay`):** A player on Server A can pay a player on Server B. The money transfers instantly via the shared MySQL database, and both players get chat messages in real time.
- **Payment notifications:** When you receive money from someone on another server, you see it immediately in chat (e.g. "You received 100K $ from Epildev_TT"). No need to relog or switch servers.
- **Offline notification queue:** If the recipient is offline everywhere when the payment happens, the notification is stored in the `pending_notifications` database table and delivered the moment they join any server.
- **Network-wide tab completion:** `/pay <tab>` shows players from ALL connected servers, not just the one you're on. This also works for other commands.
- **Player name resolution:** Player names are resolved via the shared `players` database table, so paying someone by name works even if they've never been on your specific server.

**How it works:**

1. Backend servers communicate with the Velocity proxy via the `ezeconomy:notify` plugin messaging channel.
2. The Velocity plugin (`EzEconomyVelocity.java`) receives messages and forwards them to the correct backend server where the recipient is online.
3. If the recipient isn't found on any server, the Velocity plugin sends a `RECIPIENT_OFFLINE` message back, and the backend stores it in the database.
4. The Velocity plugin periodically broadcasts the full network player list to all backends for tab completion.

### 2. HikariCP Connection Pooling (`MySQLStorageProvider.java`)

Replaced the single shared `java.sql.Connection` with a HikariCP connection pool.

- **Before:** One global connection protected by `synchronized(lock)` -- every database call blocked every other call server-wide. Under any concurrency (multiple players, background tasks) the main thread would stall waiting for the lock.
- **After:** HikariCP pool with up to 10 concurrent connections. Each method gets its own connection from the pool via `try-with-resources`, so calls never block each other.

Key config:
- `maximumPoolSize = 10`
- `minimumIdle = 2`
- `connectionTimeout = 5000ms`
- `idleTimeout = 300000ms`
- `maxLifetime = 600000ms`
- Prepared statement caching enabled (`cachePrepStmts`, `prepStmtCacheSize=250`)

The HikariCP dependency is shaded and relocated to `com.skyblockexp.ezeconomy.shaded.hikari` to avoid conflicts with other plugins.

**Line count:** 1,448 lines reduced to 625 lines (57% reduction).

### 3. Atomic Transfers via MySQL Transactions

The `transfer()` method now uses proper MySQL transactions (`setAutoCommit(false)`, `commit()`, `rollback()`) on a single connection instead of separate locked calls. This guarantees atomicity -- if the debit succeeds but the credit fails, the whole operation rolls back.

### 4. Message Formatting Overhaul (`en.yml`)

Colour scheme redesigned for clarity:

| Action | Colour | Example |
|---|---|---|
| Sending money (`/pay`) | Gold (`&6`) | "You sent 10K $ to Epildev." |
| Receiving money | Green (`&a`) | "You received 10K $ from Epildev_TT." |
| Admin give (`/eco give`) | Green (`&a`) | "Gave 100K $ to Epildev." |
| Admin take (`/eco take`) | Red (`&c`) | "Took 50K $ from Epildev." |
| Errors | Red (`&c`) | "You do not have enough money." |
| Usage hints | Grey/White (`&7`/`&f`) | "Usage: /pay \\<player\\> \\<amount\\>" |
| Amounts & player names | Yellow (`&e`) | Accent colour throughout |

### 5. Baltop Redesign

`/baltop` now has a styled header with dividers, bold title, and cleaner rank formatting:

```
──────────────
✦ Balance Top - Top 10
──────────────
 #1 Epildev_TT - 1.1M $
 #2 Epildev - 483.2K $
 #3 X0Hunter - 15K $
──────────────
```

Bank info and tax totals use the same style.

### 6. Uppercase Currency Suffixes

`NumberUtil.formatShort()` now outputs uppercase suffixes: **K**, **M**, **B**, **T** instead of k, m, b, t.

- `10000` → `10K`
- `2500000` → `2.5M`
- `1000000000` → `1B`

### 7. Dedicated Admin Eco Messages

`/eco give` and `/eco take` now use their own message keys (`eco_give`, `eco_take`) instead of sharing the player payment message. This keeps admin actions visually distinct from player-to-player payments.

---

## Files Changed

| File | What changed |
|---|---|
| `pom.xml` (parent) | Added HikariCP + SLF4J dependencies, shade plugin config for relocation |
| `src/.../storage/MySQLStorageProvider.java` | Full rewrite: HikariCP pool, removed global lock, MySQL transactions |
| `src/.../util/NumberUtil.java` | Uppercase K/M/B/T suffixes |
| `src/.../command/BaltopCommand.java` | Added footer divider after rank list |
| `src/.../command/eco/GiveSubcommand.java` | Uses `eco_give` message key, removed redundant balance fetch |
| `src/.../command/eco/TakeSubcommand.java` | Uses `eco_take` message key |
| `src/.../service/PaymentExecutor.java` | Cross-server notification support, timeout on sync event fire |
| `src/.../messaging/CrossServerMessenger.java` | Payment notification routing, pending notification DB fallback |
| `src/main/resources/languages/en.yml` | Full message colour/formatting redesign |
| `ezeconomy-velocity/.../EzEconomyVelocity.java` | Velocity proxy plugin for message forwarding and player list broadcast |

---

## Note

These changes currently only apply to the **MySQL storage provider**. The SQLite, MongoDB, and YML providers have not been updated with HikariCP or the lock removal. If you're using one of those, the original code is untouched. MySQL is the recommended provider for any multi-server setup.

---

## Setup (Multi-Server)

1. All backend servers must point to the **same MySQL database** in `plugins/EzEconomy/config-mysql.yml`.
2. Place `ezeconomy-bukkit-2.5.1.jar` in each backend server's `plugins/` folder.
3. Place `ezeconomy-velocity-2.5.1.jar` in the Velocity proxy's `plugins/` folder.
4. Velocity must have `player-info-forwarding-mode: modern` and each backend must have `velocity.enabled: true` in `paper-global.yml`.
5. Restart all servers and the proxy.

---

## Build from Source

```bash
mvn clean package -DskipTests
```

Output jars:
- `ezeconomy-bukkit/target/ezeconomy-bukkit-2.5.1.jar` → Backend servers (Paper/Spigot)
- `ezeconomy-velocity/target/ezeconomy-velocity-2.5.1.jar` → Velocity proxy

# GitEconomy

[![Licence](https://img.shields.io/badge/licence-MIT%20(Additional%20Terms)-blue.svg?style=flat-square)](LICENSE)

**GitEconomy** is a Vault-compatible Minecraft economy plugin by **GitEpildev** and **Epildevconnect Ltd**.

Focused on core economy commands. No banking subsystem.

---

## Commands

| Command | Description |
|---|---|
| `/balance` (`/bal`) | View your balance (or another player's) |
| `/pay <player> <amount>` | Send money to another player |
| `/baltop` (`/top`) | Show top balances |
| `/eco <give\|take\|set> <player> <amount>` | Admin balance controls |
| `/currency [currency]` | View or set preferred currency (optional multi-currency) |
| `/giteconomy <reload\|cleanup\|database>` | Admin utilities |

---

## Features

- Vault API compatible
- Storage: YML, MySQL, SQLite, MongoDB
- Optional multi-currency
- PlaceholderAPI support (`GitEconomy-PAPI`)
- Optional Redis / BungeeCord / Velocity networking modules

---

## Quick start

1. Install [Vault](https://www.spigotmc.org/resources/vault.34315/)
2. Drop `giteconomy-bukkit-*.jar` into `plugins/`
3. Restart and edit `plugins/GitEconomy/config.yml`

---

## Permissions

- `giteconomy.pay` - `/pay`
- `giteconomy.balance.others` - check others' balances
- `giteconomy.eco` - admin `/eco`
- `giteconomy.baltop` - `/baltop` / `/top`
- `giteconomy.admin` - `/giteconomy` utilities
- `giteconomy.payall` - `/pay *`

See [docs/permissions.md](docs/permissions.md) for the full list.

---

## Documentation

- [Overview](docs/overview.md)
- [Commands](docs/commands.md)
- [Configuration](docs/configuration.md)
- [Developer API](docs/developer-api.md)
- [Storage](docs/storage/storage.md)
- [PlaceholderAPI](docs/integration/placeholderapi.md)

---

## Build

```bash
mvn -pl giteconomy-bukkit -am clean package
```

---

## Licence

MIT Licence with Additional Terms. Copyright (c) 2026 Epildevconnect Ltd (GitEpildev).

See [LICENSE](LICENSE) for full terms (England and Wales).

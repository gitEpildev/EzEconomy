# GitEconomy

GitEconomy is a Vault-compatible economy provider by **GitEpildev** & **Epildevconnect Ltd**. It supports multiple storage backends and optional multi-currency while keeping operations safe under high concurrency. There is no banking subsystem.

## Highlights

- **Vault integration**: Works with any Vault-based plugin without extra setup.
- **Flexible storage**: YML, MySQL, SQLite, MongoDB, or a custom provider.
- **Multi-currency**: Optional per-player currency selection with conversion rates.
- **Async caching**: Keeps balance lookups fast on busy servers.
- **Core commands**: `/balance`, `/pay`, `/baltop`, `/eco`, `/currency`, `/giteconomy`.

## Supported Versions

GitEconomy targets modern Paper/Spigot servers that support Vault. For best results, use the latest versions of Paper, Vault, and GitEconomy.

## Quick Start

1. Install **Vault** and **GitEconomy**.
2. Place `giteconomy-bukkit-*.jar` in your plugins folder.
3. Configure `config.yml` and your selected storage config file.
4. Restart the server to generate data files.

## Typical Use Cases

- Replace legacy economies without changing other plugins.
- Provide multiple currencies for different game modes.
- Run a simple player-balance economy with Vault shops and PlaceholderAPI displays.

## Where to Go Next

- **Configuration**: See storage-specific settings and multi-currency setup.
- **Commands & Permissions**: Confirm staff and player access rules.
- **Storage details**: Understand backend behavior and data safety.
- **Events**: GitEconomy exposes transaction events for integrations and moderation.
  See:
  - `docs/api/event/PreTransactionEvent.md`
  - `docs/api/event/PostTransactionEvent.md`
  - `docs/api/event/PlayerPayPlayerEvent.md`
  - `docs/api/event/TransactionType.md`

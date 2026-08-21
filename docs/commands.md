# Commands

Permissions shown in parentheses are required to run the command. Commands without explicit permissions are available to all players by default.

| Command | Description | Permission |
| --- | --- | --- |
| `/balance` (`/bal`) | View your balance. | - |
| `/balance <currency>` | View your balance in the specified currency (player only). | - |
| `/balance <player>` | View another player's balance. | `giteconomy.balance.others` |
| `/balance <player> <currency>` | View another player's balance in the specified currency. | `giteconomy.balance.others` |
| `/baltop` (`/top`) `[amount]` | View the top balances. | `giteconomy.baltop` |
| `/pay <player\|*> <amount>` | Send money to another player or all players (`*`). | `giteconomy.pay` (all: `giteconomy.payall` if enabled) |
| `/currency [currency]` | View or set your preferred currency. | `giteconomy.currency` |
| `/currency convert <from> <to> <amount>` | Convert between currencies. | `giteconomy.currency` |
| `/eco give <player> <amount>` | Add funds to a player. | `giteconomy.eco` |
| `/eco take <player> <amount>` | Remove funds from a player. | `giteconomy.eco` |
| `/eco set <player> <amount>` | Set a player's balance. | `giteconomy.eco` |

## `/giteconomy` Admin Command

| Command | Description | Permission |
| --- | --- | --- |
| `/giteconomy cleanup` | Remove orphaned player data from all storage types. | `giteconomy.admin` |
| `/giteconomy reload` | Reload the plugin configuration. | `giteconomy.admin` |
| `/giteconomy reload messages` | Reload only the message file. | `giteconomy.admin` |
| `/giteconomy database info` | Show current database connection info. | `giteconomy.admin` |
| `/giteconomy database test` | Test the database connection. | `giteconomy.admin` |
| `/giteconomy database reset` | Reset all database tables (DANGEROUS). | `giteconomy.admin` |

### Tab Completion

- `/giteconomy` supports tab completion for all subcommands and database actions.
- Suggestions are context-aware and permission-sensitive.

### Tips

- Use a permissions plugin to control which groups can access administrative commands.
- For multi-currency servers, `/currency` controls each player's preferred display currency.

## Pay All (`/pay *`)

Using `/pay * <amount>` sends the specified amount to multiple recipients at once. This feature is configurable and permission-gated.

- **Default behavior:** Targets online players only.
- **Config keys:**
	- `pay.pay_all.enabled` (boolean, default: `true`) - enable/disable the pay-all feature.
	- `pay.pay_all.require_permission` (boolean, default: `true`) - require `giteconomy.payall` to use `/pay *`.
	- `pay.pay_all.include_offline` (boolean, default: `false`) - when `true`, include stored offline players (server storage) in the recipient list; when `false` only currently online players are paid.
- **Permissions:**
	- `giteconomy.pay` - standard pay permission for `/pay <player>`.
	- `giteconomy.payall` - (optional) grant access to `/pay *` when `pay.pay_all.require_permission` is `true`.
	- `giteconomy.payall.bypasswithdraw` - optional permission that lets the command credit recipients without withdrawing the total from the sender (useful for admin/gift operations).
- **Behavior notes:**
	- Unless `bypasswithdraw` is granted, the sender is charged the total amount (amount × recipients) before recipients are credited; failure to withdraw aborts the operation.
	- Recipients are credited in their preferred currency (conversion applied where needed).
	- By default the command enumerates online players via the server; enabling `pay.pay_all.include_offline` uses the storage provider to enumerate stored balances and may include offline-only accounts.
	- A summary message (`paid_all_summary`) is sent to the sender after successful execution. Recipients receive the standard payment notification if they are online.
	- Large recipient sets or mixed-currency conversions may increase execution time; consider enabling the feature only for trusted admins and ensure backup/monitoring is in place.
	- On Velocity networks with `cross-server.enabled: true`, `/pay *` includes players from all backend servers. Each remote recipient receives a notification forwarded through the proxy.

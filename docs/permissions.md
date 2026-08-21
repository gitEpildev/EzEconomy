# Permissions

Assign permissions through your permissions plugin (LuckPerms, PermissionsEx, etc.).

## Player Permissions

| Permission | Description |
| --- | --- |
| `giteconomy.balance.others` | View other players' balances. |
| `giteconomy.pay` | Send payments to other players. |
| `giteconomy.payall` | Use `/pay *` when `pay.pay_all.require_permission` is `true`. |
| `giteconomy.payall.bypasswithdraw` | Credit pay-all recipients without withdrawing from the sender. |
| `giteconomy.currency` | Set or view preferred currency. |
| `giteconomy.baltop` | View the balance top list (`/baltop` / `/top`). |

## Administrative Permissions

| Permission | Description |
| --- | --- |
| `giteconomy.eco` | Use `/eco` to give, take, or set balances. |
| `giteconomy.admin` | Use `/giteconomy` admin utilities (cleanup, reload, database). |

## Recommended Roles

- **Players**: `giteconomy.pay`, `giteconomy.currency`, `giteconomy.baltop`
- **Moderators/Staff**: `giteconomy.balance.others`
- **Administrators**: `giteconomy.eco`, `giteconomy.admin`, `giteconomy.payall`

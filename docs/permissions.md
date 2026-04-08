# Permissions

Assign permissions through your permissions plugin (LuckPerms, PermissionsEx, etc.).

## Notes

- All bank permissions are disabled if `banking.enabled` is set to `false`.
- Grant `ezeconomy.bank.admin` only to trusted staff.

## Player Permissions

| Permission | Description |
| --- | --- |
| `ezeconomy.balance.others` | View other players' balances. |
| `ezeconomy.pay` | Send payments to other players. |
| `ezeconomy.currency` | Set or view preferred currency. |

## Administrative Permissions

| Permission | Description |
| --- | --- |
| `ezeconomy.eco` | Use `/eco` to give, take, or set balances. |
| `ezeconomy.bank.admin` | Grants all bank-related permissions. |

## Bank Permissions

| Permission | Description |
| --- | --- |
| `ezeconomy.bank.create` | Create a bank. |
| `ezeconomy.bank.delete` | Delete a bank. |
| `ezeconomy.bank.balance` | View bank balance. |
| `ezeconomy.bank.deposit` | Deposit to a bank. |
| `ezeconomy.bank.withdraw` | Withdraw from a bank. |
| `ezeconomy.bank.addmember` | Add a bank member. |
| `ezeconomy.bank.removemember` | Remove a bank member. |
| `ezeconomy.bank.info` | View bank info. |

## Recommended Roles

- **Players**: `ezeconomy.pay`, `ezeconomy.currency`
- **Moderators/Staff**: `ezeconomy.balance.others`
- **Administrators**: `ezeconomy.eco`, `ezeconomy.bank.admin`

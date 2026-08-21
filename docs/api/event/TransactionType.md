# TransactionType

Enum: `com.gitepildev.giteconomy.api.events.TransactionType`

Values:
- `DEPOSIT` - money added to a single account.
- `WITHDRAW` - money removed from a single account.
- `TRANSFER` - generic transfer between two accounts (internal use by storage `transfer`).
- `PAY` - player-to-player payment (used by `PlayerPayPlayerEvent`).

Purpose: classify the kind of economy operation when firing pre/post transaction events so listeners can filter by `TransactionType`.

# Database Documentation

This document describes the database structures and setup procedures for the different storage providers supported by GitEconomy.

## Overview

GitEconomy supports multiple storage backends to store player balances and transactions:

- **YML**: File-based storage using YAML files (no database required)
- **SQLite**: Local SQLite database file
- **MySQL**: Remote MySQL database
- **MongoDB**: MongoDB NoSQL database

## Configuration

Storage providers are configured in the main `config.yml` file:

```yaml
storage: sqlite  # Options: yml, sqlite, mysql, mongodb
```

Each provider has its own configuration file (for example, `config-sqlite.yml`) that is loaded based on this value.

## YML Storage Provider

### Description
Stores data in YAML files on the filesystem. Each player has their own file.

### Setup
1. Set `storage: yml` in `config.yml`.
2. Configure in `config-yml.yml`:
   ```yaml
   yml:
     data-folder: "data"  # Folder relative to plugin data folder
     per-player-file-naming: "uuid"  # "uuid" or "username"
   ```
3. No additional setup required - files are created automatically.

### Structure
- **Player files**: `data/<uuid>.yml` or `data/<username>.yml`
  ```yaml
  uuid: "player-uuid-here"
  balances:
    dollar: 100.0
    euro: 50.0
  transactions:
    - amount: 10.0
      currency: "dollar"
      timestamp: 1640995200000
  ```

## SQLite Storage Provider

### Description
Uses a local SQLite database file for all data storage.

### Setup
1. Set `storage: sqlite` in `config.yml`.
2. Configure in `config-sqlite.yml`:
   ```yaml
   sqlite:
     file: "giteconomy.db"  # Database file name
     table: "balances"     # Player balances table
   ```
3. No additional setup required - database and tables are created automatically.

### Table Structures

#### balances
```sql
CREATE TABLE balances (
    uuid TEXT,
    currency TEXT,
    balance DOUBLE,
    PRIMARY KEY (uuid, currency)
);
```

#### transactions (optional)
```sql
CREATE TABLE transactions (
    uuid TEXT,
    currency TEXT,
    amount DOUBLE,
    timestamp INTEGER
);
```

## MySQL Storage Provider

### Description
Uses a remote MySQL database for scalable storage.

### Setup
1. Set `storage: mysql` in `config.yml`.
2. Configure in `config-mysql.yml`:
   ```yaml
   mysql:
     host: "localhost"
     port: 3306
     database: "giteconomy"
     username: "your_username"
     password: "your_password"
     table: "balances"
   ```
3. Create the MySQL database:
   ```sql
   CREATE DATABASE giteconomy;
   GRANT ALL PRIVILEGES ON giteconomy.* TO 'your_username'@'localhost' IDENTIFIED BY 'your_password';
   ```
4. Tables are created automatically on first run.

### Table Structures

#### balances
```sql
CREATE TABLE balances (
    uuid VARCHAR(36),
    currency VARCHAR(32),
    balance DOUBLE,
    PRIMARY KEY (uuid, currency)
);
```

#### transactions (optional)
```sql
CREATE TABLE transactions (
    uuid VARCHAR(36),
    currency VARCHAR(32),
    amount DOUBLE,
    timestamp BIGINT
);
```

## MongoDB Storage Provider

### Description
Uses MongoDB for NoSQL document-based storage.

### Setup
1. Set `storage: mongodb` in `config.yml`.
2. Configure in `config-mongodb.yml`:
   ```yaml
   mongodb:
     uri: "mongodb://localhost:27017"
     database: "giteconomy"
     collection: "balances"
   ```
3. Ensure MongoDB is running and accessible.
4. Collections and indexes are created automatically.

### Collection Structures

#### balances
```javascript
{
  "_id": ObjectId("..."),
  "uuid": "player-uuid-string",
  "currency": "dollar",
  "balance": 100.0
}

// Indexes:
// { uuid: 1, currency: 1 } (compound index for fast lookups)
```

#### transactions (optional)
```javascript
{
  "_id": ObjectId("..."),
  "uuid": "player-uuid",
  "currency": "dollar",
  "amount": 10.0,
  "timestamp": 1640995200000
}
```

## Migration Between Providers

Currently, there is no automatic migration tool. To switch providers:

1. Stop the server
2. Export data from the current provider (if needed)
3. Change `storage` in `config.yml`
4. Update the provider-specific config
5. Start the server (new tables/collections will be created)
6. Manually migrate data if necessary

## Performance Considerations

- **YML**: Good for small servers, simple file operations
- **SQLite**: Good for medium servers, single file database
- **MySQL**: Best for large servers, supports clustering and replication
- **MongoDB**: Good for large servers, flexible schema, horizontal scaling

## Troubleshooting

### Common Issues

1. **Permission denied**: Ensure the plugin has write access to the data folder
2. **Connection failed**: Check database credentials and network connectivity
3. **Table creation failed**: Ensure the database user has CREATE privileges.

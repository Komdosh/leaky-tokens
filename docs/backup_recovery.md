# Backup & Recovery Procedures

This document covers **local/dev** backup and recovery for the core data stores:

- PostgreSQL (auth + token databases)
- Cassandra (analytics)
- Redis (rate limit cache; optional)

These steps assume the Docker compose setup in `docker-compose.full.yml`.

## 1) PostgreSQL

### Backup
Run the helper script (creates timestamped dumps):

```bash
scripts/backup/postgres_backup.sh
```

This writes `auth_db.dump` and `token_db.dump` under `backups/<timestamp>/`.

### Restore
Restores from a specific backup directory and **drops/recreates** the databases:

```bash
scripts/restore/postgres_restore.sh backups/20260204-120000
```

Notes:
- This is destructive for `auth_db` and `token_db`.
- You can override `POSTGRES_CONTAINER`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` via env vars.

## 2) Cassandra (Analytics)

### Backup
Creates a snapshot and exports it to a `.tar.gz` archive:

```bash
scripts/backup/cassandra_backup.sh
```

This writes `cassandra-<snapshot-tag>.tar.gz` under `backups/<timestamp>/`.

### Restore (manual)
Because Cassandra restore is sensitive to paths, follow the steps below:

1. Stop analytics service (and Cassandra if needed).
2. Extract the snapshot archive to a temp folder.
3. Replace data under `/var/lib/cassandra/data/analytics_keyspace` **per table**.
4. Start Cassandra and run `nodetool refresh analytics_keyspace`.

This process is intentionally manual to avoid accidental data loss.

## 3) Redis (Optional)

Redis stores cache-like data, so restore is often unnecessary. If you want a backup:

### Backup
```bash
scripts/backup/redis_backup.sh
```

This copies `dump.rdb` and `appendonly.aof` to `backups/<timestamp>/redis/`.

### Restore
1. Stop Redis container.
2. Replace the Redis volume data with the backed-up files.
3. Start Redis container.

## Environment Variables

All scripts support overrides via environment variables:

- `BACKUP_DIR` (default: `backups/<timestamp>`)
- `POSTGRES_CONTAINER` (default: `postgres-db`)
- `POSTGRES_USER` (default: `postgres`)
- `POSTGRES_PASSWORD` (default: `password`)
- `CASSANDRA_CONTAINER` (default: `cassandra-db`)
- `CASSANDRA_KEYSPACE` (default: `analytics_keyspace`)
- `REDIS_CONTAINER` (default: `redis-cache`)


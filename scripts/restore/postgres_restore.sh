#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <backup_dir>"
  exit 1
fi

BACKUP_DIR="$1"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-postgres-db}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-password}"

if [ ! -d "${BACKUP_DIR}" ]; then
  echo "Backup dir not found: ${BACKUP_DIR}"
  exit 1
fi

for db in auth_db token_db; do
  dump="${BACKUP_DIR}/${db}.dump"
  if [ ! -f "${dump}" ]; then
    echo "Missing dump: ${dump}"
    exit 1
  fi

  echo "Restoring ${db} (drop + recreate)..."
  docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER}" \
    pg_restore -U "${POSTGRES_USER}" -d postgres --clean --if-exists -C "${dump}"
done

echo "Postgres restore completed."

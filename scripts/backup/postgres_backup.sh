#!/usr/bin/env bash
set -euo pipefail

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-postgres-db}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-password}"
BACKUP_DIR="${BACKUP_DIR:-backups/$(date +%Y%m%d-%H%M%S)}"

mkdir -p "${BACKUP_DIR}"

for db in auth_db token_db; do
  echo "Backing up ${db}..."
  docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" "${POSTGRES_CONTAINER}" \
    pg_dump -U "${POSTGRES_USER}" -Fc "${db}" > "${BACKUP_DIR}/${db}.dump"
done

echo "Postgres backups saved to ${BACKUP_DIR}"

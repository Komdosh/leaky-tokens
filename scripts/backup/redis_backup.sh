#!/usr/bin/env bash
set -euo pipefail

REDIS_CONTAINER="${REDIS_CONTAINER:-redis-cache}"
BACKUP_DIR="${BACKUP_DIR:-backups/$(date +%Y%m%d-%H%M%S)}/redis"

mkdir -p "${BACKUP_DIR}"

echo "Triggering Redis SAVE..."
docker exec "${REDIS_CONTAINER}" redis-cli SAVE

docker cp "${REDIS_CONTAINER}:/data/dump.rdb" "${BACKUP_DIR}/dump.rdb"
if docker exec "${REDIS_CONTAINER}" test -f /data/appendonly.aof; then
  docker cp "${REDIS_CONTAINER}:/data/appendonly.aof" "${BACKUP_DIR}/appendonly.aof"
fi

echo "Redis backup saved to ${BACKUP_DIR}"

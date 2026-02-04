#!/usr/bin/env bash
set -euo pipefail

CASSANDRA_CONTAINER="${CASSANDRA_CONTAINER:-cassandra-db}"
CASSANDRA_KEYSPACE="${CASSANDRA_KEYSPACE:-analytics_keyspace}"
BACKUP_DIR="${BACKUP_DIR:-backups/$(date +%Y%m%d-%H%M%S)}"
SNAPSHOT_TAG="backup-$(date +%Y%m%d-%H%M%S)"

mkdir -p "${BACKUP_DIR}"

echo "Creating Cassandra snapshot ${SNAPSHOT_TAG}..."
docker exec "${CASSANDRA_CONTAINER}" nodetool snapshot -t "${SNAPSHOT_TAG}" "${CASSANDRA_KEYSPACE}"

echo "Packaging snapshot..."
docker exec "${CASSANDRA_CONTAINER}" bash -lc \
  "find /var/lib/cassandra/data/${CASSANDRA_KEYSPACE} -type d -path \"*/snapshots/${SNAPSHOT_TAG}\" -print0 | xargs -0 tar -czf /tmp/cassandra-${SNAPSHOT_TAG}.tar.gz"

docker cp "${CASSANDRA_CONTAINER}:/tmp/cassandra-${SNAPSHOT_TAG}.tar.gz" \
  "${BACKUP_DIR}/cassandra-${SNAPSHOT_TAG}.tar.gz"

echo "Cassandra backup saved to ${BACKUP_DIR}/cassandra-${SNAPSHOT_TAG}.tar.gz"

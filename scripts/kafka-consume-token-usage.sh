#!/usr/bin/env bash
set -euo pipefail

# Requires Kafka running via docker-compose.full.yml
# Usage: ./scripts/kafka-consume-token-usage.sh

KAFKA_CONTAINER=${KAFKA_CONTAINER:-kafka-broker}
TOPIC=${TOPIC:-token-usage}

printf "Consuming from Kafka topic '%s' in container '%s'...\n" "$TOPIC" "$KAFKA_CONTAINER"

exec docker exec -it "$KAFKA_CONTAINER" \
  kafka-console-consumer --bootstrap-server kafka:9092 \
  --topic "$TOPIC" --from-beginning

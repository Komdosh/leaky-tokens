#!/usr/bin/env bash
set -euo pipefail

# Requires full stack running with Kafka + Cassandra and services.
# Usage: ./scripts/analytics-smoke.sh [provider] [tokens]

PROVIDER=${1:-openai}
TOKENS=${2:-5}

printf "Posting token usage (provider=%s, tokens=%s)...\n" "$PROVIDER" "$TOKENS"

curl -s -X POST http://localhost:8080/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"u-smoke\",\"provider\":\"$PROVIDER\",\"tokens\":$TOKENS}" | jq

printf "\nWaiting for analytics consumer...\n"
sleep 2

printf "\nFetching analytics usage...\n"
curl -s "http://localhost:8080/api/v1/analytics/usage?provider=$PROVIDER&limit=5" | jq

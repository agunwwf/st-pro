#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-http}"

if [[ "$MODE" == "https" ]]; then
  docker compose -f docker-compose.yml -f docker-compose.https.yml up -d
  docker compose -f docker-compose.yml -f docker-compose.https.yml ps
else
  docker compose up -d
  docker compose ps
fi

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f ".env" ]]; then
  echo "[ERROR] .env 不存在，请先 cp .env.example .env 并填写变量"
  exit 1
fi

docker compose up -d --build
docker compose ps

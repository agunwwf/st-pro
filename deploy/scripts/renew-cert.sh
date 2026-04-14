#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f ".env" ]]; then
  echo "[ERROR] .env 不存在，请先创建"
  exit 1
fi

set -a
source .env
set +a

if [[ -z "${DOMAIN:-}" ]]; then
  echo "[ERROR] DOMAIN 未设置，请在 .env 中配置"
  exit 1
fi

mkdir -p deploy/certbot/www deploy/certbot/conf

docker run --rm \
  -v "$ROOT_DIR/deploy/certbot/www:/var/www/certbot" \
  -v "$ROOT_DIR/deploy/certbot/conf:/etc/letsencrypt" \
  certbot/certbot renew --webroot -w /var/www/certbot

docker compose -f docker-compose.yml -f docker-compose.https.yml restart nginx

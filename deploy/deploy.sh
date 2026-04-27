#!/bin/bash
# deploy.sh — executa dentro de sos-videos-api/
# Uso: bash deploy/deploy.sh
set -e

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "==> Atualizando backend (sos-videos-api)..."
cd "$ROOT/sos-videos-api"
git pull origin main

echo "==> Atualizando frontend (sos-videos-front)..."
cd "$ROOT/sos-videos-front"
git pull origin master

echo "==> Rebuilding containers..."
cd "$ROOT"
docker compose build --no-cache backend frontend

echo "==> Subindo..."
docker compose up -d

echo "==> Status:"
docker compose ps

echo ""
echo "Deploy concluído. Logs: docker compose logs -f backend"

#!/bin/bash
# deploy.sh — executa dentro de sos-videos-api/
# Uso: bash deploy/deploy.sh
set -e

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
echo "==> Root: $ROOT"

echo ""
echo "==> Atualizando backend (sos-videos-api)..."
cd "$ROOT/sos-videos-api"
git fetch origin
git reset --hard origin/main
echo "    Commit: $(git log --oneline -1)"

echo ""
echo "==> Atualizando frontend (sos-videos-front)..."
cd "$ROOT/sos-videos-front"
git fetch origin
git reset --hard origin/master
echo "    Commit: $(git log --oneline -1)"

echo ""
echo "==> Rebuilding containers..."
cd "$ROOT"
docker compose build --no-cache backend frontend

echo ""
echo "==> Subindo..."
docker compose up -d

echo ""
echo "==> Status:"
docker compose ps

echo ""
echo "Deploy concluído. Logs: docker compose logs -f backend"

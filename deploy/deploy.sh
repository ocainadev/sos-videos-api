#!/bin/bash
# deploy.sh — executa na VPS dentro da pasta sosvideos/
# Uso: bash deploy/deploy.sh
set -e

echo "==> Atualizando backend..."
git pull origin main

echo "==> Atualizando frontend..."
cd ../sosvideos-frontend && git pull origin main && cd ../sosvideos

echo "==> Rebuilding containers..."
docker compose build --no-cache backend frontend

echo "==> Subindo..."
docker compose up -d

echo "==> Status:"
docker compose ps

echo ""
echo "Deploy concluído. Logs: docker compose logs -f backend"

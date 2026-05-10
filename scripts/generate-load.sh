#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ROUNDS="${ROUNDS:-300}"

echo "==> Fetching existing user IDs..."
USERS=$(curl -sf "$BASE_URL/api/users" | python3 -c "import sys,json; users=json.load(sys.stdin); [print(u['id']) for u in users[:2]]")
U1=$(echo "$USERS" | sed -n '1p')
U2=$(echo "$USERS" | sed -n '2p')

if [[ -z "$U1" || -z "$U2" ]]; then
  echo "ERROR: Need at least 2 users in the DB. Run ./scripts/generate-traffic.sh first."
  exit 1
fi
echo "    U1=$U1  U2=$U2"

echo ""
echo "==> Round 1: $ROUNDS concurrent actuator + user-list requests (CPU/network spike)..."
for i in $(seq 1 $ROUNDS); do
  curl -s "$BASE_URL/actuator/prometheus" > /dev/null &
  curl -s "$BASE_URL/api/users" > /dev/null &
  if (( i % 10 == 0 )); then
    wait
    echo "    batch $i/$ROUNDS done"
  fi
done
wait
echo "    Round 1 complete"

echo ""
echo "==> Round 2: 50 concurrent document creates + health checks (memory/mongo I/O spike)..."
for i in $(seq 1 50); do
  curl -s -X POST "$BASE_URL/api/documents" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: $U1" \
    -d "{\"title\":\"Load Doc $i\",\"content\":\"$(head -c 512 /dev/urandom | base64)\",\"workspaceId\":\"ws-load\"}" > /dev/null &
  curl -s "$BASE_URL/api/users" > /dev/null &
  curl -s "$BASE_URL/actuator/health" > /dev/null &
  if (( i % 10 == 0 )); then
    wait
    echo "    batch $i/50 done"
  fi
done
wait
echo "    Round 2 complete"

echo ""
echo "==> Load generation done. Check Grafana at http://localhost:3000/d/pMEd7m0Mz/cadvisor-exporter"

#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "==> Creating users..."
U1=$(curl -sf -X POST "$BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Traffic User 1","email":"traffic1@test.com"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['id'])" 2>/dev/null || echo "")

U2=$(curl -sf -X POST "$BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Traffic User 2","email":"traffic2@test.com"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['id'])" 2>/dev/null || echo "")

if [[ -z "$U1" || -z "$U2" ]]; then
  echo "    Users already exist, fetching existing IDs..."
  USERS=$(curl -sf "$BASE_URL/api/users" | python3 -c "import sys,json; users=json.load(sys.stdin); [print(u['id']) for u in users[:2]]")
  U1=$(echo "$USERS" | sed -n '1p')
  U2=$(echo "$USERS" | sed -n '2p')
fi
echo "    U1=$U1"
echo "    U2=$U2"

echo ""
echo "==> Triggering failed user creations (duplicate emails -> app_users_creation_failed_total)..."
curl -s -X POST "$BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Dup","email":"traffic1@test.com"}' > /dev/null
curl -s -X POST "$BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Dup","email":"traffic2@test.com"}' > /dev/null
echo "    2 duplicate-email failures sent"

echo ""
echo "==> Creating documents (app_documents_created_total)..."
G1=$(curl -sf -X POST "$BASE_URL/api/documents" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $U1" \
  -d '{"title":"Traffic Doc 1","content":"Hello from traffic script","workspaceId":"ws-traffic"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['documentGroupId'])")
echo "    doc1 groupId=$G1"

G2=$(curl -sf -X POST "$BASE_URL/api/documents" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $U2" \
  -d '{"title":"Traffic Doc 2","content":"More traffic content","workspaceId":"ws-traffic"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['documentGroupId'])")
echo "    doc2 groupId=$G2"

echo ""
echo "==> Looking up documents 5x each (app_document_lookup_duration_seconds)..."
for i in 1 2 3 4 5; do
  curl -s "$BASE_URL/api/documents/$G1" > /dev/null
  curl -s "$BASE_URL/api/documents/$G2" > /dev/null
done
echo "    10 lookups complete"

echo ""
echo "==> Triggering access denied (app_document_access_denied_total)..."
curl -s -X DELETE "$BASE_URL/api/documents/$G1" \
  -H "X-User-Id: $U2" > /dev/null
echo "    U2 attempted to delete U1's doc (forbidden)"

echo ""
echo "==> Current metrics snapshot:"
curl -s "$BASE_URL/actuator/prometheus" | grep -E "^app_"

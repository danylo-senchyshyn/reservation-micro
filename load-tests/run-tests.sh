#!/usr/bin/env bash
# run-tests.sh
#
# Convenience script to run all performance tests and collect resource metrics.
#
# Prerequisites:
#   - Docker & Docker Compose running with `docker-compose up -d`
#   - k6 installed: https://k6.io/docs/get-started/installation/
#     macOS: brew install k6
#
# Usage:
#   ./load-tests/run-tests.sh [microservices|monolith]
#
# Output (in load-tests/results/):
#   latency-throughput.json  – raw k6 output for latency & throughput test
#   e2e-time.json            – raw k6 output for end-to-end time test
#   docker-stats-*.csv       – container resource usage during tests

set -euo pipefail

ARCH="${1:-microservices}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
PAYMENT_URL="${PAYMENT_URL:-http://localhost:8083}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results/${ARCH}"
K6_SCRIPTS="${SCRIPT_DIR}/k6"

mkdir -p "$RESULTS_DIR"

echo "============================================================"
echo "  Architecture: ${ARCH}"
echo "  BASE_URL:     ${BASE_URL}"
echo "  Results dir:  ${RESULTS_DIR}"
echo "============================================================"

# ── Wait for gateway to be ready ─────────────────────────────────────────────
echo ""
echo "[1/4] Waiting for services to be healthy..."
for i in $(seq 1 30); do
  if curl -sf "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    echo "      Services are up."
    break
  fi
  echo "      Attempt ${i}/30 – retrying in 5s..."
  sleep 5
done

# ── Start resource collection in background ───────────────────────────────────
STATS_FILE="${RESULTS_DIR}/docker-stats-$(date +%Y%m%d_%H%M%S).csv"
echo ""
echo "[2/4] Starting Docker stats collection → ${STATS_FILE}"
bash "${SCRIPT_DIR}/collect-docker-stats.sh" 2 "$STATS_FILE" &
STATS_PID=$!
trap "kill $STATS_PID 2>/dev/null; echo 'Stats collection stopped.'" EXIT INT TERM

sleep 2  # let stats collector write the header

# ── Test 1: Latency & Throughput ─────────────────────────────────────────────
echo ""
echo "[3/4] Running LATENCY & THROUGHPUT test..."
k6 run \
  --env BASE_URL="${BASE_URL}" \
  --out "json=${RESULTS_DIR}/latency-throughput.json" \
  --summary-export "${RESULTS_DIR}/latency-throughput-summary.json" \
  "${K6_SCRIPTS}/latency-throughput.js"

echo "      Done. Summary → ${RESULTS_DIR}/latency-throughput-summary.json"

# ── Test 2: End-to-End Time ───────────────────────────────────────────────────
echo ""
echo "[4/4] Running END-TO-END TIME test..."
k6 run \
  --env BASE_URL="${BASE_URL}" \
  --env PAYMENT_URL="${PAYMENT_URL}" \
  --out "json=${RESULTS_DIR}/e2e-time.json" \
  --summary-export "${RESULTS_DIR}/e2e-time-summary.json" \
  "${K6_SCRIPTS}/e2e-time.js"

echo "      Done. Summary → ${RESULTS_DIR}/e2e-time-summary.json"

# ── Done ──────────────────────────────────────────────────────────────────────
kill "$STATS_PID" 2>/dev/null || true
echo ""
echo "============================================================"
echo "  All tests complete. Results in: ${RESULTS_DIR}/"
echo "  Files:"
ls -lh "$RESULTS_DIR"
echo "============================================================"

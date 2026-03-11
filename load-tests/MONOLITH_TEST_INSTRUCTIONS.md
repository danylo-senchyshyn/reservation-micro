# Load Test Instructions for the Monolith

This document describes exactly how performance tests were conducted on the microservices project.
Run **identical** tests on the monolith so the results are directly comparable.

---

## Context

These tests are part of a bachelor's thesis comparing a microservices architecture with a monolith.
The microservices results are in `load-tests/results/microservices/PERFORMANCE_RESULTS.md` (in the microservices repo).
The monolith results must go into an equivalent file: `load-tests/results/monolith/PERFORMANCE_RESULTS.md`.

---

## Prerequisites

Install k6 if not already installed:
```bash
brew install k6        # macOS
# or: https://grafana.com/docs/k6/latest/set-up/install-k6/
```

Make sure the monolith is running (Docker or locally). The tests assume:
- Everything is accessible via a single base URL (default: `http://localhost:8080`)
- The following REST API endpoints exist (same as in the microservices):

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/users` | Create user. Body: `{"email": "...", "fullName": "..."}`. Returns `{"id": N, ...}` |
| `POST` | `/api/reservations` | Create reservation. Body: see below. Returns `{"id": N, "status": "...", ...}` |
| `GET`  | `/api/reservations/{id}` | Get reservation by ID. Returns `{"id": N, "status": "...", ...}` |
| `GET`  | `/api/payments/by-reservation/{reservationId}` | Get payments for a reservation. Returns array `[{"id": N, ...}]` |
| `POST` | `/api/payments/{id}/confirm` | Confirm a payment. Returns 200. |
| `DELETE` | `/api/reservations` | Delete ALL reservations (for teardown) |
| `DELETE` | `/api/payments` | Delete ALL payments (for teardown) |
| `DELETE` | `/api/users` | Delete ALL users (for teardown) |

**Create reservation body:**
```json
{
  "userId": 1,
  "resourceId": 42,
  "from": "2026-09-01T10:00",
  "to": "2026-09-01T12:00"
}
```

**Reservation terminal statuses to poll for in E2E test:**
- In the microservices: `PAID` (payment confirmed) and `PAYMENT_FAILED` (payment failed)
- Adapt these values to whatever enum values the monolith uses for the same states.
  Common alternatives: `CONFIRMED`, `PAID`, `COMPLETED`. Check the monolith's `ReservationStatus` enum.

---

## Test 1: Latency & Throughput

### What it measures
HTTP response time of `POST /api/reservations` under increasing concurrent load.
This measures the **synchronous** write path: HTTP request → business logic → DB write → response.

### k6 script

Create the file `load-tests/k6/latency-throughput.js` with the following content:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

const createReservationDuration = new Trend('create_reservation_ms', true);
const successRate = new Rate('success_rate');
const totalCreated = new Counter('reservations_created');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    baseline: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      startTime: '0s',
      tags: { scenario: 'baseline' },
    },
    low_load: {
      executor: 'constant-vus',
      vus: 10,
      duration: '60s',
      startTime: '40s',
      tags: { scenario: 'low_load' },
    },
    medium_load: {
      executor: 'constant-vus',
      vus: 30,
      duration: '60s',
      startTime: '115s',
      tags: { scenario: 'medium_load' },
    },
    high_load: {
      executor: 'constant-vus',
      vus: 60,
      duration: '60s',
      startTime: '190s',
      tags: { scenario: 'high_load' },
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  const res = http.post(
    `${BASE_URL}/api/users`,
    JSON.stringify({ email: 'loadtest-lt@example.com', fullName: 'LT User' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  const userId = res.json('id');
  console.log(`Setup complete: userId=${userId}`);
  return { userId };
}

export default function (data) {
  const payload = JSON.stringify({
    userId: data.userId,
    resourceId: Math.floor(Math.random() * 1000) + 1,
    from: '2026-09-01T10:00',
    to: '2026-09-01T12:00',
  });

  const res = http.post(`${BASE_URL}/api/reservations`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'create_reservation' },
  });

  const ok = check(res, {
    'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    'has id in body': (r) => r.json('id') !== undefined,
  });

  createReservationDuration.add(res.timings.duration);
  successRate.add(ok);
  if (ok) totalCreated.add(1);

  sleep(0.05);
}

export function teardown() {
  http.del(`${BASE_URL}/api/reservations`);
  http.del(`${BASE_URL}/api/payments`);
  http.del(`${BASE_URL}/api/users`);
  console.log('Teardown complete');
}
```

### How to run

```bash
mkdir -p load-tests/results/monolith

# Start docker stats collection (in background)
nohup bash load-tests/collect-docker-stats.sh \
  load-tests/results/monolith/docker-stats-lt.csv \
  2 > /tmp/stats-lt.log 2>&1 &
STATS_PID=$!
echo "Stats PID: $STATS_PID"

# Run the test (~4.5 minutes total)
k6 run --out json=load-tests/results/monolith/latency-throughput.json \
       load-tests/k6/latency-throughput.js

# Stop stats collection
kill $STATS_PID
```

---

## Test 2: End-to-End Processing Time

### What it measures
Wall-clock time for the full business flow to complete:
1. Client creates a reservation → system creates a payment record (automatically)
2. Client confirms the payment → system updates reservation to terminal status

In the **microservices**, this flow is async (Outbox → Kafka → consumer, ~5–10 s per full flow).
In the **monolith**, this may be synchronous (< 1 s) or still async depending on design.
The test is **identical** either way — it just polls and times it.

### k6 script

Create `load-tests/k6/e2e-time.js`:

> **IMPORTANT:** Before running, check what the monolith's `ReservationStatus` enum values are
> for "payment confirmed" and "payment failed". Replace `'PAID'` and `'PAYMENT_FAILED'` below
> with the correct values (e.g., `'CONFIRMED'`, `'COMPLETED'`, etc.).

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const e2eTotal     = new Trend('e2e_total_ms', true);
const e2eToPayment = new Trend('e2e_to_payment_ms', true);
const e2eToConfirm = new Trend('e2e_to_confirm_ms', true);
const completed    = new Counter('e2e_completed');
const failed       = new Counter('e2e_failed');

const BASE_URL        = __ENV.BASE_URL || 'http://localhost:8080';
const MAX_WAIT_MS     = 30000;  // max 30 s per phase
const POLL_INTERVAL_S = 0.5;   // poll every 500 ms

export const options = {
  vus: 1,
  iterations: 20,
  thresholds: {
    e2e_total_ms: ['p(95)<25000'],
    e2e_failed:   ['count<3'],
  },
};

export function setup() {
  const res = http.post(
    `${BASE_URL}/api/users`,
    JSON.stringify({ email: 'loadtest-e2e@example.com', fullName: 'E2E User' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  const userId = res.json('id');
  console.log(`Setup complete: userId=${userId}`);
  return { userId };
}

function pollUntil(fn, maxMs) {
  const deadline = Date.now() + maxMs;
  while (Date.now() < deadline) {
    const result = fn();
    if (result !== null && result !== undefined && result !== false) return result;
    sleep(POLL_INTERVAL_S);
  }
  return null;
}

export default function (data) {
  // Step 1: Create reservation
  const e2eStart = Date.now();

  const reservationRes = http.post(
    `${BASE_URL}/api/reservations`,
    JSON.stringify({
      userId: data.userId,
      resourceId: Math.floor(Math.random() * 1000) + 1,
      from: '2026-09-01T10:00',
      to: '2026-09-01T12:00',
    }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  if (!check(reservationRes, { 'reservation created (2xx)': (r) => r.status >= 200 && r.status < 300 })) {
    console.error(`Failed to create reservation: status=${reservationRes.status}`);
    failed.add(1);
    return;
  }

  const reservationId = reservationRes.json('id');

  // Step 2: Wait for a payment to be created for this reservation
  const paymentPhaseStart = Date.now();

  const paymentId = pollUntil(() => {
    const r = http.get(`${BASE_URL}/api/payments/by-reservation/${reservationId}`);
    if (r.status === 200) {
      const list = r.json();
      if (list && list.length > 0) return list[0].id;
    }
    return null;
  }, MAX_WAIT_MS);

  if (!paymentId) {
    console.error(`Timeout waiting for payment for reservationId=${reservationId}`);
    failed.add(1);
    return;
  }

  e2eToPayment.add(Date.now() - paymentPhaseStart);

  // Step 3: Confirm payment
  const confirmRes = http.post(`${BASE_URL}/api/payments/${paymentId}/confirm`);

  if (!check(confirmRes, { 'payment confirmed (200)': (r) => r.status === 200 })) {
    console.error(`Failed to confirm paymentId=${paymentId}: status=${confirmRes.status}`);
    failed.add(1);
    return;
  }

  const confirmPhaseStart = Date.now();

  // Step 4: Wait for reservation to reach terminal status
  // ⚠️  ADAPT THESE STATUS VALUES TO MATCH THE MONOLITH'S ReservationStatus ENUM:
  //     'PAID'           → payment confirmed successfully
  //     'PAYMENT_FAILED' → payment failed
  const TERMINAL_STATUSES = ['PAID', 'PAYMENT_FAILED'];

  const finalStatus = pollUntil(() => {
    const r = http.get(`${BASE_URL}/api/reservations/${reservationId}`);
    if (r.status === 200) {
      const status = r.json('status');
      if (TERMINAL_STATUSES.includes(status)) return status;
    }
    return null;
  }, MAX_WAIT_MS);

  if (!finalStatus) {
    console.error(`Timeout waiting for reservation ${reservationId} to reach terminal status`);
    failed.add(1);
    return;
  }

  // Record metrics
  const totalMs = Date.now() - e2eStart;
  e2eTotal.add(totalMs);
  e2eToConfirm.add(Date.now() - confirmPhaseStart);
  completed.add(1);

  console.log(`[OK] reservationId=${reservationId} status=${finalStatus} totalMs=${totalMs}`);
}

export function teardown() {
  http.del(`${BASE_URL}/api/reservations`);
  http.del(`${BASE_URL}/api/payments`);
  http.del(`${BASE_URL}/api/users`);
  console.log('Teardown complete');
}
```

### How to run

```bash
# Ensure DB is clean before E2E test (no leftover data from latency test)
# Run truncate commands appropriate for the monolith's DB setup

# Start stats collection
nohup bash load-tests/collect-docker-stats.sh \
  load-tests/results/monolith/docker-stats-e2e.csv \
  2 > /tmp/stats-e2e.log 2>&1 &
STATS_PID=$!

# Run the test (~2–5 minutes)
k6 run --out json=load-tests/results/monolith/e2e-time.json \
       load-tests/k6/e2e-time.js

kill $STATS_PID
```

---

## Docker Stats Collection Script

Create `load-tests/collect-docker-stats.sh` (adapt container names for the monolith):

```bash
#!/usr/bin/env bash
set -euo pipefail

OUTPUT="${1:-load-tests/results/monolith/docker-stats.csv}"
INTERVAL="${2:-2}"

# ⚠️  Replace with the actual container names in the monolith's docker-compose
CONTAINERS=(
  monolith-app     # main application container — change this name
  postgres
  # add others if present (redis, kafka, etc.)
)

mkdir -p "$(dirname "$OUTPUT")"
echo "timestamp,container,cpu_pct,mem_usage_mb,mem_limit_mb,mem_pct,net_in_mb,net_out_mb,block_in_mb,block_out_mb" > "$OUTPUT"
echo "Collecting Docker stats every ${INTERVAL}s → $OUTPUT. Press Ctrl+C to stop."

trap 'echo "Stopped."' INT TERM

while true; do
  TS=$(date +%Y-%m-%dT%H:%M:%S)
  docker stats --no-stream --format \
    "{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}" \
    "${CONTAINERS[@]}" 2>/dev/null \
  | while IFS=$'\t' read -r name cpu mem_usage mem_pct net_io block_io; do
      cpu_val="${cpu//%/}"
      mem_num=$(echo "$mem_usage" | awk '{print $1}' | sed 's/[A-Za-z]//g')
      mem_unit=$(echo "$mem_usage" | awk '{print $1}' | sed 's/[0-9.]//g')
      mem_lim_num=$(echo "$mem_usage" | awk '{print $3}' | sed 's/[A-Za-z]//g')
      mem_lim_unit=$(echo "$mem_usage" | awk '{print $3}' | sed 's/[0-9.]//g')
      to_mb() { local val=$1 unit=$2; case "${unit^^}" in KIB|KB) echo "scale=2; $val/1024"|bc;; MIB|MB) echo "$val";; GIB|GB) echo "scale=2; $val*1024"|bc;; *) echo "$val";; esac; }
      mem_mb=$(to_mb "$mem_num" "$mem_unit")
      lim_mb=$(to_mb "$mem_lim_num" "$mem_lim_unit")
      net_in=$(echo "$net_io" | awk '{print $1}' | sed 's/[A-Za-z]//g')
      net_out=$(echo "$net_io" | awk '{print $3}' | sed 's/[A-Za-z]//g')
      blk_in=$(echo "$block_io" | awk '{print $1}' | sed 's/[A-Za-z]//g')
      blk_out=$(echo "$block_io" | awk '{print $3}' | sed 's/[A-Za-z]//g')
      echo "${TS},${name},${cpu_val},${mem_mb},${lim_mb},${mem_pct//%/},${net_in},${net_out},${blk_in},${blk_out}" >> "$OUTPUT"
    done
  sleep "$INTERVAL"
done
```

Make it executable:
```bash
chmod +x load-tests/collect-docker-stats.sh
```

---

## Step-by-Step Execution Order

```
1. Start the monolith (docker-compose up -d or equivalent)
2. Verify all endpoints respond: curl http://localhost:8080/api/users
3. Ensure DB is empty (truncate all tables)
4. Run Test 1 (latency-throughput) with stats collection
5. After Test 1 completes: truncate all tables again (including any outbox/event tables)
6. Run Test 2 (e2e-time) with stats collection
7. Analyze results and write PERFORMANCE_RESULTS.md
```

**Before running Test 2**, verify the full flow works manually:
```bash
# Create user
curl -s -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"email":"smoke@test.com","fullName":"Smoke Test"}' | jq .

# Create reservation (note the id)
curl -s -X POST http://localhost:8080/api/reservations \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"resourceId":1,"from":"2026-09-01T10:00","to":"2026-09-01T12:00"}' | jq .

# Wait a moment, then check if payment was created
curl -s http://localhost:8080/api/payments/by-reservation/1 | jq .

# Confirm payment (replace 1 with actual payment id)
curl -s -X POST http://localhost:8080/api/payments/1/confirm | jq .

# Check reservation status — should be PAID (or equivalent terminal status)
curl -s http://localhost:8080/api/reservations/1 | jq .status
```

---

## Results File to Write

After running both tests, parse the results and write
`load-tests/results/monolith/PERFORMANCE_RESULTS.md` using the **same structure** as the
microservices results file so the tables can be compared side by side.

Required sections:
1. **Architecture under test** — brief description of the monolith's request flow
2. **Latency & Throughput** — per-scenario table (n, throughput, avg, p50, p90, p95, p99, max) in ms
3. **End-to-End Processing Time** — avg, min, p50, p90, p95, max for `e2e_total_ms`, `e2e_to_payment_ms`, `e2e_to_confirm_ms`
4. **Resource Usage** — CPU% and memory (MiB) per container during peak load
5. **Summary table** — same metrics as in microservices summary for direct comparison

### Microservices reference values (for comparison):

| Metric | Microservices |
|--------|--------------|
| Peak throughput (60 VUs) | ~972 req/s |
| Median latency (60 VUs) | 7.1 ms |
| p95 latency (60 VUs) | 35.9 ms |
| E2E time — median | 5.21 s |
| E2E time — p95 | 10.33 s |
| E2E success rate | 100% (20/20) |
| HTTP error rate | 0% |
| Total memory footprint | ~3.5 GiB (7 containers) |
| Peak CPU (busiest service) | 170% |
| Async latency driver | Outbox scheduler jitter 0–5 s/hop |

---

## Notes on Objectivity

- **Same k6 version** — run `k6 version` and note it in the results
- **Same hardware** — run on the same machine, same Docker resource limits
- **Same load profile** — do not change VU counts, durations, or think-time (`sleep(0.05)`)
- **Same date range in payloads** — `from: "2026-09-01T10:00"` — do not change
- **Warm DB before measuring** — both tests have a 10 s gap between scenarios (built into startTime offsets)
- **Clean state before each test run** — truncate all tables to avoid measuring index/cache effects of leftover data
- **Do not interact with the machine** during tests (no mouse, no browser tabs, no background builds)

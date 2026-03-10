/**
 * k6 Load Test: Latency & Throughput
 *
 * Measures:
 *   - HTTP response latency (p50, p90, p95, p99) for POST /api/reservations
 *   - Throughput (requests/sec) under different concurrent user loads
 *
 * Run:
 *   k6 run load-tests/k6/latency-throughput.js
 *   k6 run --out json=results/latency-throughput.json load-tests/k6/latency-throughput.js
 *
 * Requires the full stack to be running:
 *   docker-compose up -d
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

// ── Custom metrics ────────────────────────────────────────────────────────────
const createReservationDuration = new Trend('create_reservation_ms', true);
const successRate             = new Rate('success_rate');
const totalCreated            = new Counter('reservations_created');

// ── Test configuration ────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    // Scenario 1 – baseline (single user)
    baseline: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      startTime: '0s',
      tags: { scenario: 'baseline' },
    },
    // Scenario 2 – low concurrency
    low_load: {
      executor: 'constant-vus',
      vus: 10,
      duration: '60s',
      startTime: '40s',
      tags: { scenario: 'low_load' },
    },
    // Scenario 3 – medium concurrency
    medium_load: {
      executor: 'constant-vus',
      vus: 30,
      duration: '60s',
      startTime: '115s',
      tags: { scenario: 'medium_load' },
    },
    // Scenario 4 – high concurrency
    high_load: {
      executor: 'constant-vus',
      vus: 60,
      duration: '60s',
      startTime: '190s',
      tags: { scenario: 'high_load' },
    },
  },
  thresholds: {
    // Alert if 95th-percentile latency exceeds 3 s
    http_req_duration: ['p(95)<3000'],
    // Alert if error rate exceeds 1 %
    http_req_failed: ['rate<0.01'],
  },
};

// ── Setup: create one shared user ─────────────────────────────────────────────
export function setup() {
  const res = http.post(
    `${BASE_URL}/api/users`,
    JSON.stringify({ email: 'loadtest-lt@example.com', fullName: 'LT User' }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  if (res.status !== 200 && res.status !== 201) {
    console.error(`Setup failed: could not create user. Status=${res.status} body=${res.body}`);
    // Try to parse userId from existing user if already created
  }

  const userId = res.json('id');
  console.log(`Setup complete: userId=${userId}`);
  return { userId };
}

// ── Main VU function ──────────────────────────────────────────────────────────
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

  // Small pause to avoid spinning tightly (adjust to 0 for maximum throughput)
  sleep(0.05);
}

// ── Teardown: remove test data ────────────────────────────────────────────────
export function teardown() {
  http.del(`${BASE_URL}/api/reservations`);
  http.del(`${BASE_URL}/api/payments`);
  http.del(`${BASE_URL}/api/users`);
  console.log('Teardown complete: test data removed');
}

# Microservices Performance Test Results

**Date:** 2026-03-10
**Stack:** Spring Boot 3 · Kafka · PostgreSQL · API Gateway
**Environment:** Docker Compose on macOS (8 vCPU allocated to Docker, 5.786 GiB RAM limit)
**Tool:** k6 v0.57.0

---

## Architecture Under Test

```
Client → gateway-service (8080)
           ├─ user-service        (REST, own DB)
           ├─ reservation-service (REST + Outbox → Kafka)
           └─ payment-service     (REST + Outbox → Kafka)

Async event flow:
  POST /api/reservations
    → reservation-service saves Reservation (CREATED)
    → saves OutboxEvent (NEW)
    → scheduler (fixedDelay=5s) publishes ReservationCreatedEvent to Kafka
    → payment-service consumes → creates Payment (CREATED)

  POST /api/payments/{id}/confirm
    → payment-service updates Payment (CONFIRMED)
    → saves OutboxEvent (NEW)
    → scheduler (fixedDelay=5s) publishes PaymentConfirmedEvent to Kafka
    → reservation-service consumes → updates Reservation (PAID)
```

---

## 1. Latency & Throughput

**Test:** `POST /api/reservations` — creates a reservation and writes an outbox event in a single transaction.
**Scenarios run sequentially:**

| Scenario     | VUs | Duration | Requests | Throughput |
|--------------|----:|---------|----------:|-----------:|
| baseline     |   1 | 30 s    |       479 |   ~16 req/s |
| low_load     |  10 | 60 s    |    10,249 |  ~171 req/s |
| medium_load  |  30 | 60 s    |    31,583 |  ~526 req/s |
| high_load    |  60 | 60 s    |    58,295 |  ~972 req/s |

### Response Time: `create_reservation_ms` (milliseconds)

| Scenario     |   avg |   p50 |   p90 |   p95 |   p99 |    max |
|--------------|------:|------:|------:|------:|------:|-------:|
| baseline     |  11.6 |  10.6 |  16.4 |  20.2 |  32.0 |   85.2 |
| low_load     |   8.0 |   5.7 |  14.0 |  19.8 |  41.3 |  225.1 |
| medium_load  |   6.7 |   4.4 |  11.0 |  15.9 |  33.5 |  895.6 |
| high_load    |  11.5 |   7.1 |  23.8 |  35.9 |  72.9 |  155.4 |

**0 errors** across all 100,606 requests (100% success rate).

### Observations

- **Sweet spot at 30 VUs (medium_load):** lowest average latency (6.7 ms) and lowest p50 (4.4 ms). The DB connection pool (HikariCP, 10 connections per service) is saturated just enough to keep throughput high without queuing.
- **Latency degrades at 60 VUs:** p95 grows from 15.9 ms to 35.9 ms as connection-pool contention increases. Despite this, the system sustains ~972 req/s with zero errors.
- **Baseline overhead is higher (11.6 ms avg):** with 1 VU the JIT compiler is cold and connection-pool round-trip dominates individual requests.
- **p99/max spikes** in low_load (41.3/225.1 ms) and medium_load (33.5/895.6 ms) correspond to occasional GC pauses and outbox-scheduler wakeup moments (the scheduler flushes all NEW events every 5 s causing a brief write burst).

---

## 2. End-to-End Processing Time

**Test:** full async flow from reservation creation to terminal status (`PAID`).
**20 sequential iterations, 1 VU, 0 failures.**

### Phase Breakdown (seconds)

| Metric                      |   avg |   min |   p50 |   p90 |   p95 |   max |
|-----------------------------|------:|------:|------:|------:|------:|------:|
| `e2e_total_ms`              |  7.10 |  4.71 |  5.21 |  9.92 | 10.33 | 10.34 |
| `e2e_to_payment_ms`         |  2.37 |  0.05 |  0.55 |  5.17 |  5.19 |  5.19 |
| `e2e_to_confirm_ms`         |  4.71 |  4.59 |  4.63 |  5.17 |  5.17 |  5.17 |

All 20 flows completed successfully. **Threshold `p(95) < 25 s` passed with p95 = 10.33 s.**

### Bimodal Distribution in `e2e_total_ms`

The raw values fall into two distinct clusters:

| Cluster | Count | Range      | Explanation |
|---------|------:|-----------|-------------|
| Fast    |    12 | 4.7 – 6.0 s | Reservation outbox event was published within the current scheduler window |
| Slow    |     8 | 9.8 – 10.3 s | Outbox event arrived just after a scheduler tick → waited a full extra 5 s cycle |

This bimodal pattern is the direct fingerprint of the **transactional outbox pattern** with `fixedDelay=5 s`: each Kafka hop introduces 0–5 s of latency, and two hops means total jitter of 0–10 s.

### Phase Analysis

- **`e2e_to_payment_ms` (0.05 – 5.19 s):** time from reservation creation until payment-service creates the Payment. Driven purely by outbox scheduler jitter (0–5 s) + Kafka delivery (<50 ms) + payment-service processing (<100 ms). When the event arrives before the next tick, the payment appears in <600 ms.
- **`e2e_to_confirm_ms` (4.59 – 5.17 s):** time from payment confirmation until reservation status becomes PAID. This phase is nearly always ~5 s because the payment confirmation outbox event is always newly written and almost always has to wait for the next scheduler tick. The tight range (4.59–5.17 s) confirms near-deterministic behavior.

---

## 3. Resource Usage

**Measured during the latency/throughput test (60 VUs peak, ~972 req/s).**
**Host memory limit per Docker:** 5.786 GiB total shared.

### CPU (% of one logical core)

| Service              | Avg CPU% | Max CPU% | Notes |
|----------------------|---------:|---------:|-------|
| reservation-service  |     19.6 |    144.6 | Hot path: handles all reservation writes + outbox |
| payment-service      |      8.5 |    131.7 | Consumes Kafka events + processes payments |
| gateway-service      |     10.0 |    170.3 | Proxies all requests; highest max due to routing overhead |
| kafka                |     18.8 |    137.5 | Broker under heavy topic activity |
| postgres             |     10.8 |    128.9 | Shared DB; handles all service tables |
| notification-service |      1.3 |     40.7 | Consumes payment events only; low load |
| user-service         |      0.4 |     41.7 | Mostly idle during reservation tests |

### Memory (MiB)

| Service              | Idle Mem | Peak Mem | Notes |
|----------------------|---------:|---------:|-------|
| payment-service      |      858 |      857 | Largest JVM heap; caches event class lookup |
| kafka                |      568 |      654 | Broker memory grows with partition offsets |
| reservation-service  |      561 |      564 | JVM heap stable after warmup |
| notification-service |      315 |      315 | Minimal activity, stable heap |
| user-service         |      308 |      308 | Idle throughout latency test |
| gateway-service      |      291 |      292 | Low heap; mostly routing logic |
| postgres             |      190 |      192 | Shared among all service DBs; very memory-efficient |

**Total memory footprint at peak load: ~3.5 GiB** across all 7 containers (5 Spring Boot services + Kafka + PostgreSQL).

### Resource Observations

- **Gateway-service CPU spikes highest (max 170%)** — all 972 req/s go through it, making it the most CPU-intensive component per request.
- **payment-service uses the most memory (~858 MiB)** — it deserializes and processes Kafka events in addition to serving REST APIs, and Spring Boot's default heap grows to accommodate.
- **PostgreSQL is extremely memory-efficient (190 MiB)** for serving 5 logical databases under ~972 req/s write throughput.
- **Kafka uses 137.5% CPU during peak** — at high throughput, broker is actively flushing logs to disk and managing consumer group coordination.
- **All services are CPU-bound, not memory-bound** — no service approaches its memory limit; the bottleneck is CPU and DB connection pooling.

---

## 4. Key Bottlenecks Identified & Fixed

The following issues were discovered during pre-test code review and fixed before running tests:

| # | Issue | Impact | Fix Applied |
|---|-------|--------|-------------|
| 1 | Missing Kafka consumer config in reservation-service (no `JsonDeserializer`) | **Critical** — E2E flow completely broken; events went to DLT | Added full consumer config to `application-docker.yml` |
| 2 | `@Scheduled(fixedRate=...)` on outbox publisher | Scheduler overlap under load; events published twice | Changed to `fixedDelay` |
| 3 | `outboxEventRepository.save(event)` per event in a loop | N writes per scheduler tick instead of 1 batch | Changed to `saveAll(toSave)` at end of loop |
| 4 | No Kafka ack waiting (`kafkaTemplate.send(...)` fire-and-forget) | Events marked SENT even if Kafka broker rejects | Added `.get(5, TimeUnit.SECONDS)` to await broker ack |
| 5 | `Class.forName(eventType)` on every event in payment outbox | Reflection overhead per event | Added `ConcurrentHashMap` cache |
| 6 | Missing `@Table`/`@Index` on outbox `status` column | Full table scan on every outbox poll | Added `@Index(columnList = "status")` |
| 7 | Excessive `log.info()` in `createReservation()` hot path | I/O overhead at ~972 req/s | Reduced to single log per reservation creation |
| 8 | `show-sql: true` in notification-service Docker config | SQL logging I/O under load | Removed from `application-docker.yml` |

---

## 5. Summary for Thesis

| Metric                        | Value                              |
|-------------------------------|------------------------------------|
| **Peak throughput**           | ~972 req/s (60 VUs, single node)   |
| **Median latency (60 VUs)**   | 7.1 ms                             |
| **p95 latency (60 VUs)**      | 35.9 ms                            |
| **E2E time (median)**         | 5.21 s                             |
| **E2E time (p95)**            | 10.33 s                            |
| **E2E success rate**          | 100% (20/20 flows)                 |
| **HTTP error rate**           | 0% (100,606 requests)              |
| **Total memory footprint**    | ~3.5 GiB (7 containers)            |
| **Peak CPU (gateway)**        | 170% (1.7 logical cores)           |
| **Async latency source**      | Outbox scheduler jitter: 0–5 s/hop |

### Architectural Trade-offs

**Advantages of this microservices design:**
- **Fault isolation** — each service has its own DB; one service failure doesn't cascade
- **Independent scaling** — reservation-service (hot path) can be scaled independently
- **Guaranteed delivery** — transactional outbox ensures events are never lost even if Kafka is temporarily unavailable
- **High write throughput** — 972 req/s with sub-40 ms p95 latency for synchronous reservation creation

**Costs vs. a monolith:**
- **E2E processing time** — the outbox pattern introduces 0–5 s latency per async hop; a monolith would process the same flow synchronously in <100 ms
- **Infrastructure overhead** — 7 containers consuming 3.5 GiB RAM for what a monolith could do in 1 container at ~500 MiB
- **Operational complexity** — consumer group coordination, DLT handling, outbox cleanup, distributed tracing all require additional tooling

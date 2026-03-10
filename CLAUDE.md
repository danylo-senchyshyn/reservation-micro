# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build all services (skip tests)
mvn clean install -DskipTests

# Run all tests
mvn test

# Run tests for a single service
mvn test -pl services/reservation-service

# Run a single test class
mvn test -pl services/reservation-service -Dtest=ReservationServiceTest

# Start all services via Docker Compose
docker-compose up --build -d

# Stop all services
docker-compose down
```

## Project Structure

Multi-module Maven project with 6 Spring Boot microservices under `services/`:

| Service | Port | Role |
|---|---|---|
| `gateway-service` | 8080 | API Gateway (Spring Cloud Gateway) — single entry point |
| `user-service` | 8081 | User management with PostgreSQL (`user_db`) |
| `reservation-service` | 8082 | Reservation lifecycle with PostgreSQL (`reservation_db`) |
| `payment-service` | 8083 | Payment processing with PostgreSQL (`payment_db`) |
| `notification-service` | 8084 | Event-driven notifications, no REST endpoints |
| `common` | — | Shared Kafka event POJOs (`ReservationCreatedEvent`, `PaymentConfirmedEvent`, `PaymentFailedEvent`) |

All service images are built from a single `Dockerfile` using multi-stage builds. Infrastructure (Kafka, Zookeeper, PostgreSQL) runs via `docker-compose.yml`. DB schemas are managed by Flyway; `db_init/init-db.sql` creates the 4 databases on first startup.

## Architecture: Event-Driven with Outbox Pattern

**Event flow:**
1. Client → Gateway → `reservation-service` creates reservation
2. `reservation-service` publishes `ReservationCreatedEvent` → `reservation-service.reservation-created`
3. `payment-service` consumes, processes payment, publishes `PaymentConfirmedEvent` or `PaymentFailedEvent`
4. `reservation-service` and `notification-service` consume payment events and update state / send notifications

**Outbox Pattern** (both `reservation-service` and `payment-service`):
- Events are written to the `outbox_event` table in the same DB transaction as the domain entity change.
- A `@Scheduled` task in `OutboxEventPublisher` polls every 5 seconds and publishes `NEW` events to Kafka, then marks them `SENT` or `FAILED`.
- This guarantees atomicity between the DB write and Kafka publish.

**Idempotent consumers**: All `*Listener` classes guard against duplicate Kafka messages using event IDs and state-based validation.

**Dead Letter Queue**: Failed messages go to `*.dlt` topics (e.g., `reservation.service.dlt`). Configured in each service's `KafkaConsumerConfig`.

## Key Patterns & Conventions

- **Spring profiles**: `default` for local dev, `docker` for containerized runs. Each service has `application.yml` and `application-docker.yml`.
- **Kafka consumer groups**: `payment-service-group`, `reservation-service-group`, `notification-service-group`.
- **Kafka topics** are defined in `application.yml` under `app.kafka.topics.*` and injected via `@Value`.
- **Java 17**, Spring Boot 3.4.0, Spring Cloud 2024.0.0.
- Lombok is used extensively — expect `@Data`, `@Builder`, `@RequiredArgsConstructor`.
- Database migrations live in `src/main/resources/db/migration/` per service (Flyway naming: `V{n}__{description}.sql`).

# Architecture Description

This project showcases a modern microservices architecture designed for a reservation system. It leverages an event-driven paradigm for loose coupling and high scalability, utilizing Apache Kafka as the central messaging backbone. Each service is independently deployable and scalable, communicating primarily through asynchronous events.

## Key Principles

*   **Service Autonomy:** Each microservice (`user-service`, `reservation-service`, `payment-service`, `notification-service`) owns its data and business logic, minimizing direct dependencies.
*   **Event-Driven Communication:** Services communicate by producing and consuming events via Kafka. This enables asynchronous processing, improved resilience, and easier integration.
*   **API Gateway:** A `gateway-service` provides a single, unified entry point for external clients, abstracting the underlying microservice topology.
*   **Outbox Pattern:** Employed for reliable event publishing, ensuring atomicity between local database transactions and Kafka message production.
*   **Idempotent Consumers:** All event consumers are designed to handle duplicate messages gracefully, preventing unintended side effects.
*   **Distributed Transactions:** Achieved through a saga-like orchestration via event correlation, rather than traditional two-phase commits, allowing for eventual consistency.

## Components

*   **Spring Boot Microservices:** Built with Java and Spring Boot, providing rapid development and a robust runtime environment.
*   **Apache Kafka:** A distributed streaming platform used for publishing and subscribing to events, enabling asynchronous, real-time data flows.
*   **PostgreSQL:** Each service utilizes its own dedicated PostgreSQL instance for data persistence, ensuring data isolation.
*   **Spring Cloud Gateway:** Provides routing, filtering, and load balancing capabilities for incoming API requests.
*   **Docker & Docker Compose:** Used for containerization and local orchestration of the entire microservices ecosystem.

## Data Flow (Reservation Example)

1.  **Client Request:** A client sends a `CreateReservation` request to the `gateway-service`.
2.  **Reservation Creation:** The `gateway-service` forwards the request to the `reservation-service`. The `reservation-service` processes the request, creates a reservation entry in its database, and records a `ReservationCreatedEvent` in its local Outbox table.
3.  **Event Publishing:** A component within the `reservation-service` (or an external process) reads the `ReservationCreatedEvent` from the Outbox table and publishes it to the `reservation-created-events` Kafka topic.
4.  **Payment Processing:** The `payment-service` consumes the `ReservationCreatedEvent` from Kafka. It processes the payment, updates its payment records, and either records a `PaymentConfirmedEvent` or `PaymentFailedEvent` in its local Outbox table.
5.  **Payment Event Publishing:** The `payment-service`'s Outbox mechanism publishes the payment outcome event to the respective Kafka topic (`payment-confirmed-events` or `payment-failed-events`).
6.  **Notification & Status Update:**
    *   The `notification-service` consumes the payment outcome event and sends a notification to the user.
    *   The `reservation-service` also consumes the payment outcome event and updates the status of the original reservation (e.g., from `PENDING` to `CONFIRMED` or `FAILED`).

This architecture promotes resilience, as services can operate independently even if others are temporarily unavailable (messages are queued in Kafka). It also simplifies scaling, as individual services can be scaled horizontally based on their specific load requirements.

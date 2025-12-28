# Reservation Microservices Project

This project implements a microservices-based reservation system using Spring Boot, Kafka, PostgreSQL, and Docker. It showcases an event-driven architecture, robust transaction management with the Outbox Pattern, and inter-service communication through asynchronous messaging.

## Project Overview

The Reservation Microservices project provides a foundational system for managing reservations, handling payments, and sending notifications. It addresses the challenges of distributed systems by employing key architectural patterns to ensure consistency, resilience, and scalability.

**Problem Solved:** Orchestrates the complex workflow of creating a reservation, processing its payment, and notifying users, all while maintaining data consistency across independent services in a distributed environment.

## Architecture Overview

The system is composed of several independent microservices, each responsible for a specific business domain. Communication between services primarily occurs asynchronously via Apache Kafka.

*   **`gateway-service`**: Acts as the entry point for all client requests, routing them to the appropriate backend service.
*   **`user-service`**: Manages user accounts and profiles.
*   **`reservation-service`**: Handles the creation, management, and status updates of reservations. It initiates the payment process upon a new reservation.
*   **`payment-service`**: Processes payments for reservations. It interacts with an (simulated) external payment gateway and publishes payment outcomes.
*   **`notification-service`**: Sends various notifications (e.g., payment confirmation, payment failure) to users based on events.

## Event-Driven Flow Explanation

The core business process is driven by asynchronous events flowing through Kafka:

1.  A user creates a reservation via the `gateway-service` which routes the request to the `reservation-service`.
2.  The `reservation-service` creates a new reservation and publishes a `ReservationCreatedEvent` to Kafka.
3.  The `payment-service` consumes the `ReservationCreatedEvent`. It then simulates a payment process (e.g., interacting with a payment provider).
4.  Based on the payment outcome, the `payment-service` publishes either a `PaymentConfirmedEvent` or a `PaymentFailedEvent` to Kafka.
5.  Both the `notification-service` and the `reservation-service` consume these payment events:
    *   The `notification-service` sends an appropriate notification to the user.
    *   The `reservation-service` updates the status of the reservation (e.g., to `CONFIRMED` or `FAILED`).

## Outbox Pattern Explanation

The Outbox Pattern is implemented in services that produce events (e.g., `reservation-service`, `payment-service`) to ensure atomicity between local database transactions and publishing events to Kafka.

*   **Why it exists:** In a distributed system, directly publishing an event to Kafka after committing a local database transaction (or vice-versa) can lead to inconsistencies if one operation succeeds and the other fails. The Outbox Pattern guarantees that either both operations effectively succeed or neither does.
*   **How it works:** Instead of directly publishing to Kafka, events are first saved into an `OutboxEvent` table within the same local database transaction as the business operation (e.g., creating a reservation or updating payment status). A separate process (e.g., a Kafka Connect connector or a scheduled task) then monitors this `OutboxEvent` table, reads the events, publishes them to Kafka, and marks them as processed. This ensures that the event is published *only if* the local transaction commits successfully.

## Idempotency Explanation

Idempotency is crucial for services consuming Kafka messages, particularly in scenarios where messages might be redelivered due to network issues, consumer restarts, or Kafka broker failures.

*   **How it works:** Consumers (`payment-service`, `notification-service`, `reservation-service`) are designed to process the same message multiple times without causing unintended side effects. This is typically achieved by:
    *   Using a unique identifier from the incoming event (e.g., `eventId` or `correlationId`) to check if the operation has already been performed.
    *   Storing a record of processed message IDs in the consumer's local database.
    *   Performing conditional updates or inserts based on the current state and the incoming event.

## Tech Stack

*   **Backend:** Spring Boot (Java)
*   **Messaging:** Apache Kafka
*   **Database:** PostgreSQL
*   **API Gateway:** Spring Cloud Gateway
*   **Containerization:** Docker, Docker Compose
*   **Build Tool:** Apache Maven
*   **Testing:** JUnit, Mockito, Spring Boot Test

## How to Run Locally with Docker Compose

This project can be easily run locally using Docker Compose, which orchestrates all required services including Kafka, Zookeeper, PostgreSQL, and the Spring Boot applications.

### Prerequisites

*   Docker Desktop (or Docker Engine and Docker Compose) installed.
*   Sufficient RAM allocated to Docker (at least 8GB recommended).

### Start/Stop Commands

1.  **Build the project:**
    ```bash
    mvn clean install -DskipTests
    ```
    This will build all microservice JARs.
2.  **Start all services:**
    Navigate to the project root directory and run:
    ```bash
    docker-compose up --build -d
    ```
    *   `--build`: Rebuilds service images (useful if you made code changes).
    *   `-d`: Runs services in detached mode (in the background).
3.  **Check service status:**
    ```bash
    docker-compose ps
    ```
4.  **Stop all services:**
    ```bash
    docker-compose down
    ```
    This will stop and remove containers, networks, and volumes defined in the `docker-compose.yml`.

### Ports Table

| Service               | Host Port | Container Port | Description                             |
| :-------------------- | :-------- | :------------- | :-------------------------------------- |
| `gateway-service`     | `8080`    | `8080`         | Main entry point for API requests       |
| `reservation-service` | `8081`    | `8081`         | Reservation management API              |
| `payment-service`     | `8082`    | `8082`         | Payment processing API                  |
| `user-service`        | `8083`    | `8083`         | User management API                     |
| `notification-service`| `8084`    | `8084`         | Internal service for sending notifications |
| `kafka`               | `9092`    | `9092`         | Kafka broker                            |
| `zookeeper`           | `2181`    | `2181`         | Zookeeper for Kafka                     |
| `postgres` (res)      | `5432`    | `5432`         | PostgreSQL for Reservation Service      |
| `postgres` (pay)      | `5433`    | `5432`         | PostgreSQL for Payment Service          |
| `postgres` (notif)    | `5434`    | `5432`         | PostgreSQL for Notification Service     |
| `postgres` (user)     | `5435`    | `5432`         | PostgreSQL for User Service             |

_Note: PostgreSQL port mappings are examples and might vary based on your `docker-compose.yml` configuration. Confirm actual ports in `docker-compose.yml`._

## Minimal API Usage Examples

All API requests should be routed through the `gateway-service` on port `8080`.

### Create Reservation

(Sends to Reservation Service via Gateway)

```bash
curl -X POST http://localhost:8080/reservations \
-H "Content-Type: application/json" \
-d 
'{'
  "userId": "some-user-id",
  "resourceId": "some-resource-id",
  "amount": 100.00
}'
```
Expected response: A reservation object with a `PENDING` status. This will trigger the `ReservationCreatedEvent`.

### Confirm Payment for a Reservation

(Sends to Payment Service via Gateway)

```bash
curl -X POST http://localhost:8080/payments/confirm \
-H "Content-Type: application/json" \
-d 
'{'
  "reservationId": "UUID_OF_RESERVATION",
  "paymentDetails": "..."
}'
```
Expected response: Payment confirmation, triggering `PaymentConfirmedEvent`. The reservation status will eventually update to `CONFIRMED`.

### Fail Payment for a Reservation

(Sends to Payment Service via Gateway)

```bash
curl -X POST "http://localhost:8080/payments/fail?reservationId=UUID_OF_RESERVATION&reason=INSUFFICIENT_FUNDS" \
-H "Content-Type: application/json"
```
Expected response: Payment failure, triggering `PaymentFailedEvent`. The reservation status will eventually update to `FAILED`.

### Check Reservation Status

(Sends to Reservation Service via Gateway)

```bash
curl http://localhost:8080/reservations/UUID_OF_RESERVATION
```
Expected response: Reservation details including its current `status` (PENDING, CONFIRMED, FAILED).

## Kafka Topics List

The following Kafka topics are used for inter-service communication:

| Topic Name                  | Producer Service    | Consumer Service(s)                     | Description                                            |
| :-------------------------- | :------------------ | :-------------------------------------- | :----------------------------------------------------- |
| `reservation-created-events`| `reservation-service`| `payment-service`                       | Notifies that a new reservation has been initiated.    |
| `payment-confirmed-events`  | `payment-service`   | `notification-service`, `reservation-service` | Notifies that a payment for a reservation was successful. |
| `payment-failed-events`     | `payment-service`   | `notification-service`, `reservation-service` | Notifies that a payment for a reservation has failed.    |

## Troubleshooting

### Topic Mismatch Symptoms

*   **Problem:** Services are not receiving expected events, or events are not being processed.
*   **Cause:** A producer service is sending to a topic name that a consumer service is not configured to listen to, or the topic simply doesn't exist on the Kafka broker.
*   **Resolution:**
    1.  Verify topic names in `application.yml` (or `application-docker.yml`) for both producer and consumer services.
    2.  Check Kafka logs (`docker logs <kafka-container-id>`) for topic creation issues.
    3.  Use `kafka-console-consumer` to inspect if messages are actually being published to the intended topic.

### Deserialization Errors (e.g., `Bad header byte: X`)

*   **Problem:** Consumer services fail to process messages, often with errors indicating problems with message format or headers. Common when using different `spring-kafka` versions or custom serializers.
*   **Cause:** Mismatched serialization/deserialization mechanisms between producer and consumer, or corrupted messages.
*   **Resolution:**
    1.  Ensure that both producer and consumer use compatible serializers (e.g., `StringSerializer`, `JsonSerializer`, `ByteArraySerializer`). Check `spring.kafka.producer.value-serializer` and `spring.kafka.consumer.value-deserializer` properties.
    2.  For `JsonSerializer`, ensure that the Java classes used for serialization/deserialization are identical across services (e.g., `com.bp.common.events.*`).
    3.  If using A/B testing with different versions of event schemas, ensure proper schema evolution handling (e.g., using Avro with Schema Registry, though not implemented in this project).

### How to Inspect Kafka Messages with `kafka-console-consumer`

You can use the Kafka console consumer client within the Kafka Docker container to check if messages are being produced correctly.

1.  **Find Kafka container ID/name:**
    ```bash
    docker ps | grep kafka
    ```
2.  **Access the Kafka container shell:**
    ```bash
    docker exec -it <kafka-container-id-or-name> bash
    ```
3.  **Consume messages from a topic:**
    ```bash
    kafka-console-consumer --bootstrap-server localhost:9092 --topic <topic-name> --from-beginning
    ```
    Replace `<topic-name>` with the actual topic you want to inspect (e.g., `reservation-created-events`). `--from-beginning` ensures you see all messages currently in the topic.

## Project Status / Demo Checklist

This section outlines key aspects to highlight during a demonstration or for review.

*   **Core Flow Demonstration:**
    *   Show creating a reservation via the Gateway.
    *   Observe the `reservation-service` processing.
    *   Observe the `payment-service` consuming the event and processing payment.
    *   Observe the `notification-service` receiving payment status.
    *   Show the `reservation-service` updating the final reservation status.
*   **Outbox Pattern in Action:**
    *   Briefly explain its purpose.
    *   (Optional, if feasible to show) Simulate a failure after database commit but before Kafka publish to show the event remaining in the Outbox table until successfully sent.
*   **Idempotency Handling:**
    *   Explain the concept.
    *   (Optional, if feasible to show) Manually send a duplicate Kafka message to a consumer and demonstrate it's processed only once or without adverse effects.
*   **Scalability & Resilience:**
    *   Mention how Docker Compose facilitates easy scaling (though not explicitly scaled in this setup).
    *   Discuss how Kafka provides resilience against service outages.
*   **Error Handling:**
    *   Demonstrate a payment failure scenario.
    *   Show how notifications are sent for failures, and reservation status is updated accordingly.
*   **Code Structure:**
    *   Highlight clear service boundaries.
    *   Point out the `common` module for shared event definitions.
    *   Mention consistent configuration and testing patterns across services.

# Payment Service

The Payment Service is dedicated to handling all payment-related operations for reservations. It integrates with an external payment system (simulated) and communicates payment outcomes to other services via Kafka.

## Purpose

*   Process payments for incoming reservation requests.
*   Simulate interaction with an external payment gateway.
*   Publish `PaymentConfirmedEvent` or `PaymentFailedEvent` based on the payment processing result.
*   Maintain a record of all payment transactions.

## Key Endpoints

| Method | Path        | Description                       |
| :----- | :---------- | :-------------------------------- |
| `POST` | `/payments/confirm` | Explicitly confirms a payment for a given reservation (for testing/demo purposes). |
| `POST` | `/payments/fail` | Explicitly fails a payment for a given reservation (for testing/demo purposes), optionally with a reason. |

## Consumed/Produced Topics

| Topic Name                  | Role      | Event Type          | Description                                    |
| :-------------------------- | :-------- | :------------------ | :--------------------------------------------- |
| `reservation-created-events`| Consumer  | `ReservationCreatedEvent` | Listens for new reservations to initiate payment processing. |
| `payment-confirmed-events`  | Producer  | `PaymentConfirmedEvent` | Publishes when a payment for a reservation is successfully completed. |
| `payment-failed-events`     | Producer  | `PaymentFailedEvent`    | Publishes when a payment for a reservation fails. |

## How to Debug

1.  **Check Service Logs:**
    ```bash
    docker logs <payment-service-container-id>
    ```
    Look for payment processing details, external API call errors, or Kafka publishing issues.
2.  **Inspect Kafka Topics:** Use `kafka-console-consumer` to verify `reservation-created-events` are being consumed and `payment-confirmed-events`/`payment-failed-events` are being produced.
    ```bash
    docker exec -it <kafka-container-id> bash
    kafka-console-consumer --bootstrap-server localhost:9092 --topic payment-confirmed-events --from-beginning
    ```
3.  **Database Inspection:** Connect to the `payment-service`'s PostgreSQL instance to check the `payment` and `outbox_event` tables.
    *   Verify payment records are created with correct statuses.
    *   Ensure `PaymentConfirmedEvent`s or `PaymentFailedEvent`s are recorded in the `outbox_event` table and marked as `PUBLISHED`.

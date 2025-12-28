# Reservation Service

The Reservation Service is responsible for managing the lifecycle of reservations within the system. It handles the creation of new reservations, updates their statuses based on payment outcomes, and provides an API for querying reservation details.

## Purpose

*   Create and store reservation requests.
*   Initiate the payment process by publishing `ReservationCreatedEvent`s.
*   Update reservation statuses (`PENDING`, `CONFIRMED`, `FAILED`, `CANCELLED`) based on events received from the Payment Service.
*   Provide an API for retrieving reservation information.

## Key Endpoints

| Method | Path        | Description                       |
| :----- | :---------- | :-------------------------------- |
| `POST` | `/reservations` | Creates a new reservation.        |
| `GET`  | `/reservations/{id}` | Retrieves details for a specific reservation. |

## Consumed/Produced Topics

| Topic Name                  | Role      | Event Type          | Description                                    |
| :-------------------------- | :-------- | :------------------ | :--------------------------------------------- |
| `reservation-created-events`| Producer  | `ReservationCreatedEvent` | Publishes when a new reservation is successfully created. |
| `payment-confirmed-events`  | Consumer  | `PaymentConfirmedEvent` | Listens for successful payment confirmations to update reservation status to `CONFIRMED`. |
| `payment-failed-events`     | Consumer  | `PaymentFailedEvent`    | Listens for payment failures to update reservation status to `FAILED`. |

## How to Debug

1.  **Check Service Logs:**
    ```bash
    docker logs <reservation-service-container-id>
    ```
    Look for exceptions, error messages, or logs indicating event publishing/consumption.
2.  **Inspect Kafka Topic:** Use `kafka-console-consumer` to verify `reservation-created-events` are being produced and `payment-confirmed-events`/`payment-failed-events` are present on the Kafka broker.
    ```bash
    docker exec -it <kafka-container-id> bash
    kafka-console-consumer --bootstrap-server localhost:9092 --topic reservation-created-events --from-beginning
    ```
3.  **Database Inspection:** Connect to the `reservation-service`'s PostgreSQL instance to check the `reservation` and `outbox_event` tables directly.
    *   Verify new reservations are created.
    *   Ensure `ReservationCreatedEvent`s are recorded in the `outbox_event` table and marked as `PUBLISHED`.
    *   Confirm reservation statuses are updated correctly after payment events.

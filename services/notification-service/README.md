# Notification Service

The Notification Service is a reactive component responsible for sending timely communications to users based on significant events occurring within the system, primarily payment outcomes.

## Purpose

*   Listen for payment-related events (`PaymentConfirmedEvent`, `PaymentFailedEvent`).
*   Generate and send appropriate notifications (e.g., email, SMS - simulated in this project) to users.
*   Log all outgoing notifications for auditing purposes.

## Key Endpoints

This service typically does not expose public REST endpoints as it primarily operates by consuming Kafka messages.

## Consumed/Produced Topics

| Topic Name                  | Role      | Event Type          | Description                                    |
| :-------------------------- | :-------- | :------------------ | :--------------------------------------------- |
| `payment-confirmed-events`  | Consumer  | `PaymentConfirmedEvent` | Listens for successful payment confirmations to send positive notifications. |
| `payment-failed-events`     | Consumer  | `PaymentFailedEvent`    | Listens for payment failures to send problem notifications. |

## How to Debug

1.  **Check Service Logs:**
    ```bash
    docker logs <notification-service-container-id>
    ```
    Look for messages indicating successful notification processing, any errors encountered during sending notifications, or issues with Kafka message consumption.
2.  **Inspect Kafka Topics:** Use `kafka-console-consumer` to verify that `payment-confirmed-events` and `payment-failed-events` are being produced by the Payment Service and are available for consumption.
    ```bash
    docker exec -it <kafka-container-id> bash
    kafka-console-consumer --bootstrap-server localhost:9092 --topic payment-confirmed-events --from-beginning
    ```
3.  **Database Inspection:** Connect to the `notification-service`'s PostgreSQL instance to check the `notification_log` table.
    *   Verify that entries are created for each notification attempt, indicating what was sent and to whom.

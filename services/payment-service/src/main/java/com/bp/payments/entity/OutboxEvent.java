package com.bp.payments.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The type Outbox event.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String aggregateType; // e.g., "Payment"

    @Column(nullable = false)
    private Long aggregateId; // e.g., payment.getId()

    @Column(nullable = false)
    private String eventType; // e.g., "PaymentConfirmedEvent", "PaymentFailedEvent"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON representation of the event

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status; // NEW, SENT, FAILED
}

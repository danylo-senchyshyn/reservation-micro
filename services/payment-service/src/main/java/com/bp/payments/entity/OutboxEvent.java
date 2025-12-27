package com.bp.payments.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

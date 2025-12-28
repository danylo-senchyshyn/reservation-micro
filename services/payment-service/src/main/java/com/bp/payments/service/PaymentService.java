package com.bp.payments.service;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.common.events.PaymentFailedEvent;
import com.bp.common.events.ReservationCreatedEvent;
import com.bp.payments.api.dto.CreatePaymentRequest;
import com.bp.payments.api.dto.PaymentResponse;
import com.bp.payments.entity.OutboxEvent;
import com.bp.payments.entity.OutboxEventStatus;
import com.bp.payments.entity.Payment;
import com.bp.payments.entity.PaymentStatus;
import com.bp.payments.exception.EntityNotFoundException;
import com.bp.payments.repository.OutboxEventRepository;
import com.bp.payments.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPayment(ReservationCreatedEvent event) {
        log.info(
                "Processing payment for reservationCreatedEvent: reservationId={}, userId={}, resourceId={}",
                event.reservationId(),
                event.userId(),
                event.resourceId()
        );

        if (paymentRepository.existsByReservationId(event.reservationId())) {
            log.warn(
                    "Payment already exists for reservationId={}, skipping",
                    event.reservationId()
            );
            return;
        }

        Payment payment = Payment.builder()
                .reservationId(event.reservationId())
                .amount(calculateAmount(event))
                .status(PaymentStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info(
                "Payment created for reservationId={}, paymentId={}",
                event.reservationId(),
                payment.getId()
        );
    }

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request) {
        log.info(
                "Creating payment for reservationId={}, amount={}",
                request.reservationId(),
                request.amount()
        );

        if (paymentRepository.existsByReservationId(request.reservationId())) {
            log.warn("Payment already exists for reservationId={}", request.reservationId());
            throw new IllegalStateException("Payment already exists for reservationId=" + request.reservationId());
        }

        Payment payment = Payment.builder()
                .reservationId(request.reservationId())
                .amount(request.amount())
                .status(PaymentStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);

        log.info(
                "Payment created successfully: paymentId={}, reservationId={}, status={}",
                payment.getId(),
                payment.getReservationId(),
                payment.getStatus()
        );

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse confirm(Long id) {
        Payment payment = getPayment(id);

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            log.warn("Confirm skipped: paymentId={} already CONFIRMED", id);
            return toResponse(payment);
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("Confirm rejected: paymentId={} already FAILED", id);
            return toResponse(payment);
        }

        log.info(
                "Confirming payment: paymentId={}, reservationId={}",
                payment.getId(),
                payment.getReservationId()
        );

        payment.setStatus(PaymentStatus.CONFIRMED);

        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                payment.getId(),
                payment.getReservationId(),
                toEventStatus(payment.getStatus())
        );

        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Payment")
                    .aggregateId(payment.getId())
                    .eventType(event.getClass().getName())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxEventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event saved for paymentId={} (Confirmed)", payment.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentConfirmedEvent for paymentId={}: {}", payment.getId(), e.getMessage());
            throw new RuntimeException("Failed to serialize event", e);
        }

        log.info(
                "Payment confirmed and outbox event added: paymentId={}, reservationId={}",
                payment.getId(),
                payment.getReservationId()
        );

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse fail(Long id, String reason) {
        Payment payment = getPayment(id);

        // 1️⃣ Идемпотентность
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("Fail skipped: paymentId={} already FAILED", id);
            return toResponse(payment);
        }

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            log.warn("Fail rejected: paymentId={} already CONFIRMED", id);
            return toResponse(payment);
        }

        // 2️⃣ Основная бизнес-логика
        log.info(
                "Failing payment: paymentId={}, reservationId={}, reason={}",
                payment.getId(),
                payment.getReservationId(),
                reason
        );

        payment.setStatus(PaymentStatus.FAILED);
        // ❗ сохранять явно не нужно — JPA flush внутри @Transactional

        // 3️⃣ Формирование доменного события
        PaymentFailedEvent event = new PaymentFailedEvent(
                payment.getId(),
                payment.getReservationId(),
                reason
        );

        // 4️⃣ Outbox (в одной транзакции с Payment)
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Payment")
                    .aggregateId(payment.getId())
                    .eventType(event.getClass().getName()) // FQN — правильно
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxEventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);

            log.info(
                    "Outbox event saved: type=PaymentFailedEvent, paymentId={}",
                    payment.getId()
            );
        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to serialize PaymentFailedEvent: paymentId={}, error={}",
                    payment.getId(),
                    e.getMessage(),
                    e
            );
            throw new IllegalStateException("Failed to serialize PaymentFailedEvent", e);
        }

        log.info(
                "Payment marked as FAILED and outbox event created: paymentId={}, reservationId={}",
                payment.getId(),
                payment.getReservationId()
        );

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        log.debug("Fetching payment by id={}", id);
        return toResponse(getPayment(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByReservationId(Long reservationId) {
        log.debug("Fetching payments by reservationId={}", reservationId);

        return paymentRepository.findByReservationId(reservationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteAll() {
        log.warn("Deleting all payments");
        paymentRepository.deleteAll();
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Payment not found: paymentId={}", id);
                    return new EntityNotFoundException(
                            "Payment with id " + id + " not found"
                    );
                });
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservationId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }

    private com.bp.common.events.PaymentStatus toEventStatus(PaymentStatus status) {
        return com.bp.common.events.PaymentStatus.valueOf(status.name());
    }

    private BigDecimal calculateAmount(ReservationCreatedEvent event) {
        return BigDecimal.valueOf(100);
    }
}
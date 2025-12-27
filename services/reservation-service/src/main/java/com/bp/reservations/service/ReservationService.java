package com.bp.reservations.service;

import com.bp.common.events.ReservationCreatedEvent;
import com.bp.reservations.api.dto.CreateReservationRequest;
import com.bp.reservations.api.dto.ReservationResponse;
import com.bp.reservations.entity.OutboxEvent;
import com.bp.reservations.entity.OutboxEventStatus;
import com.bp.reservations.entity.Reservation;
import com.bp.reservations.entity.ReservationStatus;
import com.bp.reservations.repository.OutboxEventRepository;
import com.bp.reservations.repository.ReservationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        log.info(
                "Creating reservation: userId={}, resourceId={}, from={}, to={}",
                request.userId(),
                request.resourceId(),
                request.from(),
                request.to()
        );

        if (!request.from().isBefore(request.to())) {
            throw new IllegalArgumentException("from must be before to");
        }

        Reservation reservation = Reservation.builder()
                .userId(request.userId())
                .resourceId(request.resourceId())
                .startTime(request.from())
                .endTime(request.to())
                .status(ReservationStatus.CREATED)
                .build();

        reservation = reservationRepository.save(reservation);

        log.info(
                "Reservation persisted: id={}, status={}",
                reservation.getId(),
                reservation.getStatus()
        );

        ReservationCreatedEvent event = new ReservationCreatedEvent(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getResourceId(),
                reservation.getStartTime(),
                reservation.getEndTime()
        );

        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Reservation")
                    .aggregateId(reservation.getId())
                    .eventType(event.getClass().getSimpleName())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxEventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event saved for reservationId={}", reservation.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ReservationCreatedEvent for reservationId={}: {}", reservation.getId(), e.getMessage());
            throw new RuntimeException("Failed to serialize event", e);
        }

        log.info(
                "Reservation created and outbox event added: reservationId={}",
                reservation.getId()
        );

        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() ->
                        new com.bp.reservations.exception.EntityNotFoundException(
                                "Reservation with id " + id + " not found"
                        )
                );

        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAll() {
        return reservationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void updateReservationStatus(Long reservationId, com.bp.common.events.PaymentStatus paymentStatus) {
        log.info(
                "Attempting to update reservation status for reservationId: {} with payment status: {}",
                reservationId,
                paymentStatus
        );

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    log.warn("Reservation not found for reservationId={}", reservationId);
                    return new com.bp.reservations.exception.EntityNotFoundException(
                            "Reservation with id " + reservationId + " not found"
                    );
                });

        ReservationStatus newStatus;
        switch (paymentStatus) {
            case CONFIRMED:
                newStatus = ReservationStatus.PAID;
                break;
            case FAILED:
                newStatus = ReservationStatus.PAYMENT_FAILED;
                break;
            default:
                log.warn("Unknown payment status received: {}. Skipping reservation status update.", paymentStatus);
                return;
        }

        if (reservation.getStatus() == newStatus) {
            log.warn("Reservation {} already has status {}. Skipping update.", reservationId, newStatus);
            return;
        }

        log.info(
                "Updating reservation status: id={}, {} -> {}",
                reservationId,
                reservation.getStatus(),
                newStatus
        );
        reservation.setStatus(newStatus);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getByUserId(Long userId) {
        return reservationRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatus newStatus) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() ->
                        new com.bp.reservations.exception.EntityNotFoundException(
                                "Reservation with id " + id + " not found"
                        )
                );

        if (reservation.getStatus() == newStatus) {
            log.info(
                    "Reservation {} already has status {}",
                    id,
                    newStatus
            );
            return toResponse(reservation);
        }

        log.info(
                "Updating reservation status: id={}, {} -> {}",
                id,
                reservation.getStatus(),
                newStatus
        );

        reservation.setStatus(newStatus);
        return toResponse(reservation);
    }

    @Transactional
    public void cancel(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() ->
                        new com.bp.reservations.exception.EntityNotFoundException(
                                "Reservation with id " + id + " not found"
                        )
                );

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            log.info("Reservation {} already cancelled", id);
            return;
        }

        log.info("Cancelling reservation {}", id);
        reservation.setStatus(ReservationStatus.CANCELLED);
    }

    @Transactional
    public void cancelAll() {
        log.warn("Cancelling ALL reservations");

        reservationRepository.cancelAllActive();
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getResourceId(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus()
        );
    }
}
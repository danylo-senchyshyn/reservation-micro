package com.bp.reservations.service;

import com.bp.common.events.ReservationCreatedEvent;
import com.bp.reservations.api.dto.CreateReservationRequest;
import com.bp.reservations.api.dto.ReservationResponse;
import com.bp.reservations.entity.Reservation;
import com.bp.reservations.entity.ReservationStatus;
import com.bp.reservations.kafka.ReservationProducer;
import com.bp.reservations.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationProducer reservationProducer;

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

        reservationProducer.sendReservationCreatedEvent(event);

        log.info(
                "ReservationCreatedEvent sent: reservationId={}",
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
        return reservationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
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
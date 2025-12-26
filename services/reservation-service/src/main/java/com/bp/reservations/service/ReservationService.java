package com.bp.reservations.service;

import com.bp.common.events.ReservationCreatedEvent;
import com.bp.reservations.api.dto.CreateReservationRequest;
import com.bp.reservations.api.dto.ReservationResponse;
import com.bp.reservations.entity.Reservation;
import com.bp.reservations.entity.ReservationStatus;
import com.bp.reservations.kafka.ReservationProducer;
import com.bp.reservations.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationProducer reservationProducer;

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        Reservation reservation = Reservation.builder()
                .userId(Long.parseLong(request.userId()))
                .resourceId(request.resourceId())
                .startTime(Instant.parse(request.fromIso()))
                .endTime(Instant.parse(request.toIso()))
                .status(ReservationStatus.CREATED)
                .build();

        reservation = reservationRepository.save(reservation);

        ReservationCreatedEvent event = new ReservationCreatedEvent(
                reservation.getId().toString(),
                reservation.getUserId().toString(),
                reservation.getResourceId(),
                reservation.getStartTime().toString(),
                reservation.getEndTime().toString()
        );
        reservationProducer.sendReservationCreatedEvent(event);

        return toResponse(reservation);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId().toString(),
                reservation.getUserId().toString(),
                reservation.getResourceId(),
                reservation.getStartTime().toString(),
                reservation.getEndTime().toString(),
                reservation.getStatus().toString()
        );
    }
}

package com.bp.reservations.api;

import com.bp.reservations.api.dto.CreateReservationRequest;
import com.bp.reservations.api.dto.ReservationResponse;
import com.bp.reservations.entity.Reservation;
import com.bp.reservations.entity.ReservationStatus;
import com.bp.reservations.kafka.ReservationProducer;
import com.bp.reservations.repository.ReservationRepository;
import com.bp.reservations.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationProducer reservationProducer;

    @InjectMocks
    private ReservationService reservationService;

    private Reservation reservation;

    private static final LocalDateTime FROM =
            LocalDateTime.of(2026, 1, 10, 10, 0);

    private static final LocalDateTime TO =
            LocalDateTime.of(2026, 1, 10, 12, 0);

    @BeforeEach
    void setUp() {
        reservation = Reservation.builder()
                .id(1L)
                .userId(1L)
                .resourceId(1L)
                .startTime(FROM)
                .endTime(TO)
                .status(ReservationStatus.CREATED)
                .build();
    }

    // ---------- CREATE ----------

    @Test
    void shouldCreateReservation() {
        CreateReservationRequest request =
                new CreateReservationRequest(1L, 1L, FROM, TO);

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        ReservationResponse response =
                reservationService.createReservation(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ReservationStatus.CREATED);

        verify(reservationRepository).save(any(Reservation.class));
        verify(reservationProducer).sendReservationCreatedEvent(any());
    }

    // ---------- UPDATE STATUS ----------

    @Test
    void shouldUpdateReservationStatus() {
        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        ReservationResponse response =
                reservationService.updateStatus(1L, ReservationStatus.PAID);

        assertThat(response.status()).isEqualTo(ReservationStatus.PAID);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PAID);

        verify(reservationRepository).findById(1L);
    }

    // ---------- GET BY ID ----------

    @Test
    void shouldGetReservationById() {
        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        ReservationResponse response =
                reservationService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ReservationStatus.CREATED);

        verify(reservationRepository).findById(1L);
    }

    // ---------- GET BY USER ----------

    @Test
    void shouldGetReservationsByUser() {
        when(reservationRepository.findByUserId(1L))
                .thenReturn(List.of(reservation));

        List<ReservationResponse> result =
                reservationService.getByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);

        verify(reservationRepository).findByUserId(1L);
    }

    // ---------- GET ALL ----------

    @Test
    void shouldGetAllReservations() {
        when(reservationRepository.findAll())
                .thenReturn(List.of(reservation));

        List<ReservationResponse> result =
                reservationService.getAll();

        assertThat(result).hasSize(1);

        verify(reservationRepository).findAll();
    }

    // ---------- CANCEL ----------

    @Test
    void shouldCancelReservation() {
        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        reservationService.cancel(1L);

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);

        verify(reservationRepository).findById(1L);
        verifyNoMoreInteractions(reservationRepository);
    }

    @Test
    void shouldThrowIfFromIsAfterTo() {
        CreateReservationRequest request =
                new CreateReservationRequest(1L, 1L, TO, FROM);

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCancelAllReservations() {
        reservationService.cancelAll();
        verify(reservationRepository).cancelAllActive();
    }
}
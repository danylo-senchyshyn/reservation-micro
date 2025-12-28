package com.bp.reservations.api;

import com.bp.common.events.ReservationCreatedEvent;
import com.bp.reservations.entity.OutboxEvent;
import com.bp.reservations.entity.OutboxEventStatus;
import com.bp.reservations.kafka.OutboxEventPublisher;
import com.bp.reservations.kafka.ReservationProducer;
import com.bp.reservations.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReservationOutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ReservationProducer reservationProducer;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxEventPublisher publisher;

    @Test
    void shouldPublishOutboxEventAndMarkAsSent() throws Exception {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .aggregateType("Reservation")
                .aggregateId(10L)
                .eventType(ReservationCreatedEvent.class.getName())
                .payload("{json}")
                .status(OutboxEventStatus.NEW)
                .build();

        when(outboxEventRepository.findByStatus(OutboxEventStatus.NEW))
                .thenReturn(List.of(event));

        ReservationCreatedEvent domainEvent =
                new ReservationCreatedEvent(
                        10L,
                        1L,
                        5L,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusHours(2)
                );

        when(objectMapper.readValue(eq("{json}"), eq(ReservationCreatedEvent.class)))
                .thenReturn(domainEvent);

        publisher.publishOutboxEvents();

        verify(reservationProducer)
                .sendReservationCreatedEvent(domainEvent);

        verify(outboxEventRepository).save(argThat(saved ->
                saved.getId().equals(1L)
                        && saved.getStatus() == OutboxEventStatus.SENT
        ));

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.SENT);
    }

    @Test
    void shouldMarkEventAsFailedWhenJsonCannotBeDeserialized() throws Exception {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .aggregateType("Reservation")
                .aggregateId(10L)
                .eventType(ReservationCreatedEvent.class.getName())
                .payload("{broken-json}")
                .status(OutboxEventStatus.NEW)
                .build();

        when(outboxEventRepository.findByStatus(OutboxEventStatus.NEW))
                .thenReturn(List.of(event));

        when(objectMapper.readValue(anyString(), eq(ReservationCreatedEvent.class)))
                .thenThrow(new RuntimeException("boom"));

        publisher.publishOutboxEvents();

        verifyNoInteractions(reservationProducer);

        verify(outboxEventRepository).save(argThat(saved ->
                saved.getId().equals(1L)
                        && saved.getStatus() == OutboxEventStatus.FAILED
        ));

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
    }

    @Test
    void shouldDoNothingWhenNoNewEvents() {
        when(outboxEventRepository.findByStatus(OutboxEventStatus.NEW))
                .thenReturn(List.of());

        publisher.publishOutboxEvents();

        verify(outboxEventRepository).findByStatus(OutboxEventStatus.NEW);
        verifyNoInteractions(reservationProducer);
        verify(outboxEventRepository, never()).save(any());
    }
}
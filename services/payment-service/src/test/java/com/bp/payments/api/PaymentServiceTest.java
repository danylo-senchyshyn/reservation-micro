package com.bp.payments.api;

import com.bp.payments.api.dto.CreatePaymentRequest;
import com.bp.payments.entity.OutboxEvent;
import com.bp.payments.entity.Payment;
import com.bp.payments.entity.PaymentStatus;
import com.bp.payments.exception.EntityNotFoundException;
import com.bp.payments.repository.OutboxEventRepository;
import com.bp.payments.repository.PaymentRepository;
import com.bp.payments.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id(1L)
                .reservationId(10L)
                .amount(BigDecimal.valueOf(100))
                .status(PaymentStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ---------- CREATE ----------

    @Test
    void shouldCreatePayment() {
        CreatePaymentRequest request =
                new CreatePaymentRequest(10L, BigDecimal.valueOf(100));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        var response = paymentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.reservationId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(PaymentStatus.CREATED);
        assertThat(response.amount()).isEqualTo(BigDecimal.valueOf(100));

        verify(paymentRepository).save(any(Payment.class));
        verifyNoInteractions(outboxEventRepository);
    }

    // ---------- CONFIRM ----------

    @Test
    void shouldConfirmPayment() throws Exception {
        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"payload\":\"json\"}");

        var response = paymentService.confirm(1L);

        assertThat(response.status()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);

        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldBeIdempotentOnConfirm() {
        payment.setStatus(PaymentStatus.CONFIRMED);

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        var response = paymentService.confirm(1L);

        assertThat(response.status()).isEqualTo(PaymentStatus.CONFIRMED);

        verifyNoInteractions(outboxEventRepository);
    }

    // ---------- FAIL ----------

    @Test
    void shouldFailPayment() throws Exception {
        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"payload\":\"json\"}");

        var response = paymentService.fail(1L, "Not enough funds");

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);

        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldBeIdempotentOnFail() {
        payment.setStatus(PaymentStatus.FAILED);

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        var response = paymentService.fail(1L, "any");

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);

        verifyNoInteractions(outboxEventRepository);
    }

    // ---------- NOT FOUND ----------

    @Test
    void shouldThrowIfPaymentNotFound() {
        when(paymentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirm(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(outboxEventRepository);
    }
}
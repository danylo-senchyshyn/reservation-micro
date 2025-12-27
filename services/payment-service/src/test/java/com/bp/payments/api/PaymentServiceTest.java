package com.bp.payments.api;

import com.bp.payments.api.dto.CreatePaymentRequest;
import com.bp.payments.entity.Payment;
import com.bp.payments.entity.PaymentStatus;
import com.bp.payments.exception.EntityNotFoundException;
import com.bp.payments.kafka.PaymentProducer;
import com.bp.payments.repository.PaymentRepository;
import com.bp.payments.service.PaymentService;
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
    private PaymentProducer paymentProducer;

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
        verifyNoInteractions(paymentProducer);
    }

    // ---------- CONFIRM ----------

    @Test
    void shouldConfirmPayment() {
        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        var response = paymentService.confirm(1L);

        assertThat(response.status()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);

        verify(paymentProducer).sendPaymentConfirmedEvent(
                argThat(event ->
                        event.paymentId().equals(1L)
                                && event.reservationId().equals(10L)
                                && event.status().name().equals("CONFIRMED")
                )
        );
    }

    @Test
    void shouldBeIdempotentOnConfirm() {
        payment.setStatus(PaymentStatus.CONFIRMED);

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        var response = paymentService.confirm(1L);

        assertThat(response.status()).isEqualTo(PaymentStatus.CONFIRMED);

        verifyNoInteractions(paymentProducer);
    }

    // ---------- FAIL ----------

    @Test
    void shouldFailPayment() {
        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        var response = paymentService.fail(1L, "Not enough funds");

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);

        verify(paymentProducer).sendPaymentFailedEvent(
                argThat(event ->
                        event.paymentId().equals(1L)
                                && event.reservationId().equals(10L)
                                && event.reason().equals("Not enough funds")
                )
        );
    }

    @Test
    void shouldBeIdempotentOnFail() {
        payment.setStatus(PaymentStatus.FAILED);

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        var response = paymentService.fail(1L, "any");

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);

        verifyNoInteractions(paymentProducer);
    }

    // ---------- NOT FOUND ----------

    @Test
    void shouldThrowIfPaymentNotFound() {
        when(paymentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirm(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(paymentProducer);
    }
}

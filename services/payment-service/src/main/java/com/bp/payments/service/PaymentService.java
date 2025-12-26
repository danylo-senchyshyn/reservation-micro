package com.bp.payments.service;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.common.events.ReservationCreatedEvent;
import com.bp.payments.entity.Payment;
import com.bp.payments.kafka.PaymentProducer;
import com.bp.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;

    @Transactional
    public void processPayment(ReservationCreatedEvent event) {
        log.info("Processing payment for reservation: {}", event.reservationId());

        // In a real system, you would calculate the amount
        BigDecimal amount = new BigDecimal("100.00");

        Payment payment = Payment.builder()
                .reservationId(Long.parseLong(event.reservationId()))
                .amount(amount)
                .status("CONFIRMED")
                .build();

        payment = paymentRepository.save(payment);

        PaymentConfirmedEvent paymentConfirmedEvent = new PaymentConfirmedEvent(
                payment.getId().toString(),
                payment.getReservationId().toString(),
                payment.getStatus()
        );

        paymentProducer.sendPaymentConfirmedEvent(paymentConfirmedEvent);
    }
}

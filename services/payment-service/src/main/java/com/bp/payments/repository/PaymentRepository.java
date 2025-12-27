package com.bp.payments.repository;

import com.bp.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReservationId(Long reservationId);
    boolean existsByReservationId(Long reservationId);
}

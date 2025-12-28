package com.bp.payments.repository;

import com.bp.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * The interface Payment repository.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    /**
     * Find by reservation id optional.
     *
     * @param reservationId the reservation id
     * @return the optional
     */
    Optional<Payment> findByReservationId(Long reservationId);

    /**
     * Exists by reservation id boolean.
     *
     * @param reservationId the reservation id
     * @return the boolean
     */
    boolean existsByReservationId(Long reservationId);
}

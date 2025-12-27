package com.bp.reservations.repository;

import com.bp.reservations.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Modifying
    @Query("""
                update Reservation r
                set r.status = com.bp.reservations.entity.ReservationStatus.CANCELLED
                where r.status <> com.bp.reservations.entity.ReservationStatus.CANCELLED
            """)
    void cancelAllActive();

    List<Reservation> findByUserId(Long userId);
}

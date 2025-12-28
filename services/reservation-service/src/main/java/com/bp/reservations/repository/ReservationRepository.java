package com.bp.reservations.repository;

import com.bp.reservations.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * The interface Reservation repository.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    /**
     * Cancel all active.
     */
    @Modifying
    @Query("""
                update Reservation r
                set r.status = com.bp.reservations.entity.ReservationStatus.CANCELLED
                where r.status <> com.bp.reservations.entity.ReservationStatus.CANCELLED
            """)
    void cancelAllActive();

    /**
     * Find by user id list.
     *
     * @param userId the user id
     * @return the list
     */
    List<Reservation> findByUserId(Long userId);
}

package com.bp.payments.repository;

import com.bp.payments.entity.OutboxEvent;
import com.bp.payments.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * The interface Outbox event repository.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    /**
     * Find by status list.
     *
     * @param status the status
     * @return the list
     */
    List<OutboxEvent> findByStatus(OutboxEventStatus status);
}

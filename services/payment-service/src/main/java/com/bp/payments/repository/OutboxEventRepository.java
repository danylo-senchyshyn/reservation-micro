package com.bp.payments.repository;

import com.bp.payments.entity.OutboxEvent;
import com.bp.payments.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatus(OutboxEventStatus status);
}

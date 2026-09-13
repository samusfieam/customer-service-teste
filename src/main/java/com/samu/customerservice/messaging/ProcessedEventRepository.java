package com.samu.customerservice.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    @Modifying
    @Query(value = """
            insert into processed_events (event_id, event_type, processed_at)
            values (:eventId, :eventType, current_timestamp)
            on conflict (event_id) do nothing
            """, nativeQuery = true)
    int reserveEventIfAbsent(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType);
}

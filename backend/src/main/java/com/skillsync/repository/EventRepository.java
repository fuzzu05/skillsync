package com.skillsync.repository;

import com.skillsync.entity.Event;
import com.skillsync.enums.EventType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithPessimisticLock(@Param("id") Long id);

    @Query("SELECT e FROM Event e WHERE " +
           "(:eventType IS NULL OR e.eventType = :eventType) AND " +
           "e.endTime >= :now AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(e.location) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Event> searchUpcomingEvents(@Param("query") String query,
                                     @Param("eventType") EventType eventType,
                                     @Param("now") Instant now,
                                     Pageable pageable);
}

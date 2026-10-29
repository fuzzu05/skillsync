package com.skillsync.repository;

import com.skillsync.entity.EventRegistration;
import com.skillsync.entity.EventRegistrationId;
import com.skillsync.enums.EventRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, EventRegistrationId> {

    @Query("SELECT er FROM EventRegistration er JOIN FETCH er.user WHERE er.id.eventId = :eventId")
    List<EventRegistration> findByEventIdWithUser(@Param("eventId") Long eventId);

    @Query("SELECT er FROM EventRegistration er JOIN FETCH er.event WHERE er.id.userId = :userId ORDER BY er.registeredAt DESC")
    List<EventRegistration> findByUserIdWithEvent(@Param("userId") Long userId);

    long countByIdEventIdAndStatus(Long eventId, EventRegistrationStatus status);

    boolean existsByIdEventIdAndIdUserId(Long eventId, Long userId);

    Optional<EventRegistration> findByIdEventIdAndIdUserId(Long eventId, Long userId);
}
